package br.com.carvalho.podcast.core.player

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.Episode
import com.sun.javafx.application.PlatformImpl
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import java.io.File

private const val TAG = "DesktopPlatformPlayer"

/** JavaFX's [MediaPlayer]; one per episode, since JavaFX cannot change a player's media. */
class DesktopPlatformPlayer : PlatformPlayer {
    private var player: MediaPlayer? = null
    private var speed = 1f
    private var listener: (PlatformEvent) -> Unit = {}

    override val positionMs: Long get() = player?.currentTime?.toMillis()?.toLong() ?: 0
    override val durationMs: Long?
        get() = player?.totalDuration?.toMillis()?.toLong()?.takeIf { it > 0 }

    init {
        // Starts the JavaFX toolkit without a JavaFX window; the app's window is Compose.
        @Suppress("TooGenericExceptionCaught") // JavaFX reports a missing toolkit with any Throwable
        try {
            PlatformImpl.startup {}
        } catch (e: Throwable) {
            AppLogger.e(TAG, "Could not start JavaFX", e)
        }
    }

    override fun setListener(listener: (PlatformEvent) -> Unit) {
        this.listener = listener
    }

    override fun load(episode: Episode, positionMs: Long, playWhenReady: Boolean) {
        player?.dispose()
        listener(PlatformEvent.BufferingChanged(true))
        val uri = episode.localPath?.let { File(it).toURI().toString() } ?: episode.audioUrl
        player = MediaPlayer(Media(uri)).apply {
            onReady = Runnable {
                if (positionMs > 0) seek(Duration.millis(positionMs.toDouble()))
                rate = speed.toDouble()
                listener(PlatformEvent.BufferingChanged(false))
                if (playWhenReady) play()
            }
            onPlaying = Runnable {
                rate = speed.toDouble() // JavaFX can drop the rate across pause and seek
                listener(PlatformEvent.PlayingChanged(true))
            }
            onPaused = Runnable { listener(PlatformEvent.PlayingChanged(false)) }
            onStopped = Runnable { listener(PlatformEvent.PlayingChanged(false)) }
            onEndOfMedia = Runnable { listener(PlatformEvent.Ended) }
            onError = Runnable {
                AppLogger.e(TAG, "JavaFX could not play the episode: ${error?.message}")
                listener(PlatformEvent.PlayingChanged(false))
            }
        }
    }

    override fun play() {
        player?.play()
    }

    override fun pause() {
        player?.pause()
    }

    override fun seekTo(positionMs: Long) {
        player?.seek(Duration.millis(positionMs.toDouble()))
    }

    override fun setSpeed(speed: Float) {
        this.speed = speed
        player?.rate = speed.toDouble()
    }
}
