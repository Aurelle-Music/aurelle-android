package com.aurelle.music.ui.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurelle.music.R
import com.aurelle.music.data.Album
import com.aurelle.music.data.Load
import com.aurelle.music.data.Song
import com.aurelle.music.data.compactCount
import com.aurelle.music.download.SongDownloads
import com.aurelle.music.ui.components.AlbumCard
import com.aurelle.music.ui.components.CardDownloads
import com.aurelle.music.ui.components.CoverImage
import com.aurelle.music.ui.components.PlayShuffleButtons
import com.aurelle.music.ui.components.ScreenTopBar
import com.aurelle.music.ui.components.SectionHeader
import com.aurelle.music.ui.components.SectionMessage
import com.aurelle.music.ui.components.SongRow
import com.aurelle.music.ui.components.SongRowSkeleton
import com.aurelle.music.ui.components.shimmer
import com.aurelle.music.ui.components.staggeredEntrance
import com.aurelle.music.ui.theme.AurelleHint
import com.aurelle.music.ui.theme.AurelleOnBackground
import com.aurelle.music.ui.theme.AurelleSurface

private const val SONGS_PREVIEW = 5
private const val ALBUMS_PREVIEW = 10
private val AlbumCardWidth = 140.dp

/**
 * Página do artista (aberta ao tocar num artista da Home / "ver tudo"): foto, Tocar/Aleatório,
 * prévia das músicas e dos álbuns. "See all" abre a lista completa de cada um; tocar num álbum abre o álbum.
 */
@Composable
fun ArtistScreen(
    ref: ArtistRef,
    songs: Load<List<Song>>,
    albums: Load<List<Album>>,
    songsHasMore: Boolean,
    albumsHasMore: Boolean,
    playingId: String?,
    downloads: CardDownloads,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (song: Song, queue: List<Song>) -> Unit,
    onShuffle: (queue: List<Song>) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenAllSongs: () -> Unit,
    onOpenAllAlbums: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val songList = (songs as? Load.Ready)?.value.orEmpty()

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
                    imageUrl = ref.imageUrl,
                    shape = CircleShape,
                    icon = Icons.Filled.Person,
                    modifier = Modifier.size(168.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = ref.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = AurelleOnBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = ref.subscribers
                        ?.let { stringResource(R.string.subscribers_format, compactCount(it)) }
                        ?: stringResource(R.string.artist_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = AurelleHint,
                )
                Spacer(Modifier.height(16.dp))
                PlayShuffleButtons(
                    onPlay = { songList.firstOrNull()?.let { onPlay(it, songList) } },
                    onShuffle = { onShuffle(songList) },
                    enabled = songList.isNotEmpty(),
                )
            }
        }

        // --- Músicas -----------------------------------------------------------------------------
        item {
            SectionHeader(
                title = stringResource(R.string.artist_songs),
                modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp),
                onClick = onOpenAllSongs.takeIf { songList.size > SONGS_PREVIEW || songsHasMore },
            )
        }
        when (songs) {
            Load.Loading -> items(SONGS_PREVIEW) {
                SongRowSkeleton(Modifier.padding(horizontal = 20.dp))
            }
            Load.Failed -> item { SectionMessage(R.string.error_loading, onRetry = onRetry) }
            is Load.Ready -> if (songList.isEmpty()) {
                item { SectionMessage(R.string.no_results) }
            } else {
                items(songList.take(SONGS_PREVIEW), key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        subtitle = song.artist,
                        playing = song.id == playingId,
                        downloads = downloads.of(song) ?: SongDownloads(),
                        onClick = { onPlay(song, songList) },
                        onDownloadClick = { downloads.open(song) },
                        onDownloadCancel = { downloads.cancel(song) },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        }

        // --- Álbuns ------------------------------------------------------------------------------
        item {
            val albumCount = (albums as? Load.Ready)?.value?.size ?: 0
            SectionHeader(
                title = stringResource(R.string.artist_albums),
                modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp),
                onClick = onOpenAllAlbums.takeIf { albumCount > ALBUMS_PREVIEW || albumsHasMore },
            )
        }
        item {
            when (albums) {
                Load.Loading -> LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = false,
                ) {
                    items(3) { AlbumSkeleton(Modifier.size(AlbumCardWidth)) }
                }
                Load.Failed -> SectionMessage(R.string.error_loading, onRetry = onRetry)
                is Load.Ready -> if (albums.value.isEmpty()) {
                    SectionMessage(R.string.artist_no_albums)
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(albums.value.take(ALBUMS_PREVIEW), key = { it.id }) { album ->
                            AlbumCard(
                                title = album.title,
                                subtitle = album.artist,
                                imageUrl = album.imageUrl,
                                modifier = Modifier.width(AlbumCardWidth),
                                onClick = { onOpenAlbum(album) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AlbumSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AurelleSurface)
            .shimmer()
    )
}
