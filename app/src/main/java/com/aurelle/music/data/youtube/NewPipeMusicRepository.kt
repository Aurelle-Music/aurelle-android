package com.aurelle.music.data.youtube

import com.aurelle.music.data.Album
import com.aurelle.music.data.AlbumDetail
import com.aurelle.music.data.ListPage
import com.aurelle.music.data.Artist
import com.aurelle.music.data.MediaItem
import com.aurelle.music.data.MusicRepository
import com.aurelle.music.data.NextPage
import com.aurelle.music.data.SearchPage
import com.aurelle.music.data.Section
import com.aurelle.music.data.Song
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.text.Normalizer
import kotlin.coroutines.cancellation.CancellationException

/** Busca no YouTube Music (filtros "músicas" e "artistas") via NewPipeExtractor. */
class NewPipeMusicRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MusicRepository {

    private class Token(
        val extractor: SearchExtractor,
        val page: Page,
        val section: Section,
    ) : NextPage

    override suspend fun search(query: String, section: Section): Result<SearchPage> = io {
        val filter = when (section) {
            Section.ARTISTS -> YoutubeSearchQueryHandlerFactory.MUSIC_ARTISTS
            Section.SONGS -> YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
        }
        val extractor = ServiceList.YouTube.getSearchExtractor(query, listOf(filter), "")
        extractor.fetchPage()
        toSearchPage(extractor, extractor.initialPage, section)
    }

    override suspend fun nextPage(next: NextPage): Result<SearchPage> = io {
        val token = next as Token
        toSearchPage(token.extractor, token.extractor.getPage(token.page), token.section)
    }

    private fun toSearchPage(
        extractor: SearchExtractor,
        page: ListExtractor.InfoItemsPage<InfoItem>,
        section: Section,
    ): SearchPage {
        val items = page.items
            .mapNotNull { it.toMediaItem(section) }
            .distinctBy { it.id }
        val next = page.nextPage?.let { Token(extractor, it, section) }
        return SearchPage(items, next)
    }

    // --- Páginas de artista e de álbum ---------------------------------------------------------

    private class ArtistToken(
        val extractor: SearchExtractor,
        val page: Page,
    ) : NextPage

    override suspend fun artistSongs(artistName: String, next: NextPage?): Result<ListPage<Song>> = io {
        artistPage<Song>(
            artistName = artistName,
            filter = YoutubeSearchQueryHandlerFactory.MUSIC_SONGS,
            next = next,
            key = { it.id },
        ) { item ->
            (item as? StreamInfoItem)
                ?.let { it.toMediaItem(Section.SONGS) as? Song }
                ?.takeIf { it.artist.matchesArtist(artistName) }
        }
    }

    override suspend fun artistAlbums(artistName: String, next: NextPage?): Result<ListPage<Album>> = io {
        artistPage<Album>(
            artistName = artistName,
            filter = YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS,
            next = next,
            key = { it.id },
        ) { item ->
            (item as? PlaylistInfoItem)
                ?.let {
                    Album(
                        id = it.url,
                        title = it.name,
                        imageUrl = pickThumbnail(it.thumbnails),
                        artist = it.uploaderName.orEmpty(),
                        artistUrl = it.uploaderUrl,
                    )
                }
                ?.takeIf { it.artist.matchesArtist(artistName) }
        }
    }

    /**
     * Lê páginas da busca do YT Music e guarda só o que [map] aceita (o filtro por nome do artista
     * descarta muita coisa). Junta até [MAX_PAGES_PER_CALL] páginas por chamada, parando assim que
     * houver [MIN_ITEMS_PER_CALL] itens; o token devolvido continua de onde parou.
     */
    private fun <T> artistPage(
        artistName: String,
        filter: String,
        next: NextPage?,
        key: (T) -> String,
        map: (InfoItem) -> T?,
    ): ListPage<T> {
        val extractor: SearchExtractor
        var page: ListExtractor.InfoItemsPage<InfoItem>
        if (next == null) {
            extractor = ServiceList.YouTube.getSearchExtractor(artistName, listOf(filter), "")
            extractor.fetchPage()
            page = extractor.initialPage
        } else {
            val token = next as ArtistToken
            extractor = token.extractor
            page = extractor.getPage(token.page)
        }

        val found = LinkedHashMap<String, T>()
        var pages = 1
        while (true) {
            page.items.forEach { item -> map(item)?.let { found.putIfAbsent(key(it), it) } }
            val following = page.nextPage
            if (following == null || found.size >= MIN_ITEMS_PER_CALL || pages >= MAX_PAGES_PER_CALL) {
                return ListPage(found.values.toList(), following?.let { ArtistToken(extractor, it) })
            }
            page = extractor.getPage(following)
            pages++
        }
    }

    override suspend fun album(album: Album): Result<AlbumDetail> = io {
        // Álbuns do YT Music são playlists (list=OLAK5uy_...). Usamos a URL "normal" do YouTube.
        val listId = Regex("[?&]list=([^&]+)").find(album.id)?.groupValues?.get(1)
        val url = if (listId != null) "https://www.youtube.com/playlist?list=$listId" else album.id

        val service = ServiceList.YouTube
        val info = PlaylistInfo.getInfo(service, url)
        val streams = ArrayList<StreamInfoItem>(info.relatedItems)
        var next = info.nextPage
        var pages = 1
        while (next != null && pages < MAX_ALBUM_PAGES) {
            val more = PlaylistInfo.getMoreItems(service, url, next)
            streams += more.items
            next = more.nextPage
            pages++
        }

        val cover = album.imageUrl ?: pickThumbnail(info.thumbnails)
        val artist = album.artist.ifBlank { info.uploaderName.orEmpty().removeSuffix(TOPIC_SUFFIX) }
        val tracks = streams.mapIndexed { index, stream ->
            Song(
                id = stream.url,
                title = stream.name,
                // Todas as faixas usam a capa do álbum e o artista do álbum (a miniatura do vídeo é outra coisa).
                imageUrl = cover,
                artist = artist.ifBlank { stream.uploaderName.orEmpty().removeSuffix(TOPIC_SUFFIX) },
                durationSeconds = stream.duration.takeIf { it >= 0 },
                album = album.title,
                trackNumber = index + 1,
            )
        }
        AlbumDetail(album.copy(imageUrl = cover, artist = artist), tracks)
    }

    /** "Beyoncé" casa com "beyonce" e com "Beyoncé, Jay-Z" (e vice-versa). */
    private fun String.matchesArtist(artistName: String): Boolean {
        val a = normalizedName()
        val b = artistName.normalizedName()
        return a.isNotEmpty() && b.isNotEmpty() && (a.contains(b) || b.contains(a))
    }

    private fun String.normalizedName(): String =
        Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    private fun InfoItem.toMediaItem(section: Section): MediaItem? = when {
        section == Section.ARTISTS && this is ChannelInfoItem -> Artist(
            id = url,
            title = name,
            imageUrl = pickThumbnail(thumbnails),
            subscriberCount = subscriberCount.takeIf { it >= 0 },
        )
        section == Section.SONGS && this is StreamInfoItem -> Song(
            id = url,
            title = name,
            imageUrl = pickThumbnail(thumbnails),
            artist = uploaderName.orEmpty(),
            durationSeconds = duration.takeIf { it >= 0 },
        )
        else -> null
    }

    /** Menor imagem com pelo menos [minSize] px de altura; senão, a maior disponível. */
    private fun pickThumbnail(images: List<Image>, minSize: Int = 240): String? {
        if (images.isEmpty()) return null
        val sorted = images.sortedBy { it.height }
        return (sorted.firstOrNull { it.height >= minSize } ?: sorted.last()).url
    }

    private suspend fun <T> io(block: () -> T): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        const val MIN_ITEMS_PER_CALL = 10
        const val MAX_PAGES_PER_CALL = 4
        const val MAX_ALBUM_PAGES = 5
        const val TOPIC_SUFFIX = " - Topic"
    }
}
