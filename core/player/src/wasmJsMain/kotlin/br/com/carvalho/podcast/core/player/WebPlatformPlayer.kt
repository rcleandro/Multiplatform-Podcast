package br.com.carvalho.podcast.core.player

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.Episode
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLVideoElement

private const val TAG = "WebPlatformPlayer"
private const val MILLIS = 1000

/**
 * An HTML `<video>` element, which plays audio episodes as well, and the browser's Media Session (media keys, OS
 * controls). The element lives hidden in the page, and moves into the player while it shows a video episode (19.10).
 */
@OptIn(ExperimentalWasmJsInterop::class)
class WebPlatformPlayer : PlatformPlayer {
    private var listener: (PlatformEvent) -> Unit = {}
    private var episode: Episode? = null

    private val media = (window.document.createElement("video") as HTMLVideoElement).apply {
        style.width = "100%"
        style.height = "100%"
        style.backgroundColor = "black"
        hide()
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

    /** Moves the element into a box the player places on the page, and returns the box. */
    fun showVideo(): HTMLElement = (window.document.createElement("div") as HTMLElement).apply {
        media.style.display = "block"
        appendChild(media)
    }

    /**
     * Back to the hidden spot, in the same task the player's box goes away: an element out of the page stops
     * playing, one moved within it does not.
     */
    fun hideVideo() = media.hide()

    override val positionMs: Long get() = (media.currentTime * MILLIS).toLong()
    override val durationMs: Long? get() = media.duration.takeUnless { it.isNaN() }?.let { (it * MILLIS).toLong() }

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
        this.episode = episode
        updateMediaSessionMetadata(episode.title, episode.podcastTitle.orEmpty(), episode.imageUrl.orEmpty())
        media.src = episode.localPath ?: episode.audioUrl
        media.currentTime = positionMs / MILLIS.toDouble()
        if (playWhenReady) play()
    }

    override fun play() {
        // play() returns a promise that the browser rejects without a user gesture; the pause event reports it.
        @Suppress("TooGenericExceptionCaught")
        try {
            media.play()
        } catch (e: Throwable) {
            AppLogger.e(TAG, "The browser refused to play", e)
        }
    }

    override fun pause() {
        media.pause()
    }

    override fun seekTo(positionMs: Long) {
        media.currentTime = positionMs / MILLIS.toDouble()
    }

    override fun setVolume(volume: Float) {
        media.volume = volume.toDouble()
    }

    override fun setSpeed(speed: Float) {
        media.playbackRate = speed.toDouble()
        media.defaultPlaybackRate = speed.toDouble()
    }

    /** The chapter as the browser's title, over the episode's (19.1). */
    override fun showChapter(title: String?) {
        val current = episode ?: return
        updateMediaSessionMetadata(
            title ?: current.title,
            if (title == null) current.podcastTitle.orEmpty() else current.title,
            current.imageUrl.orEmpty(),
        )
    }
}

private fun HTMLVideoElement.hide() {
    style.display = "none"
    window.document.body?.appendChild(this)
}
