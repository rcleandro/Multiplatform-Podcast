package br.com.carvalho.podcast.core.player

import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

private const val TAG = "PodcastMediaService"
private const val CUSTOM_COMMAND_SKIP_FORWARD = "CUSTOM_COMMAND_SKIP_FORWARD"

/** How much quiet voices are lifted: 6 dB, clearly louder without clipping most speech. */
private const val VOICE_BOOST_MILLIBELS = 600
private const val CUSTOM_COMMAND_SKIP_BACKWARD = "CUSTOM_COMMAND_SKIP_BACKWARD"

class PodcastMediaService : MediaLibraryService() {
    private var mediaLibrarySession: MediaLibrarySession? = null
    private val podcastRepository: PodcastRepository by inject()
    private val preferences: PreferencesRepository by inject()
    private val tree by lazy { MediaTree(podcastRepository) { getString(R.string.library_title) } }
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** Lifts quiet voices (18.16); tied to the player's audio session, made again if that changes. */
    private var voiceBoost: LoudnessEnhancer? = null

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
            // The jumps chosen in Settings for the system's own seek (headphones, for instance). ExoPlayer takes them
            // once: a change reaches it when the service starts again. The buttons below read them on every tap.
            .setSeekForwardIncrementMs(preferences.skipIntervals.value.forwardSeconds * AppConfig.MILLIS_PER_SECOND)
            .setSeekBackIncrementMs(preferences.skipIntervals.value.backwardSeconds * AppConfig.MILLIS_PER_SECOND)
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

        val isWatch = packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
        if (isWatch) pauseWithoutHeadphones(player)
        // Plays on the phone, or on a Chromecast while a Cast session is on, carrying the position across (18.17). A
        // watch has no Cast (19.9a): there it is the local player alone.
        val sessionPlayer: Player = if (isWatch) {
            player
        } else {
            CastPlayer.Builder(this)
                .setLocalPlayer(player)
                .setRemotePlayer(RemoteCastPlayer.Builder(this).setMediaItemConverter(EpisodeCastConverter()).build())
                .build()
        }

        mediaLibrarySession = MediaLibrarySession.Builder(this, sessionPlayer, LibraryCallback())
            .apply {
                sessionActivityPendingIntent?.let { setSessionActivity(it) }
            }
            .build()
        // Settings' speech switches (18.16), applied as they change.
        serviceScope.launch { preferences.skipSilence.collect { player.skipSilenceEnabled = it } }
        serviceScope.launch { preferences.boostVoice.collect { boostVoice(player, it) } }
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) = boostVoice(player, preferences.boostVoice.value)
        })
        // The notification and Android Auto show the jumps on their buttons: new ones when Settings change.
        serviceScope.launch {
            preferences.skipIntervals.drop(1).collect { intervals ->
                mediaLibrarySession?.setCustomLayout(ImmutableList.copyOf(skipButtons(intervals)))
            }
        }
    }

    /**
     * With "Headphones only" on (19.9f), playing with nothing but the watch's speaker pauses at once and opens the
     * system's output switcher, as the watch's own media apps do. Read on every play, so the switch counts right away.
     */
    private fun pauseWithoutHeadphones(player: Player) = player.addListener(object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (playWhenReady && preferences.headphonesOnly.value && !hasHeadphones()) {
                player.pause()
                chooseAudioOutput()
            }
        }
    })

    @OptIn(UnstableApi::class)
    private fun boostVoice(player: ExoPlayer, enabled: Boolean) {
        voiceBoost?.release()
        voiceBoost = null
        if (!enabled) return
        // ponytail: one fixed gain; a strength slider if someone finds it too much or too little.
        voiceBoost = runCatching {
            LoudnessEnhancer(player.audioSessionId).apply {
                setTargetGain(VOICE_BOOST_MILLIBELS)
                setEnabled(true)
            }
        }.onFailure { AppLogger.e(TAG, "Could not boost voices", it) }.getOrNull()
    }

    private val skipForwardCommand = SessionCommand(CUSTOM_COMMAND_SKIP_FORWARD, Bundle.EMPTY)
    private val skipBackwardCommand = SessionCommand(CUSTOM_COMMAND_SKIP_BACKWARD, Bundle.EMPTY)

    @OptIn(UnstableApi::class)
    private fun skipButtons(intervals: SkipIntervals): List<CommandButton> = listOf(
        CommandButton.Builder(skipForwardIcon(intervals.forwardSeconds))
            .setSessionCommand(skipForwardCommand)
            .setDisplayName(getString(R.string.skip_forward, intervals.forwardSeconds))
            .build(),
        CommandButton.Builder(skipBackwardIcon(intervals.backwardSeconds))
            .setSessionCommand(skipBackwardCommand)
            .setDisplayName(getString(R.string.skip_backward, intervals.backwardSeconds))
            .build(),
    )

    /** Android Auto from the store, not an app that took its name (`hasSigningCertificate` follows key rotation). */
    private fun isSignedByAndroidAuto(packageName: String): Boolean =
        // ponytail: Android 8 has no hasSigningCertificate; there Android Auto counts only through Media3's own trust.
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && packageManager.hasSigningCertificate(
            packageName,
            ANDROID_AUTO_CERTIFICATE_SHA256.hexToByteArray(),
            PackageManager.CERT_INPUT_SHA256,
        )

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
            // Other installed apps get the transport only (22.2, T1 of ADR 0014): no library, nothing new to play.
            val trusted = browsesLibrary(controller.isTrusted, controller.packageName) {
                isSignedByAndroidAuto(controller.packageName)
            }
            val customLayout = skipButtons(preferences.skipIntervals.value)

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommandsFor(trusted, skipForwardCommand, skipBackwardCommand))
                .setAvailablePlayerCommands(playerCommandsFor(trusted))
                .setCustomLayout(ImmutableList.copyOf(customLayout))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            // By the jumps chosen in Settings, read now: ExoPlayer's own increments only change when it is created.
            val intervals = preferences.skipIntervals.value
            val player = session.player
            when (customCommand.customAction) {
                CUSTOM_COMMAND_SKIP_FORWARD -> player.seekTo(
                    (player.currentPosition + intervals.forwardSeconds * AppConfig.MILLIS_PER_SECOND)
                        .coerceAtMost(player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
                )
                CUSTOM_COMMAND_SKIP_BACKWARD -> player.seekTo(
                    (player.currentPosition - intervals.backwardSeconds * AppConfig.MILLIS_PER_SECOND).coerceAtLeast(0)
                )
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
                .setMediaId(ROOT_ID)
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
                val items = withContext(Dispatchers.IO) { tree.children(parentId) }
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
                    if (mediaId == ROOT_ID) onGetLibraryRoot(session, browser, null).get().value else tree.item(mediaId)
                }

                if (item != null) {
                    LibraryResult.ofItem(item, null)
                } else {
                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                }
            }
        }

        // The car's search box finds what a voice search would play (20.7).
        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            serviceScope.launch {
                val count = withContext(Dispatchers.IO) { tree.search(query).size }
                session.notifySearchResultChanged(browser, query, count, params)
            }
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        // ponytail: one page with every match; page through it if a library grows big enough to need it.
        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = serviceScope.future {
            LibraryResult.ofItemList(withContext(Dispatchers.IO) { tree.search(query) }, params)
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            AppLogger.d(TAG, "onAddMediaItems count = ${mediaItems.size}")
            return serviceScope.future {
                // A voice search comes as one item with only the words the user said.
                mediaItems.singleOrNull()?.requestMetadata?.searchQuery?.let { query ->
                    return@future withContext(Dispatchers.IO) { tree.search(query) }.toMutableList()
                }
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
        // The Settings collectors would reach a released player otherwise.
        serviceScope.cancel()
        voiceBoost?.release()
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

internal const val ANDROID_AUTO_PACKAGE = "com.google.android.projection.gearhead"

/** Android Auto's signing certificate (SHA-256), read from the store's build on a Razr 60 on 23/10/2026. */
private const val ANDROID_AUTO_CERTIFICATE_SHA256 = "1ca8dcc0bed3cbd872d2cb791200c0292ca9975768a82d676b8b424fb65b5295"

/**
 * Whether a controller sees the library and may change what plays (22.2). Media3's trust covers this app, the system
 * and apps with media control access or a notification listener; Android Auto has only the listener, which people turn
 * off, so it also counts when it is the real, Google-signed Android Auto.
 */
internal fun browsesLibrary(trusted: Boolean, packageName: String, signedByAndroidAuto: () -> Boolean): Boolean =
    trusted || (packageName == ANDROID_AUTO_PACKAGE && signedByAndroidAuto())

/**
 * What a controller may ask the session. A trusted one (this app, the system's notification and lock screen, Android
 * Auto, apps with media control access or a notification listener; see `ControllerInfo.isTrusted`) gets Media3's
 * defaults, library included: starting from an empty set had hidden the library from Android Auto (20.7). Any other app
 * gets only [custom], the jump buttons, and never browses or searches the library (22.2).
 */
@OptIn(UnstableApi::class)
internal fun sessionCommandsFor(trusted: Boolean, vararg custom: SessionCommand): SessionCommands =
    (if (trusted) MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS else SessionCommands.EMPTY)
        .buildUpon()
        .apply { custom.forEach { add(it) } }
        .build()

/**
 * What a controller may do to the player. Any other app may play, pause and move within the episode and read what is
 * playing, as a headset helper would. Only a trusted one sees the queue or changes what plays: a new media item could
 * be any URL.
 */
@OptIn(UnstableApi::class)
internal fun playerCommandsFor(trusted: Boolean): Player.Commands =
    if (trusted) {
        MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
    } else {
        MediaSession.ConnectionResult.DEFAULT_UNTRUSTED_PLAYER_COMMANDS.buildUpon()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_BACK,
                Player.COMMAND_SEEK_FORWARD,
                Player.COMMAND_STOP,
            )
            // The timeline lists the whole queue; the current episode alone is in the metadata.
            .remove(Player.COMMAND_GET_TIMELINE)
            .build()
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
