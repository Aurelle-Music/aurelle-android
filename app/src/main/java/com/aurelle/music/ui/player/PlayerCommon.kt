package com.aurelle.music.ui.player

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aurelle.music.R
import com.aurelle.music.player.PlayerError
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleSurface
import com.aurelle.music.ui.theme.AurelleSurfaceHigh

/** Capa quadrada com o mesmo acabamento dos cards da Home (ícone por baixo até a imagem carregar). */
@Composable
fun PlayerArtwork(
    imageUrl: String?,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(AurelleSurfaceHigh, AurelleSurface)))
            .border(1.dp, AurelleGold.copy(alpha = 0.12f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
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

/** As capas do YouTube Music vêm pequenas (`=w60-h60...`); no player grande pedimos uma maior. */
fun largerArtwork(url: String?): String? =
    url?.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")

fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

@StringRes
fun PlayerError.messageRes(): Int = when (this) {
    PlayerError.AGE_RESTRICTED -> R.string.player_error_age_restricted
    PlayerError.UNAVAILABLE -> R.string.player_error_unavailable
    PlayerError.NETWORK -> R.string.player_error_network
    PlayerError.GENERIC -> R.string.player_error_generic
}
