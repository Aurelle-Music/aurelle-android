package com.aurelle.music.player

import android.net.Uri
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** Motivo pelo qual uma música não pôde ser resolvida (não adianta tentar de novo). */
enum class UnavailableReason { AGE_RESTRICTED, UNAVAILABLE, GENERIC }

/**
 * Erro "definitivo" de extração. A mensagem leva uma etiqueta (`aurelle:<MOTIVO>`) porque o erro
 * atravessa a MediaSession até o controller, que só preserva a mensagem da causa.
 */
class StreamUnavailableException(val reason: UnavailableReason, cause: Throwable? = null) :
    IOException("aurelle:${reason.name}", cause)

/**
 * Transforma a URL de uma página de vídeo (id da [com.aurelle.music.data.Song]) na URL do stream de áudio.
 *
 * - Roda em thread de carregamento do ExoPlayer (bloqueante, não chamar na thread principal).
 * - Guarda o resultado em cache até perto da expiração (parâmetro `expire` da URL do YouTube).
 */
object StreamResolver {

    private class Resolved(val url: String, val expiresAtMs: Long)

    private val cache = ConcurrentHashMap<String, Resolved>()
    private val locks = ConcurrentHashMap<String, Any>()

    private const val DEFAULT_TTL_MS = 2 * 60 * 60 * 1000L   // 2 h, se a URL não informar `expire`
    private const val SAFETY_MARGIN_MS = 10 * 60 * 1000L     // renova 10 min antes de expirar

    @Throws(IOException::class)
    fun resolve(videoUrl: String, forceRefresh: Boolean = false): String {
        val lock = locks.getOrPut(videoUrl) { Any() }
        synchronized(lock) {
            if (!forceRefresh) {
                val cached = cache[videoUrl]
                if (cached != null && cached.expiresAtMs > System.currentTimeMillis()) return cached.url
            }
            val url = extract(videoUrl)
            cache[videoUrl] = Resolved(url, expiryOf(url))
            return url
        }
    }

    fun invalidate(videoUrl: String) {
        cache.remove(videoUrl)
    }

    private fun fetchInfo(videoUrl: String): StreamInfo =
        try {
            StreamInfo.getInfo(videoUrl)
        } catch (e: AgeRestrictedContentException) {
            throw StreamUnavailableException(UnavailableReason.AGE_RESTRICTED, e)
        } catch (e: ContentNotAvailableException) {
            throw StreamUnavailableException(UnavailableReason.UNAVAILABLE, e)
        } catch (e: ExtractionException) {
            throw StreamUnavailableException(UnavailableReason.GENERIC, e)
        }
        // IOException (rede) passa direto: o ExoPlayer tenta de novo e, se falhar, mostramos "sem conexão".

    private fun extract(videoUrl: String): String {
        val stream = pickBestAudio(fetchInfo(videoUrl).audioStreams)
            ?: throw StreamUnavailableException(UnavailableReason.UNAVAILABLE)
        return stream.content
    }

    /** Só streams progressivos (URL direta), preferindo a faixa original (não dublada). */
    private fun playableOriginals(streams: List<AudioStream>): List<AudioStream> {
        val playable = streams.filter {
            it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.content.isNotEmpty()
        }
        val original = playable.filter {
            val type = it.audioTrackType
            type == null || type == AudioTrackType.ORIGINAL
        }
        return original.ifEmpty { playable }
    }

    /** Maior bitrate primeiro; em empate, M4A. */
    private fun sortedByQuality(streams: List<AudioStream>): List<AudioStream> =
        streams.sortedWith(
            compareByDescending<AudioStream> { it.averageBitrate }
                .thenByDescending { it.format?.suffix == "m4a" }
        )

    /** Para tocar: o melhor stream disponível. */
    private fun pickBestAudio(streams: List<AudioStream>): AudioStream? =
        sortedByQuality(playableOriginals(streams)).firstOrNull()

    // --- Download (Etapa 4) ---------------------------------------------------------------------

    /** Stream de áudio escolhido para baixar (sem reconverter: o arquivo é o original do YouTube). */
    class AudioChoice(
        val url: String,
        /** Extensão do arquivo: `m4a`, `webm`, `opus`... */
        val suffix: String,
        val mimeType: String,
        /** Bitrate real do stream em kbps (0 = desconhecido). */
        val bitrateKbps: Int,
        /** Tamanho total em bytes (parâmetro `clen` da URL), se o YouTube informar. */
        val contentLength: Long?,
    )

    /**
     * Escolhe o stream para baixar. [maxKbps] = teto desejado (null = o melhor disponível).
     * Nunca "sobe" a qualidade: se nenhum stream couber no teto, usa o de menor bitrate.
     * Não usa o cache (é uma chamada pontual, feita pelo worker de download).
     */
    @Throws(IOException::class)
    fun resolveForDownload(videoUrl: String, maxKbps: Int?): AudioChoice {
        val sorted = sortedByQuality(playableOriginals(fetchInfo(videoUrl).audioStreams))
        val stream = if (maxKbps == null) {
            sorted.firstOrNull()
        } else {
            // Tolerância de 10%: o M4A "128 kbps" do YouTube costuma vir como 129-130.
            val limit = maxKbps * 1.10
            sorted.firstOrNull { it.averageBitrate in 1..limit.toInt() } ?: sorted.lastOrNull()
        } ?: throw StreamUnavailableException(UnavailableReason.UNAVAILABLE)

        val url = stream.content
        val uri = Uri.parse(url)
        val mime = stream.format?.mimeType
            ?: runCatching { uri.getQueryParameter("mime") }.getOrNull()
            ?: "audio/mp4"
        val suffix = stream.format?.suffix ?: if ("webm" in mime) "webm" else "m4a"
        val clen = runCatching { uri.getQueryParameter("clen") }.getOrNull()?.toLongOrNull()
        return AudioChoice(url, suffix, mime, stream.averageBitrate.coerceAtLeast(0), clen)
    }

    private fun expiryOf(streamUrl: String): Long {
        val expireSeconds = runCatching { Uri.parse(streamUrl).getQueryParameter("expire") }
            .getOrNull()?.toLongOrNull()
        val now = System.currentTimeMillis()
        return if (expireSeconds != null) {
            (expireSeconds * 1000L - SAFETY_MARGIN_MS).coerceAtLeast(now + 60_000L)
        } else {
            now + DEFAULT_TTL_MS
        }
    }
}
