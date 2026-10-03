package br.com.carvalho.podcast.core.player

import br.com.carvalho.podcast.domain.model.Episode

/** Records commands and lets the test raise the platform's events. */
class FakePlatformPlayer : PlatformPlayer {
    override var positionMs = 0L
    override var durationMs: Long? = null

    var loaded: Episode? = null
    var loadedAt: Long? = null
    var playWhenReady: Boolean? = null
    var isPlaying = false
    var currentSpeed = 1f

    private var listener: (PlatformEvent) -> Unit = {}

    override fun setListener(listener: (PlatformEvent) -> Unit) {
        this.listener = listener
    }

    override fun load(episode: Episode, positionMs: Long, playWhenReady: Boolean) {
        loaded = episode
        loadedAt = positionMs
        this.positionMs = positionMs
        this.playWhenReady = playWhenReady
        if (playWhenReady) play()
    }

    override fun play() {
        isPlaying = true
        listener(PlatformEvent.PlayingChanged(true))
    }

    override fun pause() {
        isPlaying = false
        listener(PlatformEvent.PlayingChanged(false))
    }

    override fun seekTo(positionMs: Long) {
        this.positionMs = positionMs
    }

    override fun setSpeed(speed: Float) {
        currentSpeed = speed
    }

    fun raise(event: PlatformEvent) = listener(event)
}
