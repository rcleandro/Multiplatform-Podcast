package br.com.carvalho.podcast.presentation

import android.app.PictureInPictureParams
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import br.com.carvalho.podcast.core.player.AndroidPlatformPlayer
import br.com.carvalho.podcast.core.player.PlatformPlayer
import br.com.carvalho.podcast.feature.player.presentation.VideoSurface
import org.koin.compose.koinInject

private const val PICTURE_WIDTH = 16
private const val PICTURE_HEIGHT = 9

internal actual val platformVideoSurface: VideoSurface? = { modifier -> AndroidVideoSurface(modifier) }

/**
 * Media3's frame over the media service's player. While the video plays the screen stays on, and leaving the app
 * shrinks it to picture-in-picture.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun AndroidVideoSurface(modifier: Modifier) {
    val platformPlayer = koinInject<PlatformPlayer>() as? AndroidPlatformPlayer ?: return
    val player = produceState<Player?>(null, platformPlayer) { value = platformPlayer.videoPlayer() }.value ?: return
    val playing = !rememberPlayPauseButtonState(player).showPlay
    val view = LocalView.current
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(playing, view, activity) {
        view.keepScreenOn = playing
        val shrink = Runnable {
            val ratio = Rational(PICTURE_WIDTH, PICTURE_HEIGHT)
            activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(ratio).build())
        }
        if (playing) activity?.addOnUserLeaveHintListener(shrink)
        onDispose {
            view.keepScreenOn = false
            activity?.removeOnUserLeaveHintListener(shrink)
        }
    }
    ContentFrame(player, modifier)
}
