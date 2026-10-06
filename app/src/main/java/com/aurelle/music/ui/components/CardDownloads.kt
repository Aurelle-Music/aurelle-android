package com.aurelle.music.ui.components

import androidx.compose.runtime.Immutable
import com.aurelle.music.data.MediaItem
import com.aurelle.music.data.Song
import com.aurelle.music.download.DownloadStatus
import com.aurelle.music.download.SongDownloads
import com.aurelle.music.download.forSong

/** Liga os cards da Home / "ver tudo" aos downloads (estado + abrir folha + cancelar). Artistas não baixam. */
@Immutable
class CardDownloads(
    private val statuses: Map<String, DownloadStatus>,
    private val onOpen: (Song) -> Unit,
    private val onCancel: (String) -> Unit,
) {
    fun of(item: MediaItem): SongDownloads? = (item as? Song)?.let { statuses.forSong(it.id) }
    fun open(item: MediaItem) = (item as? Song)?.let(onOpen)
    fun cancel(item: MediaItem) = onCancel(item.id)
}
