package com.aurelle.music.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Album as AlbumIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurelle.music.R
import com.aurelle.music.download.DownloadTarget
import com.aurelle.music.download.DownloadedTrack
import com.aurelle.music.ui.components.AlbumCard
import com.aurelle.music.ui.components.AurelleLogo
import com.aurelle.music.ui.components.AurelleSearchBar
import com.aurelle.music.ui.components.SectionHeader
import com.aurelle.music.ui.components.SectionMessage
import com.aurelle.music.ui.components.staggeredEntrance

private const val RECENT_ALBUMS = 6
private const val RECENT_SONGS = 5

/**
 * Biblioteca (raiz): alternância Dispositivo ↔ App, busca e as categorias Artistas / Álbuns / Músicas,
 * cada uma abrindo a própria tela (e daí Artista → Álbuns → Músicas), como no Apple Music.
 * Abaixo, o que foi baixado há pouco. Com texto na busca, mostra os resultados separados por tipo.
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    actions: LibraryActions,
    onOpenArtists: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenSongs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toRemove by remember { mutableStateOf<DownloadedTrack?>(null) }

    // Ao abrir a aba, confere se algum arquivo foi apagado por fora do app.
    LaunchedEffect(Unit) { viewModel.refresh() }

    val search = state.search
    val isEmpty = state.tracks.isEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            AurelleLogo(
                Modifier
                    .padding(horizontal = 48.dp, vertical = 14.dp)
                    .staggeredEntrance(0)
            )
        }
        item {
            Column(Modifier.staggeredEntrance(1)) {
                SourceToggle(
                    source = state.source,
                    deviceCount = state.deviceCount,
                    appCount = state.appCount,
                    onSelect = viewModel::selectSource,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                AurelleSearchBar(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    hint = R.string.library_search_hint,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        when {
            isEmpty -> item {
                SectionMessage(
                    if (state.source == DownloadTarget.DEVICE) R.string.library_empty_device
                    else R.string.library_empty_app,
                    modifier = Modifier.staggeredEntrance(2),
                )
            }

            search != null -> searchResults(search, actions) { toRemove = it }

            else -> {
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .staggeredEntrance(2),
                    ) {
                        CategoryRow(Icons.Filled.Person, R.string.library_artists, state.artists.size, onOpenArtists)
                        CategoryRow(Icons.Filled.AlbumIcon, R.string.library_albums, state.albums.size, onOpenAlbums)
                        CategoryRow(Icons.Filled.MusicNote, R.string.library_songs, state.tracks.size, onOpenSongs)
                    }
                }

                if (state.albums.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.library_recent_albums),
                            onClick = onOpenAlbums,
                            modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp),
                        )
                    }
                    // Grade de 2 colunas feita de linhas (a tela toda já é uma lista rolável).
                    items(state.albums.take(RECENT_ALBUMS).chunked(2), key = { row -> row.first().let { "${it.title}|${it.artist}" } }) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            row.forEach { album ->
                                AlbumCard(
                                    title = album.title,
                                    subtitle = album.artist,
                                    imageUrl = album.imageUrl,
                                    modifier = Modifier.weight(1f),
                                    onClick = { actions.onOpenAlbum(album) },
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = stringResource(R.string.library_recent_songs),
                        onClick = onOpenSongs,
                        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp),
                    )
                }
                items(state.tracks.take(RECENT_SONGS), key = { it.key }) { track ->
                    LibraryTrackRow(
                        track = track,
                        playing = track.id == actions.playingId,
                        onClick = { actions.onPlay(track, state.tracks) },
                        onRemove = { toRemove = track },
                        showAlbum = true,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
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

/** Resultados da busca: artistas, álbuns e músicas que casaram, cada grupo com o próprio título. */
private fun androidx.compose.foundation.lazy.LazyListScope.searchResults(
    result: LibrarySearchResult,
    actions: LibraryActions,
    onRemove: (DownloadedTrack) -> Unit,
) {
    if (result.isEmpty) {
        item { SectionMessage(R.string.library_no_match) }
        return
    }
    if (result.artists.isNotEmpty()) {
        item { SearchHeader(R.string.library_artists) }
        items(result.artists, key = { "artist|${it.name}" }) { artist ->
            LibraryArtistRow(
                artist = artist,
                onClick = { actions.onOpenArtist(artist.name) },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
    if (result.albums.isNotEmpty()) {
        item { SearchHeader(R.string.library_albums) }
        items(result.albums, key = { "album|${it.title}|${it.artist}" }) { album ->
            LibraryAlbumRow(
                album = album,
                onClick = { actions.onOpenAlbum(album) },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
    if (result.tracks.isNotEmpty()) {
        item { SearchHeader(R.string.library_songs) }
        items(result.tracks, key = { "track|${it.key}" }) { track ->
            LibraryTrackRow(
                track = track,
                playing = track.id == actions.playingId,
                onClick = { actions.onPlay(track, result.tracks) },
                onRemove = { onRemove(track) },
                showAlbum = true,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
private fun SearchHeader(title: Int) {
    SectionHeader(
        title = stringResource(title),
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 8.dp),
    )
}
