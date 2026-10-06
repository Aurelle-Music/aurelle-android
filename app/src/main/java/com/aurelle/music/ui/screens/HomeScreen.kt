package com.aurelle.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.aurelle.music.ui.search.SearchUiState
import com.aurelle.music.ui.search.SectionState

private const val HOME_ITEM_LIMIT = 12
private val CardWidth = 112.dp

/**
 * Home: busca vazia mostra "Recommended Artists/Songs"; com texto, "Artists/Songs" (resultados).
 * Cada seção é um carrossel horizontal; "See all" abre a lista completa.
 */
@Composable
fun HomeScreen(
    state: SearchUiState,
    onOpenSection: (Section) -> Unit,
    onItemClick: (item: MediaItem, queue: List<MediaItem>) -> Unit,
    onRetry: () -> Unit,
    downloads: CardDownloads,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AurelleLogo(
            Modifier
                .padding(horizontal = 48.dp, vertical = 20.dp)
                .staggeredEntrance(0)
        )

        Section.entries.forEachIndexed { index, section ->
            SectionBlock(
                section = section,
                state = state[section],
                onOpen = { onOpenSection(section) },
                onItemClick = onItemClick,
                onRetry = onRetry,
                downloads = downloads,
                modifier = Modifier.staggeredEntrance(index + 1),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SectionBlock(
    section: Section,
    state: SectionState,
    onOpen: () -> Unit,
    onItemClick: (item: MediaItem, queue: List<MediaItem>) -> Unit,
    onRetry: () -> Unit,
    downloads: CardDownloads,
    modifier: Modifier = Modifier,
) {
    val isArtist = section == Section.ARTISTS
    Column(modifier) {
        SectionHeader(
            title = stringResource(section.titleRes(state.recommended)),
            onClick = if (state.items.isNotEmpty()) onOpen else null,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(12.dp))
        when {
            state.items.isNotEmpty() -> LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val shown = state.items.take(HOME_ITEM_LIMIT)
                items(shown, key = { it.id }) { item ->
                    MediaCard(
                        item = item,
                        modifier = Modifier.width(CardWidth),
                        downloads = downloads.of(item),
                        onDownloadClick = { downloads.open(item) },
                        onDownloadCancel = { downloads.cancel(item) },
                        onClick = { onItemClick(item, shown) },
                    )
                }
            }
            state.isLoading -> LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = false,
            ) {
                items(SKELETON_COUNT) { SkeletonCard(isArtist = isArtist, modifier = Modifier.width(CardWidth)) }
            }
            state.hasError -> SectionMessage(R.string.error_loading, onRetry = onRetry)
            else -> SectionMessage(R.string.no_results)
        }
    }
}

private const val SKELETON_COUNT = 5
