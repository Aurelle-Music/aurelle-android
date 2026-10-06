@file:OptIn(UnstableApi::class)

package com.aurelle.music.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource.HttpDataSourceException
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/** Resposta HTTP de erro ao abrir o stream (ex.: 403 = URL expirada). */
class StreamHttpException(val code: Int) : IOException("aurelle:http_$code")

/**
 * Fonte de dados do ExoPlayer para o áudio do YouTube.
 *
 * Recebe como `uri` a URL da **página** do vídeo (id da música), resolve a URL real do stream
 * ([StreamResolver]) e a abre do jeito que o YouTube espera (mesmo comportamento do NewPipe):
 * - requisição `POST` com corpo `[0x78, 0x00]`;
 * - parâmetro `&rn=<n>` em cada requisição;
 * - posição/tamanho pedidos pelo parâmetro `&range=<ini>-<fim>` (e não pelo cabeçalho Range).
 * Se o servidor responder 403/404/410 (URL expirada), resolve de novo UMA vez e tenta outra vez.
 */
class AurelleDataSource(private val client: OkHttpClient) : BaseDataSource(/* isNetwork = */ true) {

    private var dataSpec: DataSpec? = null
    private var response: Response? = null
    private var input: InputStream? = null
    private var openedUri: Uri? = null
    private var bytesRemaining = C.LENGTH_UNSET.toLong()
    private var transferStarted = false
    private var requestNumber = 0

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        transferInitializing(dataSpec)

        val videoUrl = dataSpec.uri.toString()
        var streamUrl = StreamResolver.resolve(videoUrl)
        val resp = try {
            execute(dataSpec, streamUrl)
        } catch (e: StreamHttpException) {
            if (e.code !in REFRESH_CODES) throw e
            streamUrl = StreamResolver.resolve(videoUrl, forceRefresh = true)
            execute(dataSpec, streamUrl)
        }

        response = resp
        openedUri = Uri.parse(resp.request.url.toString())
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            resp.body?.contentLength()?.takeIf { it >= 0 } ?: C.LENGTH_UNSET.toLong()
        }
        input = resp.body?.byteStream()

        transferStarted = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val spec = dataSpec!!
        val toRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            minOf(bytesRemaining, length.toLong()).toInt()
        }
        val read = try {
            input!!.read(buffer, offset, toRead)
        } catch (e: IOException) {
            throw HttpDataSourceException.createForIOException(e, spec, HttpDataSourceException.TYPE_READ)
        }
        if (read == -1) {
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                // Veio menos do que o prometido.
                throw HttpDataSourceException.createForIOException(
                    EOFException(), spec, HttpDataSourceException.TYPE_READ,
                )
            }
            return C.RESULT_END_OF_INPUT
        }
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = openedUri

    override fun close() {
        try {
            input?.close()
            response?.close()
        } finally {
            input = null
            response = null
            openedUri = null
            if (transferStarted) {
                transferStarted = false
                transferEnded()
            }
        }
    }

    // --- Requisição ----------------------------------------------------------------------------

    private fun execute(spec: DataSpec, streamUrl: String): Response {
        val request = buildRequest(spec, streamUrl)
        val resp = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw HttpDataSourceException.createForIOException(e, spec, HttpDataSourceException.TYPE_OPEN)
        }
        if (resp.isSuccessful) return resp

        val code = resp.code
        resp.close()
        if (code == 416) {
            throw DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
        }
        throw StreamHttpException(code)
    }

    private fun buildRequest(spec: DataSpec, streamUrl: String): Request =
        YoutubeStreamRequest.build(streamUrl, spec.position, spec.length, requestNumber++)

    class Factory(private val client: OkHttpClient) : DataSource.Factory {
        override fun createDataSource(): DataSource = AurelleDataSource(client)
    }

    private companion object {
        val REFRESH_CODES = setOf(403, 404, 410)
    }
}

/**
 * Política de erro de carregamento: erros definitivos (música indisponível, 403/404/410 que já foram
 * tentados de novo) falham na hora, sem as 3 tentativas com espera do padrão do ExoPlayer.
 */
class AurelleLoadErrorPolicy : DefaultLoadErrorHandlingPolicy() {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val error = loadErrorInfo.exception
        if (error is StreamUnavailableException) return C.TIME_UNSET
        if (error is StreamHttpException && error.code in setOf(403, 404, 410)) return C.TIME_UNSET
        return super.getRetryDelayMsFor(loadErrorInfo)
    }
}
