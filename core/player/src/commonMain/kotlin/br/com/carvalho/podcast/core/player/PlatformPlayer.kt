package br.com.carvalho.podcast.core.player

import br.com.carvalho.podcast.domain.model.Episode

/**
 * The native audio player of one platform (Media3, AVPlayer, JavaFX, HTML audio), translated and nothing more: queue,
 * end of episode, progress, saving and sleep timer live in [PlaybackController]. Called on the main thread.
 */
interface PlatformPlayer {
    val positionMs: Long

    /** `null` until the media says how long it is. */
    val durationMs: Long?

    /** Receives what the platform reports; set once by the controller. */
    fun setListener(listener: (PlatformEvent) -> Unit)

    /** Returns when the player can take commands (Android connects to its media service first). */
    suspend fun awaitReady() = Unit

    /** Loads [episode] from its local file or URL, at [positionMs], and starts it if [playWhenReady]. */
    fun load(episode: Episode, positionMs: Long, playWhenReady: Boolean)

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
}

sealed interface PlatformEvent {
    data class PlayingChanged(val isPlaying: Boolean) : PlatformEvent
    data class BufferingChanged(val isBuffering: Boolean) : PlatformEvent
    data object Ended : PlatformEvent

    /** A command from outside the app: lock screen, notification, headset or browser media keys. */
    data class Remote(val command: RemoteCommand) : PlatformEvent
}

enum class RemoteCommand { PLAY, PAUSE, SKIP_FORWARD, SKIP_BACKWARD, NEXT, PREVIOUS }
