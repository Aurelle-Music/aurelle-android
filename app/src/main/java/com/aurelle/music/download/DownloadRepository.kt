package com.aurelle.music.download

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Registro (banco Room) do que foi baixado. O resto do app só fala com esta classe.
 * Uma música pode ter uma cópia por destino; o mapa [items] usa [downloadKey] como chave.
 */
class DownloadRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = DownloadDatabase.create(appContext).downloads()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Músicas baixadas, por [downloadKey]. */
    val items: StateFlow<Map<String, DownloadedTrack>> = dao.observeAll()
        .map { list -> list.map { it.toTrack() }.associateBy { it.key } }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    init {
        scope.launch { migrateLegacyJson() }
    }

    suspend fun get(id: String, target: DownloadTarget): DownloadedTrack? =
        dao.get(id, target.name)?.toTrack()

    suspend fun put(track: DownloadedTrack) = dao.upsert(track.toEntity())

    suspend fun remove(id: String, target: DownloadTarget) = dao.delete(id, target.name)

    /**
     * Confere se os arquivos ainda existem e tira do registro os que sumiram (o usuário apagou pela
     * galeria/gerenciador de arquivos ou limpou os dados do app). Devolve quantos foram removidos.
     */
    suspend fun removeMissingFiles(): Int {
        var removed = 0
        for (entity in dao.getAll()) {
            val track = entity.toTrack()
            if (!fileExists(track)) {
                dao.delete(track.id, track.target.name)
                removed++
            }
        }
        return removed
    }

    private fun fileExists(track: DownloadedTrack): Boolean = when (track.target) {
        DownloadTarget.APP -> File(track.uri).exists()
        DownloadTarget.DEVICE -> {
            val uri = Uri.parse(track.uri)
            if (uri.scheme == "file") {
                uri.path?.let { File(it).exists() } ?: false
            } else {
                // Se não der para consultar (ex.: permissão perdida), assume que existe: melhor não apagar à toa.
                runCatching {
                    appContext.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                        ?.use { it.count > 0 } ?: true
                }.getOrDefault(true)
            }
        }
    }

    // --- Migração do `downloads.json` das versões 0.4.x ------------------------------------------

    private suspend fun migrateLegacyJson() {
        val legacy = File(appContext.filesDir, "downloads.json")
        if (!legacy.exists()) return
        val tracks = runCatching {
            val array = JSONArray(legacy.readText())
            buildList {
                for (i in 0 until array.length()) fromJson(array.getJSONObject(i))?.let(::add)
            }
        }.getOrNull() ?: return // JSON ilegível: deixa o arquivo como está
        dao.upsertAll(tracks.map { it.toEntity() })
        legacy.renameTo(File(appContext.filesDir, "downloads.json.migrated"))
    }

    private fun fromJson(o: JSONObject): DownloadedTrack? = runCatching {
        val target = DownloadTarget.valueOf(o.getString("target"))
        val uri = o.getString("uri")
        // Arquivo do modo App que sumiu: não conta como baixado.
        if (target == DownloadTarget.APP && !File(uri).exists()) return null
        DownloadedTrack(
            id = o.getString("id"),
            title = o.getString("title"),
            artist = o.getString("artist"),
            imageUrl = if (o.isNull("imageUrl")) null else o.getString("imageUrl"),
            durationSeconds = if (o.isNull("durationSeconds")) null else o.getLong("durationSeconds"),
            target = target,
            uri = uri,
            format = o.getString("format"),
            bitrateKbps = o.getInt("bitrateKbps"),
            sizeBytes = o.getLong("sizeBytes"),
            downloadedAt = o.getLong("downloadedAt"),
            quality = DownloadQuality.valueOf(o.getString("quality")),
        )
    }.getOrNull()
}
