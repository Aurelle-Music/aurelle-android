package com.aurelle.music.data

/** Token opaco para pedir a próxima página de uma busca. */
interface NextPage

data class SearchPage(
    val items: List<MediaItem>,
    /** `null` quando não há mais páginas. */
    val next: NextPage?,
)

/** Uma página de uma lista paginada (músicas/álbuns de um artista). [next] é `null` na última página. */
data class ListPage<T>(
    val items: List<T>,
    val next: NextPage?,
)

interface MusicRepository {
    /** Primeira página de resultados da [section] para [query]. */
    suspend fun search(query: String, section: Section): Result<SearchPage>

    /** Próxima página, a partir do token devolvido por [search] ou por outra chamada de [nextPage]. */
    suspend fun nextPage(next: NextPage): Result<SearchPage>

    /**
     * Músicas do artista [artistName] (busca no YT Music filtrada pelo nome do artista).
     * Sem [next], devolve a primeira página; com o token de uma página anterior, devolve a seguinte.
     */
    suspend fun artistSongs(artistName: String, next: NextPage? = null): Result<ListPage<Song>>

    /** Álbuns do artista [artistName]; paginação igual à de [artistSongs]. */
    suspend fun artistAlbums(artistName: String, next: NextPage? = null): Result<ListPage<Album>>

    /** Faixas de um [album], na ordem do disco; cada música já vem com álbum e número da faixa. */
    suspend fun album(album: Album): Result<AlbumDetail>
}
