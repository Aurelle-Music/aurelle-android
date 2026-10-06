package com.aurelle.music.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurelle.music.R
import com.aurelle.music.download.DownloadStatus
import com.aurelle.music.download.SongDownloads
import com.aurelle.music.player.PlaybackProgress
import com.aurelle.music.player.PlayerUiState
import com.aurelle.music.player.RepeatState
import com.aurelle.music.ui.components.AurelleBackdrop
import com.aurelle.music.ui.download.DownloadButton
import com.aurelle.music.ui.download.messageRes
import com.aurelle.music.ui.theme.AurelleBackgroundBottom
import com.aurelle.music.ui.theme.AurelleGold
import com.aurelle.music.ui.theme.AurelleGoldLight
import com.aurelle.music.ui.theme.AurelleHint
import com.aurelle.music.ui.theme.AurelleOnBackground
import com.aurelle.music.ui.theme.AurelleSurface
import com.aurelle.music.ui.theme.AurelleSurfaceHigh
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Player completo: capa grande, título, barra de busca (seek), tempos e controles. */
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    progress: StateFlow<PlaybackProgress>,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    downloads: SongDownloads,
    onOpenDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = state.nowPlaying ?: return
    BackHandler(onBack = onClose)

    // Puxar para baixo fecha o player: o gesto só "pega" quando o conteúdo está no topo (aninhado ao scroll).
    val scope = rememberCoroutineScope()
    val currentOnClose by rememberUpdatedState(onClose)
    var heightPx by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val pullConnection = remember {
        PullDownConnection(
            getOffset = { dragY },
            setOffset = { dragY = it },
            settle = { velocityY ->
                val shouldClose = heightPx > 0f && (dragY > heightPx * CLOSE_FRACTION || velocityY > CLOSE_VELOCITY)
                if (shouldClose) {
                    animate(dragY, heightPx, velocityY, tween(180)) { value, _ -> dragY = value }
                    currentOnClose()
                    // Se o player for reaberto durante a animação de saída, volta para a posição normal.
                    scope.launch {
                        delay(400)
                        dragY = 0f
                    }
                } else {
                    animate(dragY, 0f, velocityY, spring(stiffness = Spring.StiffnessMedium)) { value, _ ->
                        dragY = value
                    }
                }
            },
        )
    }

    // O pointerInput impede que toques "vazem" para a tela que está por baixo.
    AurelleBackdrop(
        modifier
            .onSizeChanged { heightPx = it.height.toFloat() }
            .graphicsLayer { translationY = dragY }
            .nestedScroll(pullConnection)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(
                state = state,
                onClose = onClose,
                trailing = {
                    DownloadButton(
                        downloads = downloads,
                        onClick = onOpenDownload,
                        onCancel = onCancelDownload,
                    )
                },
            )
            Spacer(Modifier.height(20.dp))

            PlayerArtwork(
                imageUrl = largerArtwork(now.artworkUrl),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Spacer(Modifier.height(28.dp))

            Text(
                text = now.title,
                style = MaterialTheme.typography.titleLarge,
                color = AurelleOnBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = now.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = AurelleHint,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            val error = state.error
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(error.messageRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = AurelleGoldLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val downloadError = (downloads.summary as? DownloadStatus.Failed)?.error
            if (downloadError != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(downloadError.messageRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = AurelleGoldLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(20.dp))
            SeekBar(progress = progress, onSeek = onSeek)
            Spacer(Modifier.height(12.dp))
            Controls(
                state = state,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onPrevious = onPrevious,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

private const val CLOSE_FRACTION = 0.22f   // fecha se arrastou mais de 22% da altura...
private const val CLOSE_VELOCITY = 1400f    // ...ou soltou com um empurrão rápido (px/s)

/**
 * Gesto de puxar para baixo, encaixado no scroll vertical do player: quando o conteúdo já está no topo, o
 * que "sobra" do arrasto para baixo move a tela inteira; ao soltar, ela fecha ou volta ao lugar.
 */
private class PullDownConnection(
    private val getOffset: () -> Float,
    private val setOffset: (Float) -> Unit,
    private val settle: suspend (velocityY: Float) -> Unit,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val offset = getOffset()
        // Arrastando para cima com a tela já puxada: primeiro devolve a tela, depois rola o conteúdo.
        if (source == NestedScrollSource.UserInput && offset > 0f && available.y < 0f) {
            val consumed = maxOf(available.y, -offset)
            setOffset(offset + consumed)
            return Offset(0f, consumed)
        }
        return Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && available.y > 0f) {
            setOffset(getOffset() + available.y)
            return Offset(0f, available.y)
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (getOffset() <= 0f) return Velocity.Zero
        settle(available.y)
        return available
    }
}

@Composable
private fun TopBar(state: PlayerUiState, onClose: () -> Unit, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AurelleSurface)
                .border(1.dp, AurelleGold.copy(alpha = 0.25f), CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.player_close),
                tint = AurelleGoldLight,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.player_now_playing),
                style = MaterialTheme.typography.labelLarge,
                color = AurelleGoldLight,
            )
            if (state.queueSize > 1) {
                Text(
                    text = stringResource(
                        R.string.player_queue_position, state.queueIndex + 1, state.queueSize
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = AurelleHint,
                )
            }
        }
        // Mesmo tamanho do botão de fechar (48dp), então o título continua centralizado.
        trailing()
    }
}

/** Isolada para que só ela recomponha a cada atualização de posição. */
@Composable
private fun SeekBar(progress: StateFlow<PlaybackProgress>, onSeek: (Long) -> Unit) {
    val p by progress.collectAsStateWithLifecycle()
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    val fraction = if (p.durationMs > 0) (p.positionMs.toFloat() / p.durationMs).coerceIn(0f, 1f) else 0f
    val shownPosition = dragFraction?.let { (it * p.durationMs).toLong() } ?: p.positionMs

    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = dragFraction ?: fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onSeek((it * p.durationMs).toLong()) }
                dragFraction = null
            },
            enabled = p.durationMs > 0,
            colors = SliderDefaults.colors(
                thumbColor = AurelleGoldLight,
                activeTrackColor = AurelleGold,
                inactiveTrackColor = AurelleSurfaceHigh,
                disabledThumbColor = AurelleHint,
                disabledActiveTrackColor = AurelleHint,
                disabledInactiveTrackColor = AurelleSurfaceHigh,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(shownPosition),
                style = MaterialTheme.typography.labelMedium,
                color = AurelleHint,
            )
            Text(
                text = formatTime(p.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = AurelleHint,
            )
        }
    }
}

@Composable
private fun Controls(
    state: PlayerUiState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleShuffle, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = stringResource(R.string.player_shuffle),
                tint = if (state.shuffle) AurelleGoldLight else AurelleHint,
                modifier = Modifier.size(26.dp),
            )
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = stringResource(R.string.player_previous),
                tint = AurelleOnBackground,
                modifier = Modifier.size(38.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AurelleGold)
                .clickable(onClick = onTogglePlay),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isBuffering) {
                CircularProgressIndicator(
                    color = AurelleBackgroundBottom,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (state.isPlaying) R.string.player_pause else R.string.player_play
                    ),
                    tint = AurelleBackgroundBottom,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        IconButton(onClick = onNext, enabled = state.hasNext, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = stringResource(R.string.player_next),
                tint = if (state.hasNext) AurelleOnBackground else AurelleHint.copy(alpha = 0.5f),
                modifier = Modifier.size(38.dp),
            )
        }
        IconButton(onClick = onCycleRepeat, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = if (state.repeat == RepeatState.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = stringResource(R.string.player_repeat),
                tint = if (state.repeat == RepeatState.OFF) AurelleHint else AurelleGoldLight,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
