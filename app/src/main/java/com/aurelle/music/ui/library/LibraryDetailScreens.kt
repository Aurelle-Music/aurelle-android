package com.aurelle.music.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Album as AlbumIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurelle.music.R
import com.aurelle.music.data.formatSize
import com.aurelle.music.download.DownloadedTrack
import com.aurelle.music.ui.components.AlbumCard
import com.aurelle.music.ui.components.CoverImage
import com.aurelle.music.ui.components.PlayShuffleButtons
import com.aurelle.music.ui.components.ScreenTopBar
import com.aurelle.music.ui.components.SectionHeader
import com.aurelle.music.ui.components.SectionMessage
import com.aurelle.music.ui.components.staggeredEntrance
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleHint
import com.aurelle.music.ui.theme.AurelleOnBackground

/**
 * Um artista da Biblioteca: foto, Tocar/Aleatório, os álbuns dele (carrossel) e todas as músicas baixadas.
 * Tocar num álbum abre o álbum; é o caminho Artistas → Álbuns → Músicas.
 */
@Composable
fun LibraryArtistScreen(
    name: String,
    viewModel: LibraryViewModel,
    actions: LibraryActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val artist = state.artist(name)
    val albums = remember(state.albums, name) { state.albumsOf(name) }
    val tracks = remember(state.tracks, name) { state.tracksOf(name) }
    var toRemove by remember { mutableStateOf<DownloadedTrack?>(null) }

    // Removeu a última música do artista: volta para a lista (só se a Biblioteca ainda tem outras músicas).
    LaunchedEffect(artist == null, state.tracks.isNotEmpty()) {
        if (artist == null && state.tracks.isNotEmpty()) onBack()
    }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
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
                    imageUrl = artist?.imageUrl,
                    shape = CircleShape,
                    icon = Icons.Filled.Person,
                    modifier = Modifier.size(168.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = AurelleOnBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (artist != null) {
                    Text(
                        text = artistSummary(artist.albumCount, artist.songCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = AurelleHint,
                    )
                }
                Spacer(Modifier.height(16.dp))
                PlayShuffleButtons(
                    onPlay = { tracks.firstOrNull()?.let { actions.onPlay(it, tracks) } },
                    onShuffle = { actions.onShuffle(tracks) },
                    enabled = tracks.isNotEmpty(),
                )
            }
        }

        if (albums.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.library_albums),
                    modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(albums, key = { it.title }) { album ->
                        AlbumCard(
                            title = album.title,
                            subtitle = albumTracksLabel(album),
                            imageUrl = album.imageUrl,
                            modifier = Modifier.width(140.dp),
                            onClick = { actions.onOpenAlbum(album) },
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.library_songs),
                modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp),
            )
        }
        if (tracks.isEmpty()) {
            item { SectionMessage(R.string.library_no_match) }
        } else {
            items(tracks, key = { it.key }) { track ->
                LibraryTrackRow(
                    track = track,
                    playing = track.id == actions.playingId,
                    onClick = { actions.onPlay(track, tracks) },
                    onRemove = { toRemove = track },
                    showAlbum = true,
                    showArtist = false,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }

    toRemove?.let { track ->
        RemoveTrackDialog(
            track = track,
            onConfirm = {
                actions.onRemove(track)
                toRemove = null
            },
            onDismiss = { toRemove = null },
        )
    }
}

@Composable
private fun albumTracksLabel(album: LibraryAlbum): String {
    val resources = LocalContext.current.resources
    return resources.getQuantityString(R.plurals.songs_count, album.tracks.size, album.tracks.size)
}

/**
 * Um álbum da Biblioteca: capa, artista (leva à página do artista), Tocar/Aleatório e as faixas
 * na ordem do disco. Só aparecem as faixas que foram baixadas.
 */
@Composable
fun LibraryAlbumScreen(
    title: String,
    artist: String,
    viewModel: LibraryViewModel,
    actions: LibraryActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val album = state.album(title, artist)
    val tracks = album?.tracks.orEmpty()
    var toRemove by remember { mutableStateOf<DownloadedTrack?>(null) }
    val resources = LocalContext.current.resources

    // Removeu a última faixa do álbum: volta (só se a Biblioteca ainda tem outras músicas).
    LaunchedEffect(album == null, state.tracks.isNotEmpty()) {
        if (album == null && state.tracks.isNotEmpty()) onBack()
    }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
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
                    imageUrl = album?.imageUrl,
                    shape = RoundedCornerShape(20.dp),
                    icon = Icons.Filled.AlbumIcon,
                    modifier = Modifier.size(220.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = AurelleOnBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = AurelleGoldLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { actions.onOpenArtist(artist) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                if (tracks.isNotEmpty()) {
                    val count = resources.getQuantityString(R.plurals.songs_count, tracks.size, tracks.size)
                    Text(
                        text = "$count · ${formatSize(tracks.sumOf { it.sizeBytes })}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AurelleHint,
                    )
                }
                Spacer(Modifier.height(16.dp))
                PlayShuffleButtons(
                    onPlay = { tracks.firstOrNull()?.let { actions.onPlay(it, tracks) } },
                    onShuffle = { actions.onShuffle(tracks) },
                    enabled = tracks.isNotEmpty(),
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (tracks.isEmpty()) {
            item { SectionMessage(R.string.library_no_match) }
        } else {
            itemsIndexed(tracks, key = { _, track -> track.key }) { index, track ->
                LibraryTrackRow(
                    track = track,
                    playing = track.id == actions.playingId,
                    onClick = { actions.onPlay(track, tracks) },
                    onRemove = { toRemove = track },
                    number = track.trackNumber ?: (index + 1),
                    showArtist = false,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }

    toRemove?.let { track ->
        RemoveTrackDialog(
            track = track,
            onConfirm = {
                actions.onRemove(track)
                toRemove = null
            },
            onDismiss = { toRemove = null },
        )
    }
}
