package com.aurelle.music.download

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** Caminhos, nomes de arquivo e gravação na pasta Música do aparelho. */
object DownloadFiles {

    /** Pasta (dentro de Música/ ou Download/) onde o modo Dispositivo salva. */
    const val DEVICE_FOLDER = "Aurelle"

    /** Chave curta e estável para uma música (o id é uma URL, ruim para nome de arquivo). */
    fun key(songId: String): String =
        MessageDigest.getInstance("SHA-1").digest(songId.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)

    fun partialRoot(context: Context) = File(context.filesDir, "downloads/partial")

    /** Parciais ficam separados por destino: dois downloads da mesma música podem rodar juntos. */
    fun partialDir(context: Context, songId: String, target: DownloadTarget) =
        File(partialRoot(context), key(downloadKey(songId, target)))

    fun appFile(context: Context, songId: String, suffix: String) =
        File(context.filesDir, "downloads/library/${key(songId)}.$suffix")

    /** Apaga o arquivo de uma música baixada (MediaStore/arquivo do aparelho ou arquivo do modo App). */
    fun deleteStored(context: Context, track: DownloadedTrack, keepUri: String? = null) {
        if (track.uri == keepUri) return
        runCatching {
            when (track.target) {
                DownloadTarget.APP -> File(track.uri).delete()
                DownloadTarget.DEVICE -> {
                    val uri = Uri.parse(track.uri)
                    if (uri.scheme == "file") File(requireNotNull(uri.path)).delete()
                    else context.contentResolver.delete(uri, null, null)
                }
            }
        }
    }

    fun displayName(artist: String, title: String, suffix: String): String {
        val base = if (artist.isBlank()) title else "$artist - $title"
        val clean = base.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().take(120).ifBlank { "Aurelle" }
        return "$clean.$suffix"
    }

    /**
     * Copia [source] para a pasta Música do aparelho e devolve a URI do arquivo salvo.
     * - Android 10+: MediaStore (sem permissão de armazenamento). Se o MediaStore de áudio recusar o tipo
     *   (ex.: `audio/webm`), cai para a pasta Download/Aurelle.
     * - Android 8/9: grava em Music/Aurelle (precisa de WRITE_EXTERNAL_STORAGE) e avisa o scanner de mídia.
     */
    @Throws(IOException::class)
    fun saveToDevice(
        context: Context,
        source: File,
        displayName: String,
        mimeType: String,
        title: String,
        artist: String,
    ): String =
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                insertIntoMediaStore(
                    context, source, displayName, mimeType, title, artist,
                    collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    relativePath = "${Environment.DIRECTORY_MUSIC}/$DEVICE_FOLDER",
                    isAudio = true,
                )
            } catch (e: IllegalArgumentException) {
                insertIntoMediaStore(
                    context, source, displayName, mimeType, title, artist,
                    collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$DEVICE_FOLDER",
                    isAudio = false,
                )
            }
        } else {
            saveLegacy(context, source, displayName, mimeType)
        }

    @RequiresApi(29)
    private fun insertIntoMediaStore(
        context: Context,
        source: File,
        displayName: String,
        mimeType: String,
        title: String,
        artist: String,
        collection: Uri,
        relativePath: String,
        isAudio: Boolean,
    ): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            if (isAudio) {
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
            }
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore recusou o arquivo")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: throw IOException("Não foi possível abrir o arquivo de destino")
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
        return uri.toString()
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, source: File, displayName: String, mimeType: String): String {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), DEVICE_FOLDER)
        if (!dir.exists() && !dir.mkdirs()) throw SecurityException("Sem acesso à pasta Música")
        var target = File(dir, displayName)
        var n = 1
        while (target.exists()) {
            val dot = displayName.lastIndexOf('.')
            target = File(dir, displayName.substring(0, dot) + " ($n)" + displayName.substring(dot))
            n++
        }
        source.copyTo(target)
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mimeType), null)
        return Uri.fromFile(target).toString()
    }
}
