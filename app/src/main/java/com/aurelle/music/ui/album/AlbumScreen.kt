package com.aurelle.music.ui.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurelle.music.R
import com.aurelle.music.data.Album
import com.aurelle.music.data.AlbumDetail
import com.aurelle.music.data.Load
import com.aurelle.music.data.Song
import com.aurelle.music.download.SongDownloads
import com.aurelle.music.ui.components.CardDownloads
import com.aurelle.music.ui.components.CoverImage
import com.aurelle.music.ui.components.PlayShuffleButtons
import com.aurelle.music.ui.components.ScreenTopBar
import com.aurelle.music.ui.components.SectionMessage
import com.aurelle.music.ui.components.SongRow
import com.aurelle.music.ui.components.SongRowSkeleton
import com.aurelle.music.ui.components.staggeredEntrance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album as AlbumIcon
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleHint
import com.aurelle.music.ui.theme.AurelleOnBackground

/**
 * Página de um álbum online: capa, artista (leva à página do artista), Tocar/Aleatório e as faixas
 * numeradas, cada uma com botão de baixar (o download guarda o álbum e o número da faixa).
 */
@Composable
fun AlbumScreen(
    album: Album,
    detail: Load<AlbumDetail>,
    playingId: String?,
    downloads: CardDownloads,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (song: Song, queue: List<Song>) -> Unit,
    onShuffle: (queue: List<Song>) -> Unit,
    onOpenArtist: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = (detail as? Load.Ready)?.value
    // Depois de carregar, a capa e o artista podem vir mais completos que os da rota.
    val shown = ready?.album ?: album
    val tracks = ready?.tracks.orEmpty()
    val resources = LocalContext.current.resources

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ScreenTopBar(onBack = onBack) }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .staggeredEntrance(0),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CoverImage(
                    imageUrl = shown.imageUrl,
                    shape = RoundedCornerShape(20.dp),
                    icon = Icons.Filled.AlbumIcon,
                    modifier = Modifier.size(220.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = shown.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = AurelleOnBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (shown.artist.isNotBlank()) {
                    val canOpen = !shown.artistUrl.isNullOrBlank()
                    Text(
                        text = shown.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = AurelleGoldLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .then(if (canOpen) Modifier.clickable { onOpenArtist(shown) } else Modifier)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                if (tracks.isNotEmpty()) {
                    val minutes = (tracks.sumOf { it.durationSeconds ?: 0L } / 60L).toInt()
                    val count = resources.getQuantityString(R.plurals.songs_count, tracks.size, tracks.size)
                    Text(
                        text = if (minutes > 0) "$count · ${resources.getString(R.string.minutes_format, minutes)}" else count,
                        style = MaterialTheme.typography.labelMedium,
                        color = AurelleHint,
                    )
                }
                Spacer(Modifier.height(16.dp))
                PlayShuffleButtons(
                    onPlay = { tracks.firstOrNull()?.let { onPlay(it, tracks) } },
                    onShuffle = { onShuffle(tracks) },
                    enabled = tracks.isNotEmpty(),
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        when (detail) {
            Load.Loading -> items(6) {
                SongRowSkeleton(Modifier.padding(horizontal = 20.dp), withCover = false)
            }
            Load.Failed -> item { SectionMessage(R.string.error_loading, onRetry = onRetry) }
            is Load.Ready -> if (tracks.isEmpty()) {
                item { SectionMessage(R.string.no_results) }
            } else {
                items(tracks, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        subtitle = "",
                        playing = song.id == playingId,
                        downloads = downloads.of(song) ?: SongDownloads(),
                        onClick = { onPlay(song, tracks) },
                        onDownloadClick = { downloads.open(song) },
                        onDownloadCancel = { downloads.cancel(song) },
                        number = song.trackNumber,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        }
    }
}
