package br.com.carvalho.podcast.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import br.com.carvalho.podcast.core.player.DesktopPlatformPlayer
import br.com.carvalho.podcast.core.player.PlatformPlayer
import br.com.carvalho.podcast.feature.player.presentation.VideoSurface
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.media.MediaView
import javafx.scene.paint.Color
import org.koin.compose.koinInject

internal actual val platformVideoSurface: VideoSurface? = { modifier -> DesktopVideoSurface(modifier) }

/** JavaFX's view of the episode's player, in a JavaFX panel inside the Compose window; letterboxed in black. */
@Composable
private fun DesktopVideoSurface(modifier: Modifier) {
    val platformPlayer = koinInject<PlatformPlayer>() as? DesktopPlatformPlayer ?: return
    val player by platformPlayer.mediaPlayer.collectAsState()
    val view = remember { MediaView().apply { isPreserveRatio = true } }
    LaunchedEffect(player) { Platform.runLater { view.mediaPlayer = player } }
    SwingPanel(
        factory = {
            JFXPanel().also { panel ->
                Platform.runLater {
                    val scene = Scene(StackPane(view), Color.BLACK)
                    view.fitWidthProperty().bind(scene.widthProperty())
                    view.fitHeightProperty().bind(scene.heightProperty())
                    panel.scene = scene
                }
            }
        },
        modifier = modifier,
    )
}
