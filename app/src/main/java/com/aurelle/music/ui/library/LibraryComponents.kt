package com.aurelle.music.ui.library

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Album as AlbumIcon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aurelle.music.AurelleApplication
import com.aurelle.music.R
import com.aurelle.music.data.formatSize
import com.aurelle.music.download.DownloadTarget
import com.aurelle.music.download.DownloadedTrack
import com.aurelle.music.ui.components.CoverImage
import com.aurelle.music.ui.theme.AurelleBackgroundBottom
import com.aurelle.music.ui.theme.AurelleBackgroundTop
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleHint
import com.aurelle.music.ui.theme.AurelleOnBackground
import com.aurelle.music.ui.theme.AurelleSurface

/** O que as telas da Biblioteca podem pedir ao app (tocar, remover, navegar). */
@Immutable
class LibraryActions(
    val playingId: String?,
    val onPlay: (track: DownloadedTrack, queue: List<DownloadedTrack>) -> Unit,
    val onShuffle: (queue: List<DownloadedTrack>) -> Unit,
    val onRemove: (DownloadedTrack) -> Unit,
    val onOpenArtist: (name: String) -> Unit,
    val onOpenAlbum: (LibraryAlbum) -> Unit,
)

enum class LibrarySort { RECENT, TITLE, ARTIST }

@StringRes
fun LibrarySort.label(): Int = when (this) {
    LibrarySort.RECENT -> R.string.library_sort_recent
    LibrarySort.TITLE -> R.string.library_sort_title
    LibrarySort.ARTIST -> R.string.library_sort_artist
}

/** Ordenação com que as listas abrem (Conta ▸ Biblioteca); depois cada tela guarda a que o usuário escolher. */
@Composable
fun rememberDefaultLibrarySort(): LibrarySort = remember { AurelleApplication.settings.current.librarySort }

/** Linha com chips de ordenação. */
@Composable
fun SortChips(selected: LibrarySort, onSelect: (LibrarySort) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(LibrarySort.entries) { sort ->
            Chip(text = stringResource(sort.label()), selected = selected == sort, onClick = { onSelect(sort) })
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(if (selected) AurelleGold.copy(alpha = 0.18f) else AurelleSurface)
            .border(1.dp, if (selected) AurelleGold else AurelleGold.copy(alpha = 0.12f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) AurelleGoldLight else AurelleHint,
            maxLines = 1,
        )
    }
}

/** Alternância Dispositivo ↔ App (de onde ver as músicas). */
@Composable
fun SourceToggle(
    source: DownloadTarget,
    deviceCount: Int,
    appCount: Int,
    onSelect: (DownloadTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(AurelleSurface)
            .border(1.dp, AurelleGold.copy(alpha = 0.18f), shape)
            .padding(4.dp),
    ) {
        SourceOption(
            text = "${stringResource(R.string.download_target_device)} · $deviceCount",
            selected = source == DownloadTarget.DEVICE,
            onClick = { onSelect(DownloadTarget.DEVICE) },
            modifier = Modifier.weight(1f),
        )
        SourceOption(
            text = "${stringResource(R.string.download_target_app)} · $appCount",
            selected = source == DownloadTarget.APP,
            onClick = { onSelect(DownloadTarget.APP) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SourceOption(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(if (selected) AurelleGold else AurelleSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) AurelleBackgroundBottom else AurelleHint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Linha de categoria da Biblioteca (Artistas / Álbuns / Músicas): ícone, nome, quantidade e seta. */
@Composable
fun CategoryRow(
    icon: ImageVector,
    @StringRes label: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AurelleGold.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AurelleGold, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.titleMedium,
            color = AurelleOnBackground,
            modifier = Modifier.weight(1f),
        )
        Text(text = count.toString(), style = MaterialTheme.typography.labelLarge, color = AurelleHint)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AurelleHint,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Linha de artista: capa circular, nome e "N álbuns · M músicas". */
@Composable
fun LibraryArtistRow(artist: LibraryArtist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            imageUrl = artist.imageUrl,
            shape = CircleShape,
            icon = Icons.Filled.Person,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = AurelleOnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artistSummary(artist.albumCount, artist.songCount),
                style = MaterialTheme.typography.labelMedium,
                color = AurelleHint,
                maxLines = 1,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AurelleHint,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Linha de álbum (usada nos resultados da busca): capa, título e artista. */
@Composable
fun LibraryAlbumRow(album: LibraryAlbum, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            imageUrl = album.imageUrl,
            shape = RoundedCornerShape(12.dp),
            icon = Icons.Filled.AlbumIcon,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyLarge,
                color = AurelleOnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${album.artist} · " +
                    resources.getQuantityString(R.plurals.songs_count, album.tracks.size, album.tracks.size),
                style = MaterialTheme.typography.labelMedium,
                color = AurelleHint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AurelleHint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun artistSummary(albumCount: Int, songCount: Int): String {
    val resources = LocalContext.current.resources
    val songs = resources.getQuantityString(R.plurals.songs_count, songCount, songCount)
    return if (albumCount > 0) {
        resources.getQuantityString(R.plurals.albums_count, albumCount, albumCount) + " · " + songs
    } else {
        songs
    }
}

/**
 * Linha de uma música baixada. Com [number] mostra o número da faixa no lugar da capa (páginas de álbum);
 * [showAlbum] acrescenta o álbum ao subtítulo (lista geral de músicas).
 */
@Composable
fun LibraryTrackRow(
    track: DownloadedTrack,
    playing: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    number: Int? = null,
    showAlbum: Boolean = false,
    showArtist: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (playing) AurelleGold else AurelleHint,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(36.dp),
            )
        } else {
            CoverImage(
                imageUrl = track.imageUrl,
                shape = RoundedCornerShape(12.dp),
                icon = Icons.Filled.MusicNote,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (playing) AurelleGold else AurelleOnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(
                track.artistName().takeIf { showArtist },
                track.album?.takeIf { showAlbum && it.isNotBlank() },
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = AurelleGoldLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = buildList {
                    add(track.format.uppercase())
                    if (track.bitrateKbps > 0) add("${track.bitrateKbps} kbps")
                    add(formatSize(track.sizeBytes))
                }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = AurelleHint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = stringResource(R.string.library_remove),
                tint = AurelleHint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Confirmação de "remover download" (apaga o arquivo e o registro). */
@Composable
fun RemoveTrackDialog(track: DownloadedTrack, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AurelleBackgroundTop,
        titleContentColor = AurelleOnBackground,
        textContentColor = AurelleHint,
        title = { Text(stringResource(R.string.library_remove_title)) },
        text = {
            Text(
                stringResource(
                    if (track.target == DownloadTarget.DEVICE) R.string.library_remove_message_device
                    else R.string.library_remove_message_app,
                    track.title,
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.library_remove_confirm), color = AurelleGold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.download_cancel), color = AurelleGoldLight)
            }
        },
    )
}

fun List<DownloadedTrack>.sortedFor(sort: LibrarySort): List<DownloadedTrack> = when (sort) {
    LibrarySort.RECENT -> sortedByDescending { it.downloadedAt }
    LibrarySort.TITLE -> sortedBy { it.title.lowercase() }
    LibrarySort.ARTIST -> sortedWith(compareBy<DownloadedTrack> { it.artist.lowercase() }.thenBy { it.title.lowercase() })
}
