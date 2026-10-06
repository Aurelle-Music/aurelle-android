@file:OptIn(UnstableApi::class)

package com.aurelle.music.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aurelle.music.data.Song
import com.aurelle.music.download.DownloadTarget
import com.aurelle.music.download.DownloadedTrack
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Ponte entre a UI e o [PlaybackService]. A UI nunca fala com o ExoPlayer: só com este controller,
 * que usa um [MediaController] (conectado à MediaSession do serviço) e expõe tudo por [StateFlow].
 *
 * Todo o estado é lido do controller, então ao reabrir o app o mini player reaparece sozinho se
 * o serviço continuou tocando.
 */
class PlayerController(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(PlaybackProgress())
    val progress: StateFlow<PlaybackProgress> = _progress.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()
    private var progressJob: Job? = null
    private var autoSkipJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState(player)
        }
    }

    init {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val connected = runCatching { future.get() }.getOrNull()
                if (connected != null) onConnected(connected)
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    private fun onConnected(connected: MediaController) {
        controller = connected
        connected.addListener(listener)
        syncState(connected)
        pending.forEach { it(connected) }
        pending.clear()
        startProgressLoop()
    }

    // --- Comandos ------------------------------------------------------------------------------

    /** Toca [songs] como fila, começando em [startIndex]. */
    fun playQueue(songs: List<Song>, startIndex: Int) =
        playItems(songs.map { it.toMediaItem() }, startIndex)

    /** Toca músicas baixadas direto do arquivo (funciona sem internet). */
    fun playLocalQueue(tracks: List<DownloadedTrack>, startIndex: Int) =
        playItems(tracks.map { it.toLocalMediaItem() }, startIndex)

    private fun playItems(items: List<MediaItem>, startIndex: Int) {
        if (items.isEmpty()) return
        withController { c ->
            c.setMediaItems(items, startIndex.coerceIn(items.indices), 0L)
            c.prepare()
            c.play()
        }
    }

    fun togglePlayPause() = withController { c ->
        if (_state.value.isPlaying) {
            c.pause()
        } else {
            if (c.playerError != null || c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() = withController { c ->
        c.seekToNext()
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    /** Volta ao início da música se já passou de ~3 s; senão, vai para a anterior. */
    fun previous() = withController { c ->
        c.seekToPrevious()
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }

    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun release() {
        progressJob?.cancel()
        autoSkipJob?.cancel()
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pending += block
    }

    // --- Estado --------------------------------------------------------------------------------

    private fun syncState(player: Player) {
        val item = player.currentMediaItem
        val nowPlaying = item?.let {
            NowPlaying(
                id = it.mediaId,
                title = it.mediaMetadata.title?.toString().orEmpty(),
                artist = it.mediaMetadata.artist?.toString().orEmpty(),
                artworkUrl = it.mediaMetadata.artworkUri?.toString(),
                durationHintMs = it.mediaMetadata.durationMs,
                album = it.mediaMetadata.albumTitle?.toString(),
                trackNumber = it.mediaMetadata.trackNumber,
            )
        }
        val error = player.playerError?.let(::mapError)
        val playbackState = player.playbackState
        val wantsToPlay = player.playWhenReady &&
            playbackState != Player.STATE_ENDED &&
            playbackState != Player.STATE_IDLE

        _state.value = PlayerUiState(
            nowPlaying = nowPlaying,
            isPlaying = wantsToPlay,
            isBuffering = player.playWhenReady && playbackState == Player.STATE_BUFFERING,
            hasNext = player.hasNextMediaItem(),
            repeat = when (player.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatState.ALL
                Player.REPEAT_MODE_ONE -> RepeatState.ONE
                else -> RepeatState.OFF
            },
            shuffle = player.shuffleModeEnabled,
            queueIndex = player.currentMediaItemIndex,
            queueSize = player.mediaItemCount,
            error = error,
        )
        scheduleAutoSkip(player, error)
    }

    /** Música indisponível no meio da fila: mostra o aviso por um instante e pula para a próxima. */
    private fun scheduleAutoSkip(player: Player, error: PlayerError?) {
        autoSkipJob?.cancel()
        val skippable = error == PlayerError.UNAVAILABLE || error == PlayerError.AGE_RESTRICTED
        if (!skippable || !player.hasNextMediaItem()) return
        val failedId = player.currentMediaItem?.mediaId
        autoSkipJob = scope.launch {
            delay(AUTO_SKIP_DELAY_MS)
            val c = controller ?: return@launch
            if (c.currentMediaItem?.mediaId == failedId && c.playerError != null) {
                c.seekToNext()
                c.prepare()
                c.play()
            }
        }
    }

    private fun mapError(error: PlaybackException): PlayerError {
        // A causa original não atravessa a MediaSession; só a mensagem. Por isso as etiquetas "aurelle:".
        val text = generateSequence<Throwable>(error) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
        return when {
            "aurelle:AGE_RESTRICTED" in text -> PlayerError.AGE_RESTRICTED
            "aurelle:UNAVAILABLE" in text -> PlayerError.UNAVAILABLE
            "aurelle:http_" in text -> PlayerError.UNAVAILABLE
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> PlayerError.NETWORK
            else -> PlayerError.GENERIC
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                controller?.let { c ->
                    val duration = c.duration
                    _progress.update {
                        PlaybackProgress(
                            positionMs = c.currentPosition.coerceAtLeast(0L),
                            durationMs = if (duration == C.TIME_UNSET) {
                                _state.value.nowPlaying?.durationHintMs ?: 0L
                            } else {
                                duration
                            },
                            bufferedMs = c.bufferedPosition.coerceAtLeast(0L),
                        )
                    }
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    private fun Song.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(id) // URL da página do vídeo; a AurelleDataSource resolve o stream real
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setTrackNumber(trackNumber)
                    .setArtworkUri(imageUrl?.let(Uri::parse))
                    .setDurationMs(durationSeconds?.times(1000L))
                    .build()
            )
            .build()

    private fun DownloadedTrack.toLocalMediaItem(): MediaItem {
        val localUri = when (target) {
            DownloadTarget.APP -> Uri.fromFile(File(uri))
            DownloadTarget.DEVICE -> Uri.parse(uri)
        }
        return MediaItem.Builder()
            .setMediaId(id) // mesmo id da música online: o botão de baixar do player continua certo
            .setUri(localUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setTrackNumber(trackNumber)
                    .setArtworkUri(imageUrl?.let(Uri::parse))
                    .setDurationMs(durationSeconds?.times(1000L))
                    .build()
            )
            .build()
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 300L
        const val AUTO_SKIP_DELAY_MS = 2_000L
    }
}
