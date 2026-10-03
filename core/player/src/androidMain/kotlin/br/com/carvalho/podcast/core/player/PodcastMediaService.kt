package br.com.carvalho.podcast.core.player

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import br.com.carvalho.podcast.core.AppConfig
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

private const val TAG = "PodcastMediaService"
private const val CUSTOM_COMMAND_SKIP_FORWARD = "CUSTOM_COMMAND_SKIP_FORWARD"
private const val CUSTOM_COMMAND_SKIP_BACKWARD = "CUSTOM_COMMAND_SKIP_BACKWARD"

class PodcastMediaService : MediaLibraryService() {
    private var mediaLibrarySession: MediaLibrarySession? = null
    private val podcastRepository: PodcastRepository by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        AppLogger.d(TAG, "onCreate")

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .build()
        )

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                // Pauses for calls and other apps, and ducks or resumes as the system asks.
                true
            )
            // Pauses when headphones are unplugged instead of going on through the speaker.
            .setHandleAudioBecomingNoisy(true)
            // The same jumps as the app's buttons.
            .setSeekForwardIncrementMs(AppConfig.SKIP_FORWARD_SECONDS * AppConfig.MILLIS_PER_SECOND)
            .setSeekBackIncrementMs(AppConfig.SKIP_BACKWARD_SECONDS * AppConfig.MILLIS_PER_SECOND)
            .build()

        val sessionActivityPendingIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.let { sessionIntent ->
                android.app.PendingIntent.getActivity(
                    this,
                    0,
                    sessionIntent,
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .apply {
                sessionActivityPendingIntent?.let { setSessionActivity(it) }
            }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        AppLogger.d(TAG, "onGetSession from ${controllerInfo.packageName}")
        return mediaLibrarySession
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val skipForwardCommand = SessionCommand(CUSTOM_COMMAND_SKIP_FORWARD, Bundle.EMPTY)
            val skipBackwardCommand = SessionCommand(CUSTOM_COMMAND_SKIP_BACKWARD, Bundle.EMPTY)

            val availableSessionCommands = SessionCommands.Builder()
                .add(skipForwardCommand)
                .add(skipBackwardCommand)
                .build()

            val forward = AppConfig.SKIP_FORWARD_SECONDS
            val backward = AppConfig.SKIP_BACKWARD_SECONDS
            val customLayout = listOf(
                CommandButton.Builder(skipForwardIcon(forward))
                    .setSessionCommand(skipForwardCommand)
                    .setDisplayName(getString(R.string.skip_forward, forward))
                    .build(),
                CommandButton.Builder(skipBackwardIcon(backward))
                    .setSessionCommand(skipBackwardCommand)
                    .setDisplayName(getString(R.string.skip_backward, backward))
                    .build()
            )


            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommands)
                .setCustomLayout(ImmutableList.copyOf(customLayout))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CUSTOM_COMMAND_SKIP_FORWARD -> {
                    session.player.seekForward()
                }
                CUSTOM_COMMAND_SKIP_BACKWARD -> {
                    session.player.seekBack()
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        @OptIn(UnstableApi::class)
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            AppLogger.d(TAG, "onGetLibraryRoot")
            val rootItem = MediaItem.Builder()
                .setMediaId("root")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        @OptIn(UnstableApi::class)
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            AppLogger.d(TAG, "onGetChildren parentId = $parentId")
            return serviceScope.future {
                val items = withContext(Dispatchers.IO) {
                    when (parentId) {
                        "root" -> {
                            listOf(
                                MediaItem.Builder()
                                    .setMediaId("library_node")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(getString(R.string.library_title))
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS)
                                            .setExtras(Bundle().apply {
                                                putInt(
                                                    MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                                                    MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                                                )
                                            })
                                            .build()
                                    )
                                    .build()
                            )
                        }

                        "library_node" -> {
                            podcastRepository.getPodcasts().first().map { podcast ->
                                MediaItem.Builder()
                                    .setMediaId("podcast_${podcast.id}")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(podcast.title)
                                            .setArtist(podcast.author)
                                            .setArtworkUri(podcast.imageUrl?.toUri())
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS)
                                            .setExtras(Bundle().apply {
                                                putInt(
                                                    MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                                                    MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                                                )
                                            })
                                            .build()
                                    )
                                    .build()
                            }
                        }

                        else -> {
                            if (parentId.startsWith("podcast_")) {
                                val podcastId = parentId.removePrefix("podcast_")
                                val podcast = podcastRepository.getPodcastById(podcastId)
                                podcastRepository.getEpisodes(podcastId).first().map { episode ->
                                    MediaItem.Builder()
                                        .setMediaId(episode.id)
                                        .setUri(episode.audioUrl)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(episode.title)
                                                .setArtist(podcast?.title ?: "")
                                                .setArtworkUri(
                                                    episode.imageUrl?.toUri()
                                                        ?: podcast?.imageUrl?.toUri()
                                                )
                                                .setIsBrowsable(false)
                                                .setIsPlayable(true)
                                                .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
                                                .build()
                                        )
                                        .build()
                                }
                            } else {
                                emptyList()
                            }
                        }
                    }
                }
                LibraryResult.ofItemList(items, params)
            }
        }

        @OptIn(UnstableApi::class)
        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            AppLogger.d(TAG, "onGetItem mediaId = $mediaId")
            return serviceScope.future {
                val item = withContext(Dispatchers.IO) {
                    when {
                        mediaId == "root" -> {
                            onGetLibraryRoot(session, browser, null).get().value
                        }

                        mediaId == "library_node" -> {
                            MediaItem.Builder()
                                .setMediaId("library_node")
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(getString(R.string.library_title))
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS)
                                        .build()
                                ).build()
                        }

                        mediaId.startsWith("podcast_") -> {
                            val podcastId = mediaId.removePrefix("podcast_")
                            val podcast = podcastRepository.getPodcastById(podcastId)
                            podcast?.let {
                                MediaItem.Builder()
                                    .setMediaId(mediaId)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(it.title)
                                            .setArtist(it.author)
                                            .setArtworkUri(it.imageUrl?.toUri())
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS)
                                            .build()
                                    )
                                    .build()
                            }
                        }

                        else -> {
                            val episode = podcastRepository.getEpisodeById(mediaId)
                            episode?.let { ep ->
                                val podcast = podcastRepository.getPodcastById(ep.podcastId)
                                MediaItem.Builder()
                                    .setMediaId(ep.id)
                                    .setUri(ep.audioUrl)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(ep.title)
                                            .setArtist(podcast?.title ?: "")
                                            .setArtworkUri(
                                                ep.imageUrl?.toUri() ?: podcast?.imageUrl?.toUri()
                                            )
                                            .setIsBrowsable(false)
                                            .setIsPlayable(true)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
                                            .build()
                                    )
                                    .build()
                            }
                        }
                    }
                }

                if (item != null) {
                    LibraryResult.ofItem(item, null)
                } else {
                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                }
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            AppLogger.d(TAG, "onAddMediaItems count = ${mediaItems.size}")
            return serviceScope.future {
                mediaItems.map { item ->
                    if (item.localConfiguration?.uri != null) {
                        item
                    } else {
                        onGetItem(mediaSession as MediaLibrarySession, controller, item.mediaId)
                            .await().value ?: item
                    }
                }.toMutableList()
            }
        }
    }

    override fun onDestroy() {
        AppLogger.d(TAG, "onDestroy")
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        AppLogger.d(TAG, "onTaskRemoved")
        val player = mediaLibrarySession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }
}

/** Media3's icon with the jump's number when there is one; a plain arrow otherwise. */
private fun skipForwardIcon(seconds: Int): Int = when (seconds) {
    SHORT_JUMP -> CommandButton.ICON_SKIP_FORWARD_5
    MEDIUM_JUMP -> CommandButton.ICON_SKIP_FORWARD_10
    QUARTER_JUMP -> CommandButton.ICON_SKIP_FORWARD_15
    LONG_JUMP -> CommandButton.ICON_SKIP_FORWARD_30
    else -> CommandButton.ICON_SKIP_FORWARD
}

private fun skipBackwardIcon(seconds: Int): Int = when (seconds) {
    SHORT_JUMP -> CommandButton.ICON_SKIP_BACK_5
    MEDIUM_JUMP -> CommandButton.ICON_SKIP_BACK_10
    QUARTER_JUMP -> CommandButton.ICON_SKIP_BACK_15
    LONG_JUMP -> CommandButton.ICON_SKIP_BACK_30
    else -> CommandButton.ICON_SKIP_BACK
}

private const val SHORT_JUMP = 5
private const val MEDIUM_JUMP = 10
private const val QUARTER_JUMP = 15
private const val LONG_JUMP = 30
