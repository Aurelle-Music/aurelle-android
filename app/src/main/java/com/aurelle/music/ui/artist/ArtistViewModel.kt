package com.aurelle.music.ui.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurelle.music.AurelleApplication
import com.aurelle.music.data.Album
import com.aurelle.music.data.Load
import com.aurelle.music.data.MusicRepository
import com.aurelle.music.data.NextPage
import com.aurelle.music.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** O que a rota leva para a página do artista (já aparece na tela antes de a rede responder). */
data class ArtistRef(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val subscribers: Long?,
)

/** Estado da paginação de uma lista: há mais páginas? Está buscando uma agora? */
data class Paging(
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false,
)

/**
 * Página do artista: músicas e álbuns carregam em paralelo e falham separado.
 * As telas "ver tudo" (todas as músicas / todos os álbuns) usam este mesmo ViewModel, então
 * continuam de onde a página do artista parou e a rolagem infinita pede só as páginas seguintes.
 */
class ArtistViewModel(
    val ref: ArtistRef,
    private val repository: MusicRepository,
) : ViewModel() {

    private val _songs = MutableStateFlow<Load<List<Song>>>(Load.Loading)
    val songs: StateFlow<Load<List<Song>>> = _songs

    private val _albums = MutableStateFlow<Load<List<Album>>>(Load.Loading)
    val albums: StateFlow<Load<List<Album>>> = _albums

    private val _songsPaging = MutableStateFlow(Paging())
    val songsPaging: StateFlow<Paging> = _songsPaging

    private val _albumsPaging = MutableStateFlow(Paging())
    val albumsPaging: StateFlow<Paging> = _albumsPaging

    private var songsNext: NextPage? = null
    private var albumsNext: NextPage? = null

    init {
        loadSongs()
        loadAlbums()
    }

    fun retry() {
        if (_songs.value is Load.Failed) loadSongs()
        if (_albums.value is Load.Failed) loadAlbums()
    }

    private fun loadSongs() {
        _songs.value = Load.Loading
        _songsPaging.value = Paging()
        viewModelScope.launch {
            repository.artistSongs(ref.name).fold(
                onSuccess = { page ->
                    songsNext = page.next
                    _songs.value = Load.Ready(page.items)
                    _songsPaging.value = Paging(canLoadMore = page.next != null)
                },
                onFailure = { _songs.value = Load.Failed },
            )
        }
    }

    private fun loadAlbums() {
        _albums.value = Load.Loading
        _albumsPaging.value = Paging()
        viewModelScope.launch {
            repository.artistAlbums(ref.name).fold(
                onSuccess = { page ->
                    albumsNext = page.next
                    _albums.value = Load.Ready(page.items)
                    _albumsPaging.value = Paging(canLoadMore = page.next != null)
                },
                onFailure = { _albums.value = Load.Failed },
            )
        }
    }

    fun loadMoreSongs() {
        val next = songsNext ?: return
        val current = (_songs.value as? Load.Ready)?.value ?: return
        if (_songsPaging.value.isLoadingMore) return
        _songsPaging.value = Paging(canLoadMore = true, isLoadingMore = true)
        viewModelScope.launch {
            repository.artistSongs(ref.name, next).fold(
                onSuccess = { page ->
                    songsNext = page.next
                    _songs.value = Load.Ready((current + page.items).distinctBy { it.id })
                    _songsPaging.value = Paging(canLoadMore = page.next != null)
                },
                // Mantém o token: rolar de novo tenta outra vez.
                onFailure = { _songsPaging.value = Paging(canLoadMore = true) },
            )
        }
    }

    fun loadMoreAlbums() {
        val next = albumsNext ?: return
        val current = (_albums.value as? Load.Ready)?.value ?: return
        if (_albumsPaging.value.isLoadingMore) return
        _albumsPaging.value = Paging(canLoadMore = true, isLoadingMore = true)
        viewModelScope.launch {
            repository.artistAlbums(ref.name, next).fold(
                onSuccess = { page ->
                    albumsNext = page.next
                    _albums.value = Load.Ready((current + page.items).distinctBy { it.id })
                    _albumsPaging.value = Paging(canLoadMore = page.next != null)
                },
                onFailure = { _albumsPaging.value = Paging(canLoadMore = true) },
            )
        }
    }

    companion object {
        fun factory(ref: ArtistRef): ViewModelProvider.Factory = viewModelFactory {
            initializer { ArtistViewModel(ref, AurelleApplication.repository) }
        }
    }
}
