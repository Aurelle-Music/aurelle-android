package com.aurelle.music.player

import androidx.media3.common.C
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getVisionOsUserAgent
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.isVisionOsStreamingUrl
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.isWebStreamingUrl

/**
 * Monta a requisição HTTP que o YouTube espera para um stream de áudio (mesmo comportamento do NewPipe).
 * Compartilhada pelo player ([AurelleDataSource]) e pelo downloader (`download/AudioDownloader`):
 * - `POST` com corpo `[0x78, 0x00]`;
 * - parâmetro `&rn=<n>` em cada requisição;
 * - posição/tamanho pelo parâmetro `&range=<ini>-<fim>` (e não pelo cabeçalho Range).
 */
object YoutubeStreamRequest {

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0"
    private const val YOUTUBE_URL = "https://www.youtube.com"
    private val POST_BODY = byteArrayOf(0x78, 0)

    /** [length] = [C.LENGTH_UNSET] pede "até o fim". */
    fun build(streamUrl: String, position: Long, length: Long, requestNumber: Int): Request {
        var url = streamUrl
        val unset = C.LENGTH_UNSET.toLong()
        val isPlaybackUrl = android.net.Uri.parse(streamUrl).path?.startsWith("/videoplayback") == true

        val builder = Request.Builder()
        if (isPlaybackUrl) {
            if (!url.contains("&rn=")) url += "&rn=$requestNumber"
            if (position != 0L || length != unset) {
                url += "&range=$position-" + if (length != unset) position + length - 1 else ""
            }
            builder.post(POST_BODY.toRequestBody(null))
        } else if (position != 0L || length != unset) {
            val end = if (length != unset) (position + length - 1).toString() else ""
            builder.header("Range", "bytes=$position-$end")
        }

        builder.url(url)
        builder.header(
            "User-Agent",
            if (isVisionOsStreamingUrl(url)) getVisionOsUserAgent(null) else USER_AGENT,
        )
        builder.header("Accept-Encoding", "identity") // sem gzip: o Content-Length precisa ser o real
        if (isWebStreamingUrl(url)) {
            builder.header("Origin", YOUTUBE_URL)
            builder.header("Referer", YOUTUBE_URL)
        }
        return builder.build()
    }
}
