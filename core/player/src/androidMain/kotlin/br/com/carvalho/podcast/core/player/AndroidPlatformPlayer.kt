package br.com.carvalho.podcast.core.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.Episode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "AndroidPlatformPlayer"
private const val EXTRA_EPISODE_TITLE = "episodeTitle"
private const val EXTRA_PODCAST_TITLE = "podcastTitle"

/**
 * Media3 through a [MediaController] connected to [PodcastMediaService], which owns the ExoPlayer, the notification
 * and Android Auto. Commands sent before the connection is ready wait for it.
 */
@Suppress("TooManyFunctions") // the PlatformPlayer interface plus the controller's connection
class AndroidPlatformPlayer(context: Context) : PlatformPlayer {
    private val controllerFuture = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PodcastMediaService::class.java)),
    ).buildAsync()
    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var listener: (PlatformEvent) -> Unit = {}

    override val positionMs: Long get() = controller?.currentPosition ?: 0
    override val durationMs: Long? get() = controller?.duration?.takeIf { it >= 0 }
    override val loadedEpisodeId: String? get() = controller?.currentMediaItem?.mediaId

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = listener(PlatformEvent.PlayingChanged(isPlaying))

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> listener(PlatformEvent.BufferingChanged(true))
                Player.STATE_READY -> listener(PlatformEvent.BufferingChanged(false))
                Player.STATE_ENDED -> listener(PlatformEvent.Ended)
            }
        }
    }

    override fun setListener(listener: (PlatformEvent) -> Unit) {
        this.listener = listener
    }

    override suspend fun awaitReady() {
        connected()
    }

    override fun load(episode: Episode, positionMs: Long, playWhenReady: Boolean) = command {
        val item = MediaItem.Builder()
            .setMediaId(episode.id)
            .setUri(episode.playableUri())
            // A downloaded episode plays from its file; a Chromecast needs the address it came from (18.17).
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(episode.audioUrl.toUri()).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setArtist(episode.podcastTitle)
                    .setArtworkUri(episode.imageUrl?.toUri())
                    .build()
            )
            .build()
        it.setMediaItem(item, positionMs)
        it.playWhenReady = playWhenReady
        it.prepare()
    }

    override fun play() = command { it.play() }

    override fun pause() = command { it.pause() }

    override fun seekTo(positionMs: Long) = command { it.seekTo(positionMs) }

    override fun setSpeed(speed: Float) = command { it.playbackParameters = PlaybackParameters(speed) }

    override fun setVolume(volume: Float) = command { it.volume = volume }

    /**
     * The chapter as the title, over the episode's (19.1); without one, the episode over its podcast. The same item
     * with new metadata: Media3 swaps it without loading the audio again.
     */
    override fun showChapter(title: String?) = command {
        val item = it.currentMediaItem ?: return@command
        // A Chromecast would load the episode again; the chapter shows only while the phone plays.
        if (it.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) return@command
        val metadata = item.mediaMetadata
        val episodeTitle = metadata.extras?.getString(EXTRA_EPISODE_TITLE) ?: metadata.title?.toString()
        val podcastTitle = metadata.extras?.getString(EXTRA_PODCAST_TITLE) ?: metadata.artist?.toString()
        val shown = metadata.buildUpon()
            .setTitle(title ?: episodeTitle)
            .setArtist(if (title == null) podcastTitle else episodeTitle)
            .setExtras(bundleOf(EXTRA_EPISODE_TITLE to episodeTitle, EXTRA_PODCAST_TITLE to podcastTitle))
            .build()
        it.replaceMediaItem(it.currentMediaItemIndex, item.buildUpon().setMediaMetadata(shown).build())
    }

    private fun command(block: (MediaController) -> Unit) {
        controller?.let(block) ?: scope.launch { connected()?.let(block) }
    }

    /** The connected controller; the first call connects and reports what the service is already doing. */
    @Suppress("TooGenericExceptionCaught") // the future fails with whatever the binder throws; playback just stays off
    private suspend fun connected(): MediaController? {
        controller?.let { return it }
        return try {
            controllerFuture.await().also {
                controller = it
                it.addListener(playerListener)
                if (it.isPlaying) listener(PlatformEvent.PlayingChanged(true))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Could not connect to the media service", e)
            null
        }
    }
}

private fun Episode.playableUri(): Uri = localPath?.let { Uri.fromFile(File(it)) } ?: audioUrl.toUri()
