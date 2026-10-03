package br.com.carvalho.podcast.core.player

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.Episode
import kotlinx.browser.window
import org.w3c.dom.HTMLAudioElement

private const val TAG = "WebPlatformPlayer"
private const val MILLIS = 1000

/** An HTML `<audio>` element and the browser's Media Session (media keys, OS controls). */
@OptIn(ExperimentalWasmJsInterop::class)
class WebPlatformPlayer : PlatformPlayer {
    private var listener: (PlatformEvent) -> Unit = {}

    private val audio = (window.document.createElement("audio") as HTMLAudioElement).apply {
        onplaying = {
            listener(PlatformEvent.BufferingChanged(false))
            listener(PlatformEvent.PlayingChanged(true))
            updateMediaSessionPlaybackState("playing")
        }
        onpause = {
            listener(PlatformEvent.PlayingChanged(false))
            updateMediaSessionPlaybackState("paused")
        }
        onwaiting = { listener(PlatformEvent.BufferingChanged(true)) }
        onended = { listener(PlatformEvent.Ended) }
    }

    override val positionMs: Long get() = (audio.currentTime * MILLIS).toLong()
    override val durationMs: Long? get() = audio.duration.takeUnless { it.isNaN() }?.let { (it * MILLIS).toLong() }

    init {
        fun remote(command: RemoteCommand): () -> Unit = { listener(PlatformEvent.Remote(command)) }
        setupMediaSessionActions(
            onPlay = remote(RemoteCommand.PLAY),
            onPause = remote(RemoteCommand.PAUSE),
            onSeekBackward = remote(RemoteCommand.SKIP_BACKWARD),
            onSeekForward = remote(RemoteCommand.SKIP_FORWARD),
            onPreviousTrack = remote(RemoteCommand.PREVIOUS),
            onNextTrack = remote(RemoteCommand.NEXT),
        )
    }

    override fun setListener(listener: (PlatformEvent) -> Unit) {
        this.listener = listener
    }

    override fun load(episode: Episode, positionMs: Long, playWhenReady: Boolean) {
        updateMediaSessionMetadata(episode.title, episode.podcastTitle.orEmpty(), episode.imageUrl.orEmpty())
        audio.src = episode.localPath ?: episode.audioUrl
        audio.currentTime = positionMs / MILLIS.toDouble()
        if (playWhenReady) play()
    }

    override fun play() {
        // play() returns a promise that the browser rejects without a user gesture; the pause event reports it.
        @Suppress("TooGenericExceptionCaught")
        try {
            audio.play()
        } catch (e: Throwable) {
            AppLogger.e(TAG, "The browser refused to play", e)
        }
    }

    override fun pause() {
        audio.pause()
    }

    override fun seekTo(positionMs: Long) {
        audio.currentTime = positionMs / MILLIS.toDouble()
    }

    override fun setSpeed(speed: Float) {
        audio.playbackRate = speed.toDouble()
        audio.defaultPlaybackRate = speed.toDouble()
    }
}
