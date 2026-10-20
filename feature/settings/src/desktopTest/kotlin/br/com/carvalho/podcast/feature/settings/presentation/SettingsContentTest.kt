package br.com.carvalho.podcast.feature.settings.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.backup_not_valid
import br.com.carvalho.podcast.core.ui.generated.resources.delete
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_create_pin
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_notice
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_pin_ok
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_pin_wrong
import br.com.carvalho.podcast.core.ui.generated.resources.guardian_title
import br.com.carvalho.podcast.core.ui.generated.resources.opml_importing
import br.com.carvalho.podcast.core.ui.generated.resources.opml_nothing_to_import
import br.com.carvalho.podcast.core.ui.generated.resources.seconds_short
import br.com.carvalho.podcast.core.ui.generated.resources.settings
import br.com.carvalho.podcast.core.ui.generated.resources.settings_boost_voice
import br.com.carvalho.podcast.core.ui.generated.resources.settings_continuous_playback
import br.com.carvalho.podcast.core.ui.generated.resources.settings_delete_after
import br.com.carvalho.podcast.core.ui.generated.resources.settings_delete_after_days
import br.com.carvalho.podcast.core.ui.generated.resources.settings_delete_all
import br.com.carvalho.podcast.core.ui.generated.resources.settings_delete_finished
import br.com.carvalho.podcast.core.ui.generated.resources.settings_export_backup
import br.com.carvalho.podcast.core.ui.generated.resources.settings_locked_for_minors
import br.com.carvalho.podcast.core.ui.generated.resources.settings_show_explicit_locked
import br.com.carvalho.podcast.core.ui.generated.resources.stats_title
import br.com.carvalho.podcast.core.ui.generated.resources.settings_export_backup_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_export_opml
import br.com.carvalho.podcast.core.ui.generated.resources.settings_language
import br.com.carvalho.podcast.core.ui.generated.resources.settings_no_limit
import br.com.carvalho.podcast.core.ui.generated.resources.settings_refresh
import br.com.carvalho.podcast.core.ui.generated.resources.settings_show_explicit
import br.com.carvalho.podcast.core.ui.generated.resources.settings_show_explicit_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_skip_silence
import br.com.carvalho.podcast.core.ui.generated.resources.settings_telemetry
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme_dark
import br.com.carvalho.podcast.core.ui.generated.resources.settings_theme_system
import br.com.carvalho.podcast.core.ui.generated.resources.settings_sync_folder_description
import br.com.carvalho.podcast.core.ui.generated.resources.settings_sync_folder_failed
import br.com.carvalho.podcast.core.ui.generated.resources.settings_sync_folder_waiting
import br.com.carvalho.podcast.core.ui.generated.resources.settings_sync_stop
import br.com.carvalho.podcast.core.ui.generated.resources.settings_wifi_only
import br.com.carvalho.podcast.core.ui.generated.resources.size_gigabytes
import br.com.carvalho.podcast.core.util.DiskSpace
import br.com.carvalho.podcast.domain.model.AppLanguage
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.model.ThemeMode
import br.com.carvalho.podcast.domain.usecase.FolderSyncStatus
import br.com.carvalho.podcast.domain.usecase.OpmlImport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class SettingsContentTest {
    private fun text(res: StringResource, vararg args: Any) = runBlocking { getString(res, *args) }

    @Test
    fun theThemeIsPickedFromItsOptions() = runComposeUiTest {
        var chosen: ThemeMode? = null
        setContent {
            PodcastTheme {
                SettingsContent(SettingsUiState(), onBack = {}, onIntent = { (it as? SettingsIntent.SetTheme)?.let { chosen = it.mode } })
            }
        }

        onNode(isHeading() and hasText(text(Res.string.settings))).assertExists()
        // The row shows the current theme and opens its choices; picking one closes them.
        onNodeWithText(text(Res.string.settings_theme)).performClick()
        onNode(isSelectable() and hasText(text(Res.string.settings_theme_system))).assertIsSelected()
        onNodeWithText(text(Res.string.settings_theme_dark)).performClick()

        assertEquals(ThemeMode.DARK, chosen)
        onAllNodesWithText(text(Res.string.settings_theme_dark)).assertCountEquals(0)
    }

    @Test
    fun theOpmlRowsSayHowTheImportGoesAndExportWhenTapped() = runComposeUiTest {
        var status: OpmlStatus by mutableStateOf(OpmlStatus.Importing(done = 3, total = 40))
        val intents = mutableListOf<SettingsIntent>()
        setContent {
            PodcastTheme { SettingsContent(SettingsUiState(opml = status), onBack = {}, onIntent = { intents += it }) }
        }

        onNodeWithText(text(Res.string.opml_importing, 3, 40)).assertExists()
        status = OpmlStatus.Imported(OpmlImport(added = 0, known = 0, failed = 0))
        onNodeWithText(text(Res.string.opml_nothing_to_import)).assertExists()

        onNodeWithText(text(Res.string.settings_export_opml)).performClick()
        assertEquals(listOf<SettingsIntent>(SettingsIntent.ExportOpml), intents)
    }

    @Test
    fun theSyncFolderRowSaysHowTheLastExchangeWentAndStopOnlyShowsOnceChosen() = runComposeUiTest {
        var state by mutableStateOf(SettingsUiState())
        val intents = mutableListOf<SettingsIntent>()
        setContent { PodcastTheme { SettingsContent(state, onBack = {}, onIntent = { intents += it }) } }
        val stop = text(Res.string.settings_sync_stop)

        onNodeWithText(text(Res.string.settings_sync_folder_description)).performScrollTo().assertExists()
        onNodeWithText(stop).assertDoesNotExist()

        state = SettingsUiState(syncFolderName = "Sync")
        onNodeWithText(text(Res.string.settings_sync_folder_waiting, "Sync")).performScrollTo().assertExists()
        state = SettingsUiState(syncFolderName = "Sync", folderSync = FolderSyncStatus.Failed)
        onNodeWithText(text(Res.string.settings_sync_folder_failed, "Sync")).performScrollTo().assertExists()

        onNodeWithText(stop).performScrollTo().performClick()
        assertEquals(listOf<SettingsIntent>(SettingsIntent.StopSync), intents)
    }

    @Test
    fun eachLanguageIsOfferedInItsOwnName() = runComposeUiTest {
        var chosen: AppLanguage? = null
        setContent {
            PodcastTheme {
                SettingsContent(
                    SettingsUiState(language = AppLanguage.PORTUGUESE),
                    onBack = {},
                    onIntent = { (it as? SettingsIntent.SetLanguage)?.let { chosen = it.language } },
                )
            }
        }

        onNodeWithText("Português").assertExists()
        onNodeWithText(text(Res.string.settings_language)).performClick()
        onNode(isSelectable() and hasText("Português")).assertIsSelected()
        onNodeWithText("Español").performClick()

        assertEquals(AppLanguage.SPANISH, chosen)
        onAllNodesWithText("Español").assertCountEquals(0)
    }

    @Test
    fun explicitContentIsASwitchWithItsExplanation() = runComposeUiTest {
        var shown: Boolean? = null
        setContent {
            PodcastTheme {
                SettingsContent(
                    SettingsUiState(showExplicit = true),
                    onBack = {},
                    onIntent = { (it as? SettingsIntent.SetShowExplicit)?.let { shown = it.enabled } },
                )
            }
        }

        onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text(Res.string.settings_show_explicit)))
        onNodeWithText(text(Res.string.settings_show_explicit_description)).assertExists()
        onNodeWithText(text(Res.string.settings_show_explicit)).assertIsOn().performClick()

        assertEquals(false, shown)
    }

    @Test
    fun underTheEcaDigitalTheExplicitSwitchIsOffLockedAndSaysWhy() = runComposeUiTest {
        val intents = mutableListOf<SettingsIntent>()
        setContent {
            PodcastTheme {
                SettingsContent(
                    SettingsUiState(showExplicit = false, explicitLocked = true),
                    onBack = {},
                    onIntent = { intents += it },
                )
            }
        }

        onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text(Res.string.settings_show_explicit)))
        onNodeWithText(text(Res.string.settings_show_explicit_locked)).assertExists()
        onNodeWithText(text(Res.string.settings_show_explicit)).assertIsOff().assertIsNotEnabled().performClick()

        assertEquals(emptyList(), intents)
    }

    @Test
    fun theQueueMovingOnIsASwitchAMinorInBrazilCannotTurnOn() = runComposeUiTest {
        var state by mutableStateOf(SettingsUiState())
        val intents = mutableListOf<SettingsIntent>()
        setContent { PodcastTheme { SettingsContent(state, onBack = {}, onIntent = { intents += it }) } }
        val row = hasText(text(Res.string.settings_continuous_playback))
        onNode(hasScrollToNodeAction()).performScrollToNode(row)

        onNode(row).assertIsOn().performClick()
        assertEquals(listOf<SettingsIntent>(SettingsIntent.SetContinuousPlayback(false)), intents)

        state = SettingsUiState(isMinor = true)
        onNodeWithText(text(Res.string.settings_locked_for_minors)).assertExists()
        onNode(row).assertIsOff().assertIsNotEnabled()
    }

    @Test
    fun inBrazilTheGuardiansSectionSaysWhatIsInEffectAndAsksForThePin() = runComposeUiTest {
        val intents = mutableListOf<SettingsIntent>()
        var state by mutableStateOf(SettingsUiState())
        setContent { PodcastTheme { SettingsContent(state, onBack = {}, onIntent = { intents += it }) } }
        onAllNodesWithText(text(Res.string.guardian_title), ignoreCase = true).assertCountEquals(0)

        state = SettingsUiState(isMinor = true, guardian = GuardianState(inBrazil = true))
        val list = onNode(hasScrollToNodeAction())
        list.performScrollToNode(hasText(text(Res.string.guardian_notice)))
        list.performScrollToNode(hasText(text(Res.string.guardian_create_pin)))
        onNodeWithText(text(Res.string.guardian_create_pin)).performClick()
        assertEquals(listOf<SettingsIntent>(GuardianIntent.Open), intents)

        state = state.copy(guardian = state.guardian.copy(pinStep = PinStep.UNLOCK, pinError = PinError.WRONG))
        onNodeWithText(text(Res.string.guardian_pin_wrong)).assertExists()
        onNode(hasSetTextAction()).performTextInput("12a34")
        onNodeWithText(text(Res.string.guardian_pin_ok)).performClick()
        assertEquals(GuardianIntent.EnterPin("1234"), intents.last())
    }

    @Test
    fun aMinorInBrazilSeesNoTelemetryRow() = runComposeUiTest {
        setContent { PodcastTheme { SettingsContent(SettingsUiState(isMinor = true), onBack = {}, onIntent = {}) } }

        onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text(Res.string.settings_show_explicit)))
        onAllNodesWithText(text(Res.string.settings_telemetry)).assertCountEquals(0)
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

        // Last in the list: scrolled to, below the subscription rows.
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text(Res.string.settings_telemetry)))
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

        onNode(hasScrollToNodeAction()).performScrollToNode(hasText("50", substring = true))
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
        val isSlider = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo) and hasStateDescription(text(Res.string.settings_no_limit))
        onNode(hasScrollToNodeAction()).performScrollToNode(isSlider)
        val slider = onNode(isSlider)

        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(FIVE_GB_STOP) }

        assertEquals(5 * GB, limit)
        // A computer has no mobile data: no Wi-Fi only switch.
        onAllNodesWithText(text(Res.string.settings_wifi_only)).assertCountEquals(0)
        // Nor a player that can skip silences or lift voices (18.16): those switches are Android's.
        onAllNodesWithText(text(Res.string.settings_skip_silence)).assertCountEquals(0)
        onAllNodesWithText(text(Res.string.settings_boost_voice)).assertCountEquals(0)
        // Nor a background refresh: a computer looks for new episodes when the app opens (18.18).
        onAllNodesWithText(text(Res.string.settings_refresh)).assertCountEquals(0)
    }

    @Test
    fun theDiskShowsWhatTheDownloadsTakeAndWhatIsFree() = runComposeUiTest {
        val state = SettingsUiState(usedBytes = 1 * GB, disk = DiskSpace(totalBytes = 64 * GB, freeBytes = 12 * GB))
        setContent { PodcastTheme { SettingsContent(state, onBack = {}, onIntent = {}) } }

        // One sentence for screen readers, with the sizes of the bar, in the runner's language.
        val app = runBlocking { getString(Res.string.size_gigabytes, 1, 0) }
        val free = runBlocking { getString(Res.string.size_gigabytes, 12, 0) }
        val bar = hasContentDescription(app, substring = true) and hasContentDescription(free, substring = true)
        onNode(hasScrollToNodeAction()).performScrollToNode(bar)
        onNode(bar).assertExists()
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
        val list = onNode(hasScrollToNodeAction())
        list.performScrollToNode(hasText(text(Res.string.settings_delete_finished)))
        onAllNodesWithText(text(Res.string.settings_delete_all)).assertCountEquals(0)

        state = SettingsUiState(usedBytes = GB)
        list.performScrollToNode(hasText(text(Res.string.settings_delete_all)))
        onNodeWithText(text(Res.string.settings_delete_all)).performClick()
        state = state.copy(isConfirmingDeleteAll = true)
        onNodeWithText(text(Res.string.delete)).performClick()

        assertEquals(listOf(SettingsIntent.RequestDeleteAll, SettingsIntent.ConfirmDeleteAll), intents)
    }

    @Test
    fun howLongDownloadsStayIsPickedFromItsChoices() = runComposeUiTest {
        val intents = mutableListOf<SettingsIntent>()
        setContent { PodcastTheme { SettingsContent(SettingsUiState(usedBytes = 0), onBack = {}, onIntent = { intents += it }) } }
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text(Res.string.settings_delete_after)))

        onNodeWithText(text(Res.string.settings_delete_after)).performClick()
        onNodeWithText(runBlocking { getPluralString(Res.plurals.settings_delete_after_days, 7, 7) }).performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.SetDeleteAfter(7)), intents)
    }

    @Test
    fun eachJumpIsPickedAmongTheSecondsTheButtonsShow() = runComposeUiTest {
        val intents = mutableListOf<SettingsIntent>()
        setContent { PodcastTheme { SettingsContent(SettingsUiState(), onBack = {}, onIntent = { intents += it }) } }

        // Skip back starts at 10 s and skip forward at 30 s; the first "5 s" is skip back's.
        onAllNodesWithText(text(Res.string.seconds_short, 5))[0].performClick()

        assertEquals(listOf<SettingsIntent>(SettingsIntent.SetSkipIntervals(SkipIntervals(5, 30))), intents)
        // Only the jumps with a numbered icon: no 15 s.
        onAllNodesWithText(text(Res.string.seconds_short, 15)).assertCountEquals(0)
    }

    private companion object {
        const val GB = 1024L * 1024 * 1024
        const val FIVE_GB_STOP = 5f
    }

    @Test
    fun theBackupRowsSayWhatTheyKeepAndRefuseAnotherFile() = runComposeUiTest {
        val intents = mutableListOf<SettingsIntent>()
        var state by mutableStateOf(SettingsUiState())
        setContent { PodcastTheme { SettingsContent(state, onBack = {}, onIntent = { intents += it }) } }
        val list = onNode(hasScrollToNodeAction())

        list.performScrollToNode(hasText(text(Res.string.settings_export_backup)))
        onNodeWithText(text(Res.string.settings_export_backup_description)).assertExists()
        onNodeWithText(text(Res.string.settings_export_backup)).performClick()
        assertEquals(listOf<SettingsIntent>(SettingsIntent.ExportBackup), intents)

        state = state.copy(backup = OpmlStatus.NotABackup)
        list.performScrollToNode(hasText(text(Res.string.backup_not_valid)))
    }

    @Test
    fun theListeningStatsOpenFromTheTopOfSettings() = runComposeUiTest {
        var opened = 0
        setContent {
            PodcastTheme { SettingsContent(SettingsUiState(), onBack = {}, onIntent = {}, onOpenStats = { opened++ }) }
        }

        onNodeWithText(text(Res.string.stats_title)).performClick()

        assertEquals(1, opened)
    }
}
