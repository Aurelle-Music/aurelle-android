package com.aurelle.music.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurelle.music.AurelleApplication
import com.aurelle.music.download.DownloadManager
import com.aurelle.music.download.DownloadTarget
import com.aurelle.music.download.DownloadedTrack
import com.aurelle.music.settings.SettingsRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

/** Um álbum da Biblioteca: as músicas baixadas que têm o mesmo álbum e o mesmo artista. */
data class LibraryAlbum(
    val title: String,
    val artist: String,
    val imageUrl: String?,
    /** Na ordem do disco (número da faixa; as sem número vão para o fim, por título). */
    val tracks: List<DownloadedTrack>,
    val lastDownloadedAt: Long,
)

/** Um artista da Biblioteca (só o que existe nas músicas baixadas). */
data class LibraryArtist(
    val name: String,
    val imageUrl: String?,
    val albumCount: Int,
    val songCount: Int,
)

/** Resultado da busca da Biblioteca: o que casou, separado por tipo. */
data class LibrarySearchResult(
    val artists: List<LibraryArtist>,
    val albums: List<LibraryAlbum>,
    val tracks: List<DownloadedTrack>,
) {
    val isEmpty: Boolean get() = artists.isEmpty() && albums.isEmpty() && tracks.isEmpty()
}

data class LibraryUiState(
    val source: DownloadTarget = DownloadTarget.DEVICE,
    val query: String = "",
    /** Todas as músicas de [source], da mais recente para a mais antiga. */
    val tracks: List<DownloadedTrack> = emptyList(),
    /** Artistas de [source], em ordem alfabética. */
    val artists: List<LibraryArtist> = emptyList(),
    /** Álbuns de [source], do baixado mais recentemente para o mais antigo. */
    val albums: List<LibraryAlbum> = emptyList(),
    /** Não nulo quando há texto na busca. */
    val search: LibrarySearchResult? = null,
    val deviceCount: Int = 0,
    val appCount: Int = 0,
) {
    fun album(title: String, artist: String): LibraryAlbum? =
        albums.firstOrNull { it.title == title && it.artist == artist }

    fun artist(name: String): LibraryArtist? = artists.firstOrNull { it.name == name }

    fun albumsOf(artist: String): List<LibraryAlbum> =
        albums.filter { it.artist == artist }.sortedBy { it.title.lowercase() }

    fun tracksOf(artist: String): List<DownloadedTrack> =
        tracks.filter { it.artistName() == artist }.sortedBy { it.title.lowercase() }
}

/** Nome do artista como a Biblioteca o mostra (sem nome = "—"). */
fun DownloadedTrack.artistName(): String = artist.ifBlank { LibraryViewModel.UNKNOWN_ARTIST }

/**
 * Estado da Biblioteca. Junta o registro de downloads (Room) com as escolhas da tela (Dispositivo ↔ App e busca)
 * e monta as três visões: músicas, álbuns e artistas. Um só ViewModel (escopo da Activity) serve a Biblioteca
 * e as subtelas (Artistas → Álbuns → Músicas).
 */
class LibraryViewModel(
    private val downloads: DownloadManager,
    settings: SettingsRepository,
) : ViewModel() {

    private data class Controls(
        val source: DownloadTarget = DownloadTarget.DEVICE,
        val query: String = "",
    )

    // A aba com que a Biblioteca abre vem das Configurações (e mudar lá troca a aba na hora).
    private val controls = MutableStateFlow(Controls(source = settings.current.librarySource))

    val state: StateFlow<LibraryUiState> =
        combine(downloads.repository.items, controls) { items, c -> build(items.values, c) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        viewModelScope.launch {
            settings.state.map { it.librarySource }.distinctUntilChanged().collect { selectSource(it) }
        }
    }

    fun selectSource(target: DownloadTarget) = controls.update { it.copy(source = target) }
    fun onQueryChange(query: String) = controls.update { it.copy(query = query) }

    fun remove(track: DownloadedTrack) = downloads.remove(track)

    /** Chamado ao abrir a Biblioteca: tira da lista o que o usuário apagou por fora do app. */
    fun refresh() {
        viewModelScope.launch { downloads.removeMissingFiles() }
    }

    private fun build(all: Collection<DownloadedTrack>, c: Controls): LibraryUiState {
        val tracks = all.filter { it.target == c.source }.sortedByDescending { it.downloadedAt }
        val albums = buildAlbums(tracks)
        val artists = buildArtists(tracks, albums)

        val needle = c.query.normalized()
        val search = if (needle.isEmpty()) null else LibrarySearchResult(
            artists = artists.filter { it.name.normalized().contains(needle) },
            albums = albums.filter {
                it.title.normalized().contains(needle) || it.artist.normalized().contains(needle)
            },
            tracks = tracks.filter {
                it.title.normalized().contains(needle) ||
                    it.artist.normalized().contains(needle) ||
                    it.album.orEmpty().normalized().contains(needle)
            },
        )

        return LibraryUiState(
            source = c.source,
            query = c.query,
            tracks = tracks,
            artists = artists,
            albums = albums,
            search = search,
            deviceCount = all.count { it.target == DownloadTarget.DEVICE },
            appCount = all.count { it.target == DownloadTarget.APP },
        )
    }

    private fun buildAlbums(tracks: List<DownloadedTrack>): List<LibraryAlbum> =
        tracks
            .filter { !it.album.isNullOrBlank() }
            .groupBy { it.album!!.trim() to it.artistName() }
            .map { (key, list) ->
                val ordered = list.sortedWith(
                    compareBy<DownloadedTrack> { it.trackNumber ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() }
                )
                LibraryAlbum(
                    title = key.first,
                    artist = key.second,
                    imageUrl = ordered.firstNotNullOfOrNull { it.imageUrl },
                    tracks = ordered,
                    lastDownloadedAt = list.maxOf { it.downloadedAt },
                )
            }
            .sortedByDescending { it.lastDownloadedAt }

    private fun buildArtists(tracks: List<DownloadedTrack>, albums: List<LibraryAlbum>): List<LibraryArtist> =
        tracks
            .groupBy { it.artistName() }
            .map { (name, list) ->
                LibraryArtist(
                    name = name,
                    // `tracks` já vem da mais recente para a mais antiga: a capa é a da última música baixada.
                    imageUrl = list.firstNotNullOfOrNull { it.imageUrl },
                    albumCount = albums.count { it.artist == name },
                    songCount = list.size,
                )
            }
            .sortedBy { it.name.lowercase() }

    /** Minúsculas e sem acento: "Beyoncé" casa com "beyonce". */
    private fun String.normalized(): String =
        Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    companion object {
        /** Nome usado para músicas sem artista no registro. */
        const val UNKNOWN_ARTIST = "—"

        val Factory = viewModelFactory {
            initializer { LibraryViewModel(AurelleApplication.downloads, AurelleApplication.settings) }
        }
    }
}
