package br.com.carvalho.podcast.feature.settings.presentation

import br.com.carvalho.podcast.core.ui.generated.resources.settings_delete_all
import br.com.carvalho.podcast.core.ui.generated.resources.delete
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import br.com.carvalho.podcast.core.ui.generated.resources.size_gigabytes
import br.com.carvalho.podcast.core.util.DiskSpace
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.semantics.SemanticsActions
import br.com.carvalho.podcast.core.ui.generated.resources.settings_wifi_only
import br.com.carvalho.podcast.core.ui.generated.resources.settings_no_limit
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
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
            PodcastTheme {
                SettingsContent(SettingsUiState(), onBack = {}, onIntent = { (it as? SettingsIntent.SetTheme)?.let { chosen = it.mode } })
            }
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
                    onIntent = { (it as? SettingsIntent.SetTelemetry)?.let { enabled = it.enabled } },
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
                SettingsContent(SettingsUiState(usedBytes = 52_428_800), onBack = {}, onIntent = {})
            }
        }

        onNode(hasText("50", substring = true)).assertExists()
    }

    @Test
    fun theDownloadLimitIsPickedOnASliderThatEndsWithNoLimit() = runComposeUiTest {
        var limit: Long? = -1
        val state = SettingsUiState(usedBytes = 0, disk = DiskSpace(totalBytes = 64 * GB, freeBytes = 12 * GB))
        setContent {
            PodcastTheme {
                SettingsContent(state, onBack = {}, onIntent = { (it as? SettingsIntent.SetDownloadLimit)?.let { limit = it.bytes } })
            }
        }
        // 12 GB fit: half a gigabyte, then 1 to 10 GB, then no limit, where it starts.
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo) and hasStateDescription(text(Res.string.settings_no_limit)))

        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(FIVE_GB_STOP) }

        assertEquals(5 * GB, limit)
        // A computer has no mobile data: no Wi-Fi only switch.
        onAllNodesWithText(text(Res.string.settings_wifi_only)).assertCountEquals(0)
    }

    @Test
    fun theDiskShowsWhatTheDownloadsTakeAndWhatIsFree() = runComposeUiTest {
        val state = SettingsUiState(usedBytes = 1 * GB, disk = DiskSpace(totalBytes = 64 * GB, freeBytes = 12 * GB))
        setContent { PodcastTheme { SettingsContent(state, onBack = {}, onIntent = {}) } }

        // One sentence for screen readers, with the sizes of the bar, in the runner's language.
        val app = runBlocking { getString(Res.string.size_gigabytes, 1, 0) }
        val free = runBlocking { getString(Res.string.size_gigabytes, 12, 0) }
        onNode(hasContentDescription(app, substring = true) and hasContentDescription(free, substring = true)).assertExists()
    }

    @Test
    fun backLeavesTheScreen() = runComposeUiTest {
        var back = 0
        setContent {
            PodcastTheme { SettingsContent(SettingsUiState(), onBack = { back++ }, onIntent = {}) }
        }

        onNodeWithContentDescription(text(Res.string.back)).performClick()

        assertEquals(1, back)
    }

    @Test
    fun deletingAllDownloadsIsOfferedOnlyWithDownloadsAndAsksFirst() = runComposeUiTest {
        val intents = mutableListOf<SettingsIntent>()
        var state by mutableStateOf(SettingsUiState(usedBytes = 0))
        setContent { PodcastTheme { SettingsContent(state, onBack = {}, onIntent = { intents += it }) } }
        onAllNodesWithText(text(Res.string.settings_delete_all)).assertCountEquals(0)

        state = SettingsUiState(usedBytes = GB)
        onNodeWithText(text(Res.string.settings_delete_all)).performScrollTo().performClick()
        state = state.copy(isConfirmingDeleteAll = true)
        onNodeWithText(text(Res.string.delete)).performClick()

        assertEquals(listOf(SettingsIntent.RequestDeleteAll, SettingsIntent.ConfirmDeleteAll), intents)
    }

    private companion object {
        const val GB = 1024L * 1024 * 1024
        const val FIVE_GB_STOP = 5f
    }
}
