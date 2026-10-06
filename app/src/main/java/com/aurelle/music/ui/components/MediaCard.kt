package com.aurelle.music.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aurelle.music.R
import com.aurelle.music.data.Artist
import com.aurelle.music.data.MediaItem
import com.aurelle.music.data.Section
import com.aurelle.music.data.Song
import com.aurelle.music.data.compactCount
import com.aurelle.music.download.SongDownloads
import com.aurelle.music.ui.download.DownloadButton
import com.aurelle.music.ui.theme.AurelleBackgroundBottom
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleHint
import com.aurelle.music.ui.theme.AurelleOnBackground
import com.aurelle.music.ui.theme.AurelleSurface
import com.aurelle.music.ui.theme.AurelleSurfaceHigh

private fun coverShape(isArtist: Boolean): Shape =
    if (isArtist) CircleShape else RoundedCornerShape(14.dp)

/** Card com capa + título + subtítulo. Artistas têm capa circular; músicas, quadrada arredondada. */
@Composable
fun MediaCard(
    item: MediaItem,
    modifier: Modifier = Modifier,
    /** Estado de download da música (só músicas mostram o botão de baixar no canto da capa). */
    downloads: SongDownloads? = null,
    onDownloadClick: () -> Unit = {},
    onDownloadCancel: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    val isArtist = item.section == Section.ARTISTS
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "cardScale",
    )
    val textAlign = if (isArtist) TextAlign.Center else TextAlign.Start

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Box {
            Cover(imageUrl = item.imageUrl, isArtist = isArtist)
            if (downloads != null && item is Song) {
                DownloadButton(
                    downloads = downloads,
                    onClick = onDownloadClick,
                    onCancel = onDownloadCancel,
                    size = 32.dp,
                    container = AurelleBackgroundBottom.copy(alpha = 0.78f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            color = AurelleOnBackground,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = subtitleOf(item),
            style = MaterialTheme.typography.labelSmall,
            color = AurelleHint,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun subtitleOf(item: MediaItem): String = when (item) {
    is Artist -> item.subscriberCount
        ?.let { stringResource(R.string.subscribers_format, compactCount(it)) }
        ?: stringResource(R.string.artist_label)
    is Song -> item.artist
}

@Composable
private fun Cover(imageUrl: String?, isArtist: Boolean) {
    val shape = coverShape(isArtist)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(AurelleSurfaceHigh, AurelleSurface)))
            .border(1.dp, AurelleGold.copy(alpha = 0.12f), shape),
        contentAlignment = Alignment.Center,
    ) {
        // Ícone fica por baixo e some atrás da imagem quando ela carrega (ou se não houver imagem).
        Icon(
            imageVector = if (isArtist) Icons.Filled.Person else Icons.Filled.MusicNote,
            contentDescription = null,
            tint = AurelleGold.copy(alpha = 0.40f),
            modifier = Modifier.fillMaxSize(0.4f),
        )
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Esqueleto (com brilho) mostrado enquanto a primeira página carrega. */
@Composable
fun SkeletonCard(
    isArtist: Boolean,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(4.dp)
    Column(
        modifier = modifier,
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(coverShape(isArtist))
                .background(AurelleSurface)
                .shimmer()
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth(0.75f)
                .height(10.dp)
                .clip(barShape)
                .background(AurelleSurface)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth(0.5f)
                .height(8.dp)
                .clip(barShape)
                .background(AurelleSurface)
        )
    }
}
