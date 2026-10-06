package com.aurelle.music.player

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurelle.music.data.Song
import com.aurelle.music.download.DownloadedTrack
import kotlinx.coroutines.flow.StateFlow

/** Estado do player para a UI (escopo da Activity, compartilhado por mini player e player completo). */
class PlayerViewModel(application: Application) : ViewModel() {

    private val controller = PlayerController(application, viewModelScope)

    val state: StateFlow<PlayerUiState> = controller.state
    val progress: StateFlow<PlaybackProgress> = controller.progress

    /** Toca [song] criando a fila com as músicas de [queue] (a lista de onde ela foi tocada). */
    fun play(queue: List<Song>, song: Song) {
        val index = queue.indexOfFirst { it.id == song.id }
        if (index >= 0) controller.playQueue(queue, index) else controller.playQueue(listOf(song), 0)
    }

    /** Toca [track] a partir do arquivo baixado, com a fila de [queue] (a lista filtrada da Biblioteca). */
    fun playLocal(queue: List<DownloadedTrack>, track: DownloadedTrack) {
        val index = queue.indexOfFirst { it.key == track.key }
        if (index >= 0) controller.playLocalQueue(queue, index) else controller.playLocalQueue(listOf(track), 0)
    }

    fun togglePlayPause() = controller.togglePlayPause()
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)
    fun toggleShuffle() = controller.toggleShuffle()
    fun cycleRepeat() = controller.cycleRepeat()

    override fun onCleared() {
        // Só solta a conexão: o serviço (e a música) continuam se estiver tocando.
        controller.release()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PlayerViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
            }
        }
    }
}
