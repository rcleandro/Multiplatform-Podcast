package br.com.carvalho.podcast.feature.settings.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.settings
import br.com.carvalho.podcast.core.ui.generated.resources.settings_telemetry
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme_dark
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme_system
import br.com.carvalho.podcast.domain.model.ThemeMode
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsContentTest {
    private fun text(res: StringResource) = runBlocking { getString(res) }

    @Test
    fun theThemeIsPickedFromItsOptions() = runComposeUiTest {
        var chosen: ThemeMode? = null
        setContent {
            PodcastTheme { SettingsContent(SettingsUiState(), onBack = {}, onThemeChange = { chosen = it }, onTelemetryChange = {}) }
        }

        onNode(isHeading() and hasText(text(Res.string.settings))).assertExists()
        onNodeWithText(text(Res.string.settings_theme_system)).assertIsSelected()
        onNodeWithText(text(Res.string.settings_theme_dark)).performClick()

        assertEquals(ThemeMode.DARK, chosen)
    }

    @Test
    fun telemetryIsASwitchWithItsExplanation() = runComposeUiTest {
        var enabled: Boolean? = null
        setContent {
            PodcastTheme {
                SettingsContent(
                    SettingsUiState(telemetryEnabled = false),
                    onBack = {},
                    onThemeChange = {},
                    onTelemetryChange = { enabled = it },
                )
            }
        }

        onNodeWithText(text(Res.string.settings_telemetry)).assertIsOff()
        onNodeWithText(text(Res.string.settings_telemetry)).performClick()

        assertEquals(true, enabled)
    }

    @Test
    fun theDownloadSpaceShowsOnlyOnceMeasured() = runComposeUiTest {
        setContent {
            PodcastTheme {
                SettingsContent(SettingsUiState(usedBytes = 52_428_800), onBack = {}, onThemeChange = {}, onTelemetryChange = {})
            }
        }

        onNode(hasText("50", substring = true)).assertExists()
    }

    @Test
    fun backLeavesTheScreen() = runComposeUiTest {
        var back = 0
        setContent {
            PodcastTheme { SettingsContent(SettingsUiState(), onBack = { back++ }, onThemeChange = {}, onTelemetryChange = {}) }
        }

        onNodeWithContentDescription(text(Res.string.back)).performClick()

        assertEquals(1, back)
    }
}
