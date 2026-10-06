package com.aurelle.music.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurelle.music.R
import com.aurelle.music.download.DownloadedTrack
import com.aurelle.music.ui.components.AlbumCard
import com.aurelle.music.ui.components.PlayShuffleButtons
import com.aurelle.music.ui.components.ScreenTopBar
import com.aurelle.music.ui.components.SectionMessage
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleOnBackground

/** Artistas da Biblioteca, em ordem alfabética com a letra de cada grupo. Tocar abre o artista. */
@Composable
fun LibraryArtistsScreen(
    viewModel: LibraryViewModel,
    actions: LibraryActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenTopBar(onBack = onBack, title = stringResource(R.string.library_artists)) }
        if (state.artists.isEmpty()) {
            item { SectionMessage(R.string.library_no_artists) }
        } else {
            var previousInitial = ""
            state.artists.forEach { artist ->
                val initial = artist.name.initialLetter()
                if (initial != previousInitial) {
                    previousInitial = initial
                    item(key = "initial|$initial") {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium,
                            color = AurelleGold,
                            modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 2.dp),
                        )
                    }
                }
                item(key = "artist|${artist.name}") {
                    LibraryArtistRow(
                        artist = artist,
                        onClick = { actions.onOpenArtist(artist.name) },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        }
    }
}

/** "beyoncé" -> "B"; nomes que não começam com letra (números, "—") ficam sob "#". */
private fun String.initialLetter(): String {
    val first = trim().firstOrNull() ?: return "#"
    val base = java.text.Normalizer.normalize(first.toString(), java.text.Normalizer.Form.NFD)
        .firstOrNull() ?: first
    return if (base.isLetter()) base.uppercaseChar().toString() else "#"
}

/** Todos os álbuns da Biblioteca em grade de 2 colunas, com ordenação. Tocar abre o álbum. */
@Composable
fun LibraryAlbumsScreen(
    viewModel: LibraryViewModel,
    actions: LibraryActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val defaultSort = rememberDefaultLibrarySort()
    var sort by rememberSaveable { mutableStateOf(defaultSort) }
    val albums = remember(state.albums, sort) {
        when (sort) {
            LibrarySort.RECENT -> state.albums.sortedByDescending { it.lastDownloadedAt }
            LibrarySort.TITLE -> state.albums.sortedBy { it.title.lowercase() }
            LibrarySort.ARTIST -> state.albums.sortedWith(
                compareBy<LibraryAlbum> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
            )
        }
    }

    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxSize()) {
        ScreenTopBar(onBack = onBack, title = stringResource(R.string.library_albums))
        if (state.albums.isEmpty()) {
            SectionMessage(R.string.library_no_albums)
        } else {
            SortChips(selected = sort, onSelect = { sort = it })
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            ) {
                items(albums, key = { "${it.title}|${it.artist}" }) { album ->
                    AlbumCard(
                        title = album.title,
                        subtitle = album.artist,
                        imageUrl = album.imageUrl,
                        onClick = { actions.onOpenAlbum(album) },
                    )
                }
            }
        }
    }
}

/** Todas as músicas baixadas, com Tocar/Aleatório e ordenação. */
@Composable
fun LibrarySongsScreen(
    viewModel: LibraryViewModel,
    actions: LibraryActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val defaultSort = rememberDefaultLibrarySort()
    var sort by rememberSaveable { mutableStateOf(defaultSort) }
    var toRemove by remember { mutableStateOf<DownloadedTrack?>(null) }
    val tracks = remember(state.tracks, sort) { state.tracks.sortedFor(sort) }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenTopBar(onBack = onBack, title = stringResource(R.string.library_songs)) }
        if (tracks.isEmpty()) {
            item { SectionMessage(R.string.library_no_match) }
        } else {
            item {
                PlayShuffleButtons(
                    onPlay = { actions.onPlay(tracks.first(), tracks) },
                    onShuffle = { actions.onShuffle(tracks) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                SortChips(selected = sort, onSelect = { sort = it })
                Spacer(Modifier.height(8.dp))
            }
            items(tracks, key = { it.key }) { track ->
                LibraryTrackRow(
                    track = track,
                    playing = track.id == actions.playingId,
                    onClick = { actions.onPlay(track, tracks) },
                    onRemove = { toRemove = track },
                    showAlbum = true,
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
