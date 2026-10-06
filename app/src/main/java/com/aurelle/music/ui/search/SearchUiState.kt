package com.aurelle.music.ui.search

import com.aurelle.music.data.MediaItem
import com.aurelle.music.data.Section

data class SectionState(
    val items: List<MediaItem> = emptyList(),
    /** Carregando a primeira página. */
    val isLoading: Boolean = true,
    /** Carregando páginas seguintes (rolagem infinita). */
    val isLoadingMore: Boolean = false,
    val hasError: Boolean = false,
    /** `true` quando os itens são recomendações (busca vazia) e não resultados de busca. */
    val recommended: Boolean = true,
    val canLoadMore: Boolean = false,
)

data class SearchUiState(
    val artists: SectionState = SectionState(),
    val songs: SectionState = SectionState(),
) {
    operator fun get(section: Section): SectionState = when (section) {
        Section.ARTISTS -> artists
        Section.SONGS -> songs
    }

    fun with(section: Section, transform: (SectionState) -> SectionState): SearchUiState =
        when (section) {
            Section.ARTISTS -> copy(artists = transform(artists))
            Section.SONGS -> copy(songs = transform(songs))
        }
}
