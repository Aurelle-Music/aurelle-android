package com.aurelle.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurelle.music.R
import com.aurelle.music.player.PlaybackProgress
import com.aurelle.music.player.PlayerUiState
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleHint
import com.aurelle.music.ui.theme.AurelleOnBackground
import com.aurelle.music.ui.theme.AurelleSurface
import kotlinx.coroutines.flow.StateFlow

/** Mini player acima do dock: capa, título/artista, play-pause, próxima e barra de progresso fina. */
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    progress: StateFlow<PlaybackProgress>,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = state.nowPlaying ?: return
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AurelleSurface)
            .border(1.dp, AurelleGold.copy(alpha = 0.14f), shape)
            .clickable(onClickLabel = stringResource(R.string.player_open), onClick = onOpen),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerArtwork(
                imageUrl = now.artworkUrl,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = now.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = AurelleOnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.error?.let { stringResource(it.messageRes()) } ?: now.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.error != null) AurelleGoldLight else AurelleHint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onTogglePlay) {
                if (state.isBuffering) {
                    CircularProgressIndicator(
                        color = AurelleGold,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(26.dp),
                    )
                } else {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (state.isPlaying) R.string.player_pause else R.string.player_play
                        ),
                        tint = AurelleGoldLight,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            IconButton(onClick = onNext, enabled = state.hasNext) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.player_next),
                    tint = if (state.hasNext) AurelleGoldLight else AurelleHint.copy(alpha = 0.5f),
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        MiniProgressBar(progress)
    }
}

/** Isolada para que só ela recomponha a cada atualização de posição. */
@Composable
private fun MiniProgressBar(progress: StateFlow<PlaybackProgress>) {
    val p by progress.collectAsStateWithLifecycle()
    val fraction = if (p.durationMs > 0) (p.positionMs.toFloat() / p.durationMs).coerceIn(0f, 1f) else 0f
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(AurelleGold.copy(alpha = 0.15f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(AurelleGold)
        )
    }
}
