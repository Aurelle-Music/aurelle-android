package com.aurelle.music.download

import androidx.work.Data

/** Onde salvar: na pasta Música do aparelho (MediaStore) ou dentro do armazenamento interno do app. */
enum class DownloadTarget { DEVICE, APP }

/**
 * Qualidade pedida. [maxKbps] é o TETO: baixamos o melhor stream original que caiba nele (sem reconverter).
 * `null` = o melhor stream original disponível.
 */
enum class DownloadQuality(val maxKbps: Int?) { LOW(128), MID(192), HIGH(256), MAX(null) }

enum class DownloadError { NO_NETWORK, NO_SPACE, AGE_RESTRICTED, UNAVAILABLE, STORAGE_PERMISSION, GENERIC }

data class DownloadChoice(val target: DownloadTarget, val quality: DownloadQuality)

/**
 * Chave de um download. Uma música pode ter UMA cópia por destino (ex.: baixa qualidade no Dispositivo e
 * qualidade máxima no App), então o que identifica um arquivo é o par (música, destino).
 */
fun downloadKey(songId: String, target: DownloadTarget): String = "${target.name}|$songId"

/** Música que o usuário quer baixar (vem do player, de um card da Home etc.). */
data class DownloadRequest(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val durationSeconds: Long?,
    /** Álbum e posição da faixa, quando a música vem de uma página de álbum (ou do player, que os guarda). */
    val album: String? = null,
    val trackNumber: Int? = null,
) {
    fun toJob(choice: DownloadChoice) = DownloadJob(
        id = id,
        title = title,
        artist = artist,
        imageUrl = imageUrl,
        durationSeconds = durationSeconds,
        target = choice.target,
        quality = choice.quality,
        album = album,
        trackNumber = trackNumber,
    )
}

/** Registro de uma música baixada (a Etapa 5 troca o armazenamento por um banco Room). */
data class DownloadedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val durationSeconds: Long?,
    val target: DownloadTarget,
    /** DEVICE: `content://` (ou `file://` no Android < 10). APP: caminho absoluto do arquivo. */
    val uri: String,
    /** Extensão real do arquivo: `m4a`, `webm`, `opus`... */
    val format: String,
    /** Bitrate real do stream baixado (0 = desconhecido). */
    val bitrateKbps: Int,
    val sizeBytes: Long,
    val downloadedAt: Long,
    val quality: DownloadQuality,
    /** Álbum e posição da faixa (nulos em músicas baixadas fora de uma página de álbum). */
    val album: String? = null,
    val trackNumber: Int? = null,
) {
    val key: String get() = downloadKey(id, target)
}

/** Estado de um download para a UI. Ausente do mapa = não baixada neste destino. */
sealed interface DownloadStatus {
    data class Queued(val retrying: Boolean = false) : DownloadStatus
    /** [percent] = -1 quando o tamanho total ainda é desconhecido. */
    data class Downloading(val percent: Int) : DownloadStatus
    data class Done(val track: DownloadedTrack) : DownloadStatus
    data class Failed(val error: DownloadError) : DownloadStatus
}

/** Estado de uma música nos dois destinos. */
data class SongDownloads(val device: DownloadStatus? = null, val app: DownloadStatus? = null) {

    fun of(target: DownloadTarget): DownloadStatus? = if (target == DownloadTarget.DEVICE) device else app

    private val all get() = listOfNotNull(device, app)

    val isActive: Boolean get() = all.any { it.isActive }

    /** Estado único para o botão: em andamento > baixada em algum destino > falhou > nada. */
    val summary: DownloadStatus?
        get() = all.filter { it.isActive }.maxByOrNull { (it as? DownloadStatus.Downloading)?.percent ?: -1 }
            ?: all.firstOrNull { it is DownloadStatus.Done }
            ?: all.firstOrNull { it is DownloadStatus.Failed }
}

val DownloadStatus.isActive: Boolean
    get() = this is DownloadStatus.Queued || this is DownloadStatus.Downloading

/** Estado da música [songId] nos dois destinos, a partir do mapa do [DownloadManager]. */
fun Map<String, DownloadStatus>.forSong(songId: String) = SongDownloads(
    device = this[downloadKey(songId, DownloadTarget.DEVICE)],
    app = this[downloadKey(songId, DownloadTarget.APP)],
)

/** Dados de entrada do [DownloadWorker]. */
data class DownloadJob(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val durationSeconds: Long?,
    val target: DownloadTarget,
    val quality: DownloadQuality,
    val album: String? = null,
    val trackNumber: Int? = null,
) {
    fun toData(): Data = Data.Builder()
        .putString(K_ID, id)
        .putString(K_TITLE, title)
        .putString(K_ARTIST, artist)
        .putString(K_IMAGE, imageUrl)
        .putLong(K_DURATION, durationSeconds ?: -1L)
        .putString(K_TARGET, target.name)
        .putString(K_QUALITY, quality.name)
        .putString(K_ALBUM, album)
        .putInt(K_TRACK, trackNumber ?: -1)
        .build()

    companion object {
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"

        private const val K_ID = "id"
        private const val K_TITLE = "title"
        private const val K_ARTIST = "artist"
        private const val K_IMAGE = "image"
        private const val K_DURATION = "duration"
        private const val K_TARGET = "target"
        private const val K_QUALITY = "quality"
        private const val K_ALBUM = "album"
        private const val K_TRACK = "track"

        fun from(data: Data): DownloadJob? {
            val id = data.getString(K_ID) ?: return null
            return DownloadJob(
                id = id,
                title = data.getString(K_TITLE).orEmpty(),
                artist = data.getString(K_ARTIST).orEmpty(),
                imageUrl = data.getString(K_IMAGE),
                durationSeconds = data.getLong(K_DURATION, -1L).takeIf { it >= 0 },
                target = runCatching { DownloadTarget.valueOf(data.getString(K_TARGET).orEmpty()) }
                    .getOrDefault(DownloadTarget.DEVICE),
                quality = runCatching { DownloadQuality.valueOf(data.getString(K_QUALITY).orEmpty()) }
                    .getOrDefault(DownloadQuality.MAX),
                album = data.getString(K_ALBUM)?.takeIf { it.isNotBlank() },
                trackNumber = data.getInt(K_TRACK, -1).takeIf { it > 0 },
            )
        }
    }
}
