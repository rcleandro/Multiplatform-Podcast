package br.com.carvalho.podcast.core.player

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.Episode
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.setRate
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import br.com.carvalho.podcast.core.AppConfig

private const val TAG = "IosPlatformPlayer"
private const val MILLIS = 1000

/** AVPlayer, the lock screen's now-playing info and its remote commands. */
@OptIn(ExperimentalForeignApi::class)
class IosPlatformPlayer : PlatformPlayer {
    private val player = AVPlayer()
    private var episode: Episode? = null
    private var speed = 1f
    private var isPlaying = false
    private var listener: (PlatformEvent) -> Unit = {}

    override val positionMs: Long get() = seconds(CMTimeGetSeconds(player.currentTime()))
    override val durationMs: Long?
        get() = player.currentItem?.duration?.let { CMTimeGetSeconds(it) }?.takeUnless { it.isNaN() }?.let(::seconds)

    init {
        setupIosAudioSession()
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = null,
        ) { _ -> listener(PlatformEvent.Ended) }
        setUpRemoteCommands()
    }

    override fun setListener(listener: (PlatformEvent) -> Unit) {
        this.listener = listener
    }

    override fun load(episode: Episode, positionMs: Long, playWhenReady: Boolean) {
        val url = episode.localPath?.let { NSURL.fileURLWithPath(it) } ?: NSURL.URLWithString(episode.audioUrl)
        if (url == null) {
            AppLogger.e(TAG, "Episode ${episode.id} has no playable URL")
            return
        }
        this.episode = episode
        player.replaceCurrentItemWithPlayerItem(AVPlayerItem.playerItemWithURL(url))
        if (positionMs > 0) player.seekToTime(CMTimeMake(positionMs, MILLIS))
        if (playWhenReady) play() else updateNowPlaying()
    }

    override fun play() {
        player.setRate(speed)
        setPlaying(true)
    }

    override fun pause() {
        player.pause()
        setPlaying(false)
    }

    override fun seekTo(positionMs: Long) {
        player.seekToTime(CMTimeMake(positionMs, MILLIS))
        updateNowPlaying()
    }

    override fun setSpeed(speed: Float) {
        this.speed = speed
        if (isPlaying) player.setRate(speed)
        updateNowPlaying()
    }

    private fun setPlaying(playing: Boolean) {
        isPlaying = playing
        listener(PlatformEvent.PlayingChanged(playing))
        updateNowPlaying()
    }

    private fun updateNowPlaying() {
        val current = episode ?: return
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = mapOf<Any?, Any?>(
            MPMediaItemPropertyTitle to current.title,
            MPMediaItemPropertyArtist to current.podcastTitle,
            MPNowPlayingInfoPropertyElapsedPlaybackTime to positionMs / MILLIS.toDouble(),
            MPMediaItemPropertyPlaybackDuration to (durationMs ?: 0) / MILLIS.toDouble(),
            MPNowPlayingInfoPropertyPlaybackRate to if (isPlaying) speed.toDouble() else 0.0,
        )
    }

    private fun setUpRemoteCommands() {
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        fun forward(command: RemoteCommand): (Any?) -> Long = {
            listener(PlatformEvent.Remote(command))
            MPRemoteCommandHandlerStatusSuccess
        }
        center.playCommand.addTargetWithHandler(forward(RemoteCommand.PLAY))
        center.pauseCommand.addTargetWithHandler(forward(RemoteCommand.PAUSE))
        center.nextTrackCommand.addTargetWithHandler(forward(RemoteCommand.NEXT))
        center.previousTrackCommand.addTargetWithHandler(forward(RemoteCommand.PREVIOUS))
        center.skipForwardCommand.preferredIntervals = listOf(AppConfig.SKIP_FORWARD_SECONDS.toDouble())
        center.skipForwardCommand.addTargetWithHandler(forward(RemoteCommand.SKIP_FORWARD))
        center.skipBackwardCommand.preferredIntervals = listOf(AppConfig.SKIP_BACKWARD_SECONDS.toDouble())
        center.skipBackwardCommand.addTargetWithHandler(forward(RemoteCommand.SKIP_BACKWARD))
    }

    private fun seconds(value: Double): Long = if (value.isNaN()) 0 else (value * MILLIS).toLong()
}
