package com.aurelle.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurelle.music.data.Song
import com.aurelle.music.download.SongDownloads
import com.aurelle.music.ui.download.DownloadButton
import com.aurelle.music.ui.player.formatTime
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleHint
import com.aurelle.music.ui.theme.AurelleOnBackground
import com.aurelle.music.ui.theme.AurelleSurface

/**
 * Linha de uma música online (páginas de artista e de álbum): capa ou número da faixa, título, subtítulo,
 * duração e botão de baixar. [number] != null troca a capa pelo número (estilo página de álbum).
 */
@Composable
fun SongRow(
    song: Song,
    subtitle: String,
    playing: Boolean,
    downloads: SongDownloads,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDownloadCancel: () -> Unit,
    modifier: Modifier = Modifier,
    number: Int? = null,
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
                imageUrl = song.imageUrl,
                shape = RoundedCornerShape(12.dp),
                icon = Icons.Filled.MusicNote,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (playing) AurelleGold else AurelleOnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val extra = song.durationSeconds?.let { formatTime(it * 1000L) }
            val line = listOfNotNull(subtitle.takeIf { it.isNotBlank() }, extra).joinToString(" · ")
            if (line.isNotEmpty()) {
                Text(
                    text = line,
                    style = MaterialTheme.typography.labelMedium,
                    color = AurelleGoldLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        DownloadButton(
            downloads = downloads,
            onClick = onDownloadClick,
            onCancel = onDownloadCancel,
            size = 40.dp,
        )
    }
}

/** Esqueleto de uma linha de música (com brilho) enquanto a lista carrega. */
@Composable
fun SongRowSkeleton(modifier: Modifier = Modifier, withCover: Boolean = true) {
    val bar = RoundedCornerShape(4.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (withCover) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AurelleSurface)
                    .shimmer()
            )
            Spacer(Modifier.width(14.dp))
        } else {
            Spacer(Modifier.width(36.dp))
        }
        Column(Modifier.weight(1f)) {
            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(bar)
                    .background(AurelleSurface)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.35f)
                    .height(9.dp)
                    .clip(bar)
                    .background(AurelleSurface)
            )
        }
    }
}
