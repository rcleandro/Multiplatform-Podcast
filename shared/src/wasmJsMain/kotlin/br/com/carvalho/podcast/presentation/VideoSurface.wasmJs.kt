package br.com.carvalho.podcast.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import br.com.carvalho.podcast.core.player.PlatformPlayer
import br.com.carvalho.podcast.core.player.WebPlatformPlayer
import br.com.carvalho.podcast.feature.player.presentation.VideoSurface
import org.koin.compose.koinInject

internal actual val platformVideoSurface: VideoSurface? = { modifier -> WebVideoSurface(modifier) }

/** The player's own `<video>` element over the page's canvas; the browser offers picture-in-picture on it. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WebVideoSurface(modifier: Modifier) {
    val player = koinInject<PlatformPlayer>() as? WebPlatformPlayer ?: return
    HtmlElementView(factory = player::showVideo, modifier = modifier, onRelease = { player.hideVideo() })
}
