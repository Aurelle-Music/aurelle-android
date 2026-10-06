package com.aurelle.music.player

/** Por que a música não tocou (a UI traduz para uma mensagem simples). */
enum class PlayerError { AGE_RESTRICTED, UNAVAILABLE, NETWORK, GENERIC }

enum class RepeatState { OFF, ALL, ONE }

data class NowPlaying(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    /** Duração conhecida pela busca; usada até o player descobrir a real. */
    val durationHintMs: Long?,
    val album: String? = null,
    val trackNumber: Int? = null,
)

/** Estado "lento" do player (muda só em eventos). A posição fica em [PlaybackProgress]. */
data class PlayerUiState(
    val nowPlaying: NowPlaying? = null,
    /** Verdadeiro quando o usuário quer ouvir (tocando ou carregando). Define o ícone play/pause. */
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasNext: Boolean = false,
    val repeat: RepeatState = RepeatState.OFF,
    val shuffle: Boolean = false,
    val queueIndex: Int = 0,
    val queueSize: Int = 0,
    val error: PlayerError? = null,
)

/** Posição atual; atualizada várias vezes por segundo, por isso fica separada do resto. */
data class PlaybackProgress(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
)
