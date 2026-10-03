package br.com.carvalho.podcast.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import br.com.carvalho.podcast.core.designsystem.component.ButtonsSample
import br.com.carvalho.podcast.core.designsystem.component.EpisodeRowsSample
import br.com.carvalho.podcast.core.designsystem.component.PlayerPartsSample
import br.com.carvalho.podcast.core.designsystem.component.PreviewSurface
import br.com.carvalho.podcast.core.designsystem.component.StatesSample
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * One image per component group, in both themes, plus the episode rows at 200% font scale.
 * Record with the "Record snapshots" workflow (Linux); `verifyRoborazziAndroidHostTest` checks them in CI.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class DesignSystemSnapshotTest {

    private fun snap(name: String, darkTheme: Boolean, content: @Composable () -> Unit) =
        captureRoboImage("snapshots/$name-${if (darkTheme) "dark" else "light"}.png") {
            PreviewSurface(darkTheme) { content() }
        }

    @Test fun buttonsLight() = snap("buttons", darkTheme = false) { ButtonsSample() }

    @Test fun buttonsDark() = snap("buttons", darkTheme = true) { ButtonsSample() }

    @Test fun episodeRowsLight() = snap("episode-rows", darkTheme = false) { EpisodeRowsSample() }

    @Test fun episodeRowsDark() = snap("episode-rows", darkTheme = true) { EpisodeRowsSample() }

    @Test fun playerPartsLight() = snap("player-parts", darkTheme = false) { PlayerPartsSample() }

    @Test fun playerPartsDark() = snap("player-parts", darkTheme = true) { PlayerPartsSample() }

    @Test fun statesLight() = snap("states", darkTheme = false) { StatesSample() }

    @Test fun statesDark() = snap("states", darkTheme = true) { StatesSample() }

    @Test
    fun episodeRowsLargeFont() = captureRoboImage("snapshots/episode-rows-font200-light.png") {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = LARGE_FONT_SCALE)) {
            PreviewSurface(darkTheme = false) { EpisodeRowsSample() }
        }
    }
}

private const val LARGE_FONT_SCALE = 2f
