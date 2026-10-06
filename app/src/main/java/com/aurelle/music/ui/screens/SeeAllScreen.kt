package com.aurelle.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aurelle.music.R
import com.aurelle.music.data.MediaItem
import com.aurelle.music.data.Section
import com.aurelle.music.ui.components.AurelleLogo
import com.aurelle.music.ui.components.CardDownloads
import com.aurelle.music.ui.components.MediaCard
import com.aurelle.music.ui.components.SectionHeader
import com.aurelle.music.ui.components.SectionMessage
import com.aurelle.music.ui.components.SkeletonCard
import com.aurelle.music.ui.components.staggeredEntrance
import com.aurelle.music.ui.search.SectionState
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleSurface

private const val LOAD_MORE_THRESHOLD = 6

/** Todos os artistas/músicas de uma seção, em grade de 3 colunas, com rolagem infinita. */
@Composable
fun SeeAllScreen(
    section: Section,
    state: SectionState,
    onBack: () -> Unit,
    onItemClick: (item: MediaItem, queue: List<MediaItem>) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    downloads: CardDownloads,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(nearEnd, state.items.size, state.canLoadMore) {
        if (nearEnd && state.canLoadMore) onLoadMore()
    }

    Column(modifier = modifier.fillMaxSize()) {
        AurelleLogo(
            Modifier
                .padding(horizontal = 96.dp, vertical = 14.dp)
                .staggeredEntrance(0)
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .staggeredEntrance(1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AurelleSurface)
                    .border(1.dp, AurelleGold.copy(alpha = 0.25f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = AurelleGoldLight,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            SectionHeader(
                title = stringResource(section.titleRes(state.recommended)),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))

        when {
            state.items.isNotEmpty() -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier.staggeredEntrance(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    MediaCard(
                        item = item,
                        downloads = downloads.of(item),
                        onDownloadClick = { downloads.open(item) },
                        onDownloadCancel = { downloads.cancel(item) },
                        onClick = { onItemClick(item, state.items) },
                    )
                }
                if (state.isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
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
                }
            }
            state.isLoading -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            ) {
                items(9) { SkeletonCard(isArtist = section == Section.ARTISTS) }
            }
            state.hasError -> SectionMessage(R.string.error_loading, onRetry = onRetry)
            else -> SectionMessage(R.string.no_results)
        }
    }
}
