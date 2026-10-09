package br.com.carvalho.podcast.feature.settings.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Settings on a phone, dark theme (18.5). Recorded on Linux by the "Record snapshots" workflow. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SettingsSnapshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun settings() {
        compose.setContent {
            PodcastTheme(darkTheme = true) {
                SettingsContent(SettingsUiState(usedBytes = 52_428_800), onBack = {}, onThemeChange = {}, onTelemetryChange = {})
            }
        }
        compose.onRoot().captureRoboImage("snapshots/settings.png")
    }
}
