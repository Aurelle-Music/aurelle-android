package com.aurelle.music.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurelle.music.AurelleApplication
import com.aurelle.music.data.Album
import com.aurelle.music.data.AlbumDetail
import com.aurelle.music.data.Load
import com.aurelle.music.data.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Página de um álbum online: carrega as faixas (o cabeçalho já vem da rota). */
class AlbumViewModel(
    val album: Album,
    private val repository: MusicRepository,
) : ViewModel() {

    private val _detail = MutableStateFlow<Load<AlbumDetail>>(Load.Loading)
    val detail: StateFlow<Load<AlbumDetail>> = _detail

    init {
        load()
    }

    fun retry() {
        if (_detail.value is Load.Failed) load()
    }

    private fun load() {
        _detail.value = Load.Loading
        viewModelScope.launch {
            _detail.value = repository.album(album).fold({ Load.Ready(it) }, { Load.Failed })
        }
    }

    companion object {
        fun factory(album: Album): ViewModelProvider.Factory = viewModelFactory {
            initializer { AlbumViewModel(album, AurelleApplication.repository) }
        }
    }
}
