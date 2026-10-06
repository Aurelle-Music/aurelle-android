package com.aurelle.music.download

import com.aurelle.music.player.StreamHttpException
import com.aurelle.music.player.StreamResolver.AudioChoice
import com.aurelle.music.player.YoutubeStreamRequest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.CoroutineContext

/** Erro com motivo já conhecido (a UI traduz o [error] para uma mensagem). */
class DownloadException(val error: DownloadError) : IOException("aurelle:download_${error.name}")

/**
 * Baixa o stream de áudio em BLOCOS (`&range=ini-fim`, como o player faz): o YouTube limita a velocidade
 * quando o arquivo é pedido inteiro de uma vez. Retoma de onde o arquivo parcial parou.
 */
class AudioDownloader(private val client: OkHttpClient) {

    private sealed interface Chunk {
        data class Read(val bytes: Long) : Chunk
        data object Expired : Chunk
        data object EndOfFile : Chunk
    }

    /**
     * @param refresh pede um stream novo (URL expirada). Se vier outro formato/bitrate, o parcial é descartado.
     * @param onProgress chamado a cada leitura com (bytes baixados, total ou null).
     */
    suspend fun download(
        initial: AudioChoice,
        partial: File,
        refresh: () -> AudioChoice,
        onProgress: (done: Long, total: Long?) -> Unit,
    ) {
        val ctx = currentCoroutineContext()
        var choice = initial
        var total: Long? = choice.contentLength
        var offset = partial.length()
        var requestNumber = 0
        var refreshedInARow = false

        val startTotal = total
        if (startTotal != null && offset > startTotal) { // parcial inválido
            partial.delete()
            offset = 0
        }

        FileOutputStream(partial, true).use { out ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val knownTotal = total
                if (knownTotal != null && offset >= knownTotal) break
                ctx.ensureActive()

                val want = if (knownTotal != null) minOf(CHUNK_SIZE, knownTotal - offset) else CHUNK_SIZE
                val result = readChunk(ctx, choice.url, offset, want, requestNumber++, out, buffer) { n ->
                    offset += n
                    onProgress(offset, total)
                }

                when (result) {
                    is Chunk.Read -> {
                        refreshedInARow = false
                        if (result.bytes == 0L) throw IOException("aurelle:empty_chunk")
                        if (knownTotal == null && result.bytes < want) break // sem total: bloco curto = fim
                    }
                    Chunk.Expired -> {
                        if (refreshedInARow) throw StreamHttpException(403)
                        refreshedInARow = true
                        val fresh = refresh()
                        if (fresh.suffix != choice.suffix || fresh.bitrateKbps != choice.bitrateKbps) {
                            out.channel.truncate(0) // outro stream: o parcial não serve
                            offset = 0
                        }
                        choice = fresh
                        total = fresh.contentLength
                    }
                    Chunk.EndOfFile -> {
                        if (offset == 0L) throw StreamHttpException(416)
                        break
                    }
                }
            }
            out.flush()
            out.fd.sync()
        }

        val expected = total
        if (expected != null && partial.length() != expected) throw IOException("aurelle:incomplete")
        if (partial.length() == 0L) throw IOException("aurelle:empty_file")
    }

    private fun readChunk(
        ctx: CoroutineContext,
        url: String,
        position: Long,
        length: Long,
        requestNumber: Int,
        out: FileOutputStream,
        buffer: ByteArray,
        onBytes: (Long) -> Unit,
    ): Chunk {
        val request = YoutubeStreamRequest.build(url, position, length, requestNumber)
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                return when (resp.code) {
                    403, 404, 410 -> Chunk.Expired
                    416 -> Chunk.EndOfFile
                    else -> throw StreamHttpException(resp.code)
                }
            }
            val body = resp.body ?: throw IOException("aurelle:empty_body")
            var read = 0L
            body.byteStream().use { input ->
                while (true) {
                    ctx.ensureActive()
                    val n = input.read(buffer)
                    if (n == -1) break
                    out.write(buffer, 0, n)
                    read += n
                    onBytes(n.toLong())
                }
            }
            return Chunk.Read(read)
        }
    }

    private companion object {
        const val CHUNK_SIZE = 1L * 1024 * 1024
        const val BUFFER_SIZE = 16 * 1024
    }
}
