package com.aurelle.music.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import android.net.Uri
import androidx.annotation.StringRes
import com.aurelle.music.R
import com.aurelle.music.data.Album
import com.aurelle.music.data.Artist
import com.aurelle.music.data.Section

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val ACCOUNT = "account"

    const val SECTION_ARG = "section"
    const val SEE_ALL_PATTERN = "see_all/{$SECTION_ARG}"
    const val SEE_ALL_PREFIX = "see_all/"

    fun seeAll(section: Section) = "$SEE_ALL_PREFIX${section.name}"

    // --- Páginas online (a partir da Home): artista e álbum --------------------------------------
    // Os dados do item vão na rota (codificados), então a tela abre na hora, sem esperar a rede.
    const val ARG_ID = "id"
    const val ARG_NAME = "name"
    const val ARG_IMAGE = "image"
    const val ARG_SUBS = "subs"
    const val ARG_ARTIST = "artist"
    const val ARG_ARTIST_URL = "artistUrl"

    const val ARTIST_PREFIX = "artist"
    const val ARTIST_PATTERN =
        "$ARTIST_PREFIX?$ARG_ID={$ARG_ID}&$ARG_NAME={$ARG_NAME}&$ARG_IMAGE={$ARG_IMAGE}&$ARG_SUBS={$ARG_SUBS}"

    // Listas completas do artista. Sem argumentos: usam o ArtistViewModel da página do artista de onde vieram.
    // Os nomes começam com "artist", então a Home continua sendo a aba selecionada.
    const val ARTIST_SONGS = "artist_songs"
    const val ARTIST_ALBUMS = "artist_albums"

    const val ALBUM_PREFIX = "album"
    const val ALBUM_PATTERN =
        "$ALBUM_PREFIX?$ARG_ID={$ARG_ID}&$ARG_NAME={$ARG_NAME}&$ARG_IMAGE={$ARG_IMAGE}" +
            "&$ARG_ARTIST={$ARG_ARTIST}&$ARG_ARTIST_URL={$ARG_ARTIST_URL}"

    fun artist(artist: Artist) =
        "$ARTIST_PREFIX?$ARG_ID=${enc(artist.id)}&$ARG_NAME=${enc(artist.title)}" +
            "&$ARG_IMAGE=${enc(artist.imageUrl)}&$ARG_SUBS=${artist.subscriberCount ?: -1L}"

    fun album(album: Album) =
        "$ALBUM_PREFIX?$ARG_ID=${enc(album.id)}&$ARG_NAME=${enc(album.title)}&$ARG_IMAGE=${enc(album.imageUrl)}" +
            "&$ARG_ARTIST=${enc(album.artist)}&$ARG_ARTIST_URL=${enc(album.artistUrl)}"

    // --- Biblioteca (músicas baixadas): Artistas -> Álbuns -> Músicas ---------------------------
    const val LIBRARY_PREFIX = "library"
    const val LIBRARY_ARTISTS = "library/artists"
    const val LIBRARY_ALBUMS = "library/albums"
    const val LIBRARY_SONGS = "library/songs"

    const val LIBRARY_ARTIST_PREFIX = "library/artist"
    const val LIBRARY_ARTIST_PATTERN = "$LIBRARY_ARTIST_PREFIX?$ARG_NAME={$ARG_NAME}"

    const val LIBRARY_ALBUM_PREFIX = "library/album"
    const val LIBRARY_ALBUM_PATTERN = "$LIBRARY_ALBUM_PREFIX?$ARG_NAME={$ARG_NAME}&$ARG_ARTIST={$ARG_ARTIST}"

    fun libraryArtist(name: String) = "$LIBRARY_ARTIST_PREFIX?$ARG_NAME=${enc(name)}"

    fun libraryAlbum(title: String, artist: String) =
        "$LIBRARY_ALBUM_PREFIX?$ARG_NAME=${enc(title)}&$ARG_ARTIST=${enc(artist)}"

    /** Marcador para valor vazio: um parâmetro de rota vazio não é confiável, então nunca mandamos "". */
    private const val NONE = "~"

    private fun enc(value: String?): String = Uri.encode(value?.takeIf { it.isNotEmpty() } ?: NONE)

    /** Lê um argumento de rota escrito por [enc]; vazio/ausente vira `null`. */
    fun arg(value: String?): String? = value?.takeIf { it.isNotEmpty() && it != NONE }
}

/** Abas da barra inferior. */
enum class TopLevelDestination(
    val route: String,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    LIBRARY(Routes.LIBRARY, R.string.nav_library, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    ACCOUNT(Routes.ACCOUNT, R.string.nav_account, Icons.Filled.Person, Icons.Outlined.Person),
}
