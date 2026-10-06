package com.aurelle.music.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurelle.music.AurelleApplication
import com.aurelle.music.data.MusicRepository
import com.aurelle.music.data.NextPage
import com.aurelle.music.data.RecommendedSeeds
import com.aurelle.music.data.Section
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da busca compartilhado entre a Home e a tela "ver tudo".
 * - Texto digitado -> debounce de 400 ms -> busca (a anterior é cancelada).
 * - Texto vazio -> recomendações ([RecommendedSeeds]).
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: MusicRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val requests = MutableSharedFlow<String>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val nextPages = mutableMapOf<Section, NextPage?>()
    private val loadMoreJobs = mutableMapOf<Section, Job>()

    init {
        viewModelScope.launch {
            requests.collectLatest { runSearch(it) }
        }
        viewModelScope.launch {
            _query
                .map { it.trim() }
                .distinctUntilChanged()
                .debounce { if (it.isEmpty()) 0L else DEBOUNCE_MS }
                .collect { requests.emit(it) }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** Refaz a busca atual (botão "Try again"). */
    fun retry() {
        requests.tryEmit(_query.value.trim())
    }

    /** Carrega a próxima página da [section] (rolagem infinita na tela "ver tudo"). */
    fun loadMore(section: Section) {
        val current = _state.value[section]
        val next = nextPages[section] ?: return
        if (current.isLoading || current.isLoadingMore) return

        update(section) { it.copy(isLoadingMore = true) }
        loadMoreJobs[section] = viewModelScope.launch {
            repository.nextPage(next).fold(
                onSuccess = { page ->
                    nextPages[section] = page.next
                    update(section) { cur ->
                        cur.copy(
                            items = (cur.items + page.items).distinctBy { it.id },
                            isLoadingMore = false,
                            canLoadMore = page.next != null,
                        )
                    }
                },
                onFailure = {
                    // Para de tentar nessa busca para não ficar em loop de erro.
                    update(section) { it.copy(isLoadingMore = false, canLoadMore = false) }
                },
            )
        }
    }

    private suspend fun runSearch(query: String) {
        loadMoreJobs.values.forEach { it.cancel() }
        loadMoreJobs.clear()
        nextPages.clear()

        Section.entries.forEach { section ->
            update(section) { it.copy(isLoading = true, isLoadingMore = false, hasError = false) }
        }
        // Cada seção carrega por conta própria: se uma falhar, a outra continua.
        coroutineScope {
            Section.entries.forEach { section ->
                launch { loadFirstPage(section, query) }
            }
        }
    }

    private suspend fun loadFirstPage(section: Section, query: String) {
        val recommended = query.isEmpty()
        val effectiveQuery = if (recommended) RecommendedSeeds.queryFor(section) else query
        repository.search(effectiveQuery, section).fold(
            onSuccess = { page ->
                nextPages[section] = page.next
                update(section) {
                    SectionState(
                        items = page.items,
                        isLoading = false,
                        recommended = recommended,
                        canLoadMore = page.next != null,
                    )
                }
            },
            onFailure = {
                update(section) {
                    SectionState(
                        items = emptyList(),
                        isLoading = false,
                        hasError = true,
                        recommended = recommended,
                    )
                }
            },
        )
    }

    private fun update(section: Section, transform: (SectionState) -> SectionState) {
        _state.update { it.with(section, transform) }
    }

    companion object {
        private const val DEBOUNCE_MS = 400L

        val Factory = viewModelFactory {
            initializer { SearchViewModel(AurelleApplication.repository) }
        }
    }
}
