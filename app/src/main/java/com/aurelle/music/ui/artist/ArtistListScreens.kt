package com.aurelle.music.ui.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aurelle.music.R
import com.aurelle.music.data.Album
import com.aurelle.music.data.Load
import com.aurelle.music.data.Song
import com.aurelle.music.download.SongDownloads
import com.aurelle.music.ui.components.AlbumCard
import com.aurelle.music.ui.components.CardDownloads
import com.aurelle.music.ui.components.PlayShuffleButtons
import com.aurelle.music.ui.components.ScreenTopBar
import com.aurelle.music.ui.components.SectionMessage
import com.aurelle.music.ui.components.SongRow
import com.aurelle.music.ui.components.SongRowSkeleton
import com.aurelle.music.ui.theme.AurelleGold

/** Faltando tantos itens para o fim da lista, já pede a próxima página. */
private const val LOAD_MORE_THRESHOLD = 5

/** Todas as músicas do artista, com rolagem infinita. Tocar numa música cria a fila com a lista carregada. */
@Composable
fun ArtistSongsScreen(
    ref: ArtistRef,
    songs: Load<List<Song>>,
    paging: Paging,
    playingId: String?,
    downloads: CardDownloads,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (song: Song, queue: List<Song>) -> Unit,
    onShuffle: (queue: List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val songList = (songs as? Load.Ready)?.value.orEmpty()
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(nearEnd, songList.size, paging.canLoadMore) {
        if (nearEnd && paging.canLoadMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            ScreenTopBar(
                onBack = onBack,
                title = stringResource(R.string.artist_songs_title, ref.name),
            )
        }
        when (songs) {
            Load.Loading -> items(8) { SongRowSkeleton(Modifier.padding(horizontal = 20.dp)) }
            Load.Failed -> item { SectionMessage(R.string.error_loading, onRetry = onRetry) }
            is Load.Ready -> if (songList.isEmpty()) {
                item { SectionMessage(R.string.no_results) }
            } else {
                item {
                    PlayShuffleButtons(
                        onPlay = { onPlay(songList.first(), songList) },
                        onShuffle = { onShuffle(songList) },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
                items(songList, key = { it.id }) { song ->
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
                if (paging.isLoadingMore) item { LoadingMoreIndicator() }
            }
        }
    }
}

/** Todos os álbuns do artista em grade de 2 colunas, com rolagem infinita. Tocar abre o álbum. */
@Composable
fun ArtistAlbumsScreen(
    ref: ArtistRef,
    albums: Load<List<Album>>,
    paging: Paging,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    val albumList = (albums as? Load.Ready)?.value.orEmpty()
    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(nearEnd, albumList.size, paging.canLoadMore) {
        if (nearEnd && paging.canLoadMore) onLoadMore()
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenTopBar(
            onBack = onBack,
            title = stringResource(R.string.artist_albums_title, ref.name),
        )
        when (albums) {
            Load.Loading -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            ) {
                items(6) { AlbumSkeleton(Modifier.fillMaxWidth().aspectRatio(1f)) }
            }
            Load.Failed -> SectionMessage(R.string.error_loading, onRetry = onRetry)
            is Load.Ready -> if (albumList.isEmpty()) {
                SectionMessage(R.string.artist_no_albums)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                ) {
                    items(albumList, key = { it.id }) { album ->
                        AlbumCard(
                            title = album.title,
                            subtitle = album.artist,
                            imageUrl = album.imageUrl,
                            onClick = { onOpenAlbum(album) },
                        )
                    }
                    if (paging.isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) { LoadingMoreIndicator() }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingMoreIndicator() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = AurelleGold,
            strokeWidth = 3.dp,
            modifier = Modifier.size(28.dp),
        )
    }
}
