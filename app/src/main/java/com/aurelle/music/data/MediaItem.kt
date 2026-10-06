package com.aurelle.music.data

/** Seções da Home. */
enum class Section { ARTISTS, SONGS }

/** Item exibido nos carrosséis e grades: artista ou música. */
sealed interface MediaItem {
    /** Identificador único (URL do YouTube). */
    val id: String
    val title: String
    val imageUrl: String?
    val section: Section
}

data class Artist(
    override val id: String,
    override val title: String,
    override val imageUrl: String?,
    val subscriberCount: Long?,
) : MediaItem {
    override val section: Section get() = Section.ARTISTS
}

data class Song(
    override val id: String,
    override val title: String,
    override val imageUrl: String?,
    val artist: String,
    val durationSeconds: Long?,
    /** Álbum da música, quando se sabe (músicas que vêm de uma página de álbum). */
    val album: String? = null,
    /** Posição da faixa no álbum (1, 2, 3...), quando vem de uma página de álbum. */
    val trackNumber: Int? = null,
) : MediaItem {
    override val section: Section get() = Section.SONGS
}

/** Álbum do YouTube Music. Não é um [MediaItem] da Home: abre a própria página (lista de faixas). */
data class Album(
    /** URL da playlist do álbum (é o que o NewPipeExtractor abre). */
    val id: String,
    val title: String,
    val imageUrl: String?,
    val artist: String,
    /** URL do canal do artista, quando o YouTube informa (leva à página do artista). */
    val artistUrl: String?,
)

/** Álbum com as faixas, na ordem do disco. */
data class AlbumDetail(val album: Album, val tracks: List<Song>)

/** Estado de uma carga de rede para a UI. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data object Failed : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
}
