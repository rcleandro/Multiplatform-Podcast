package br.com.carvalho.podcast.feature.settings.presentation

import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.AgeGroup
import br.com.carvalho.podcast.domain.model.AppLanguage
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.GuardianControls
import br.com.carvalho.podcast.domain.model.GuardianPin
import br.com.carvalho.podcast.domain.model.PinAttempts
import br.com.carvalho.podcast.domain.model.PinLockout
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.SyncFolder
import br.com.carvalho.podcast.domain.model.ThemeMode
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakeAgeSignalSource
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakeOpmlFormat
import br.com.carvalho.podcast.domain.repository.FakePlayerRepository
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.ChildSafety
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import br.com.carvalho.podcast.domain.usecase.ExplicitContent
import br.com.carvalho.podcast.domain.usecase.ExplicitRule
import br.com.carvalho.podcast.domain.usecase.ExportBackupUseCase
import br.com.carvalho.podcast.domain.usecase.ExportOpmlUseCase
import br.com.carvalho.podcast.domain.usecase.FolderSyncUseCase
import br.com.carvalho.podcast.domain.usecase.ImportOpmlUseCase
import br.com.carvalho.podcast.domain.usecase.LibrarySyncUseCase
import br.com.carvalho.podcast.domain.usecase.OpmlImport
import br.com.carvalho.podcast.domain.usecase.RestoreBackupUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val preferences = FakePreferencesRepository()
    private val downloader = FakeEpisodeDownloader().apply { usedBytes = USED_BYTES }
    private val repository = FakePodcastRepository()
    private val feedSource = FakeFeedSource()
    private val playerRepository = FakePlayerRepository()
    private val player = FakeAudioPlayer()


    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        SettingsViewModel(
            preferences, downloader, AppDirectories(FakeFileSystem(), "/app".toPath()), repository,
            CoroutineDispatchers(main = dispatcher, io = dispatcher),
            ImportOpmlUseCase(FakeOpmlFormat, AddPodcastFromUrlUseCase(feedSource, repository)),
            ExportOpmlUseCase(repository, FakeOpmlFormat),
            ExportBackupUseCase(repository, playerRepository, preferences, player),
            RestoreBackupUseCase(
                repository, playerRepository, preferences, player, AddPodcastFromUrlUseCase(feedSource, repository),
            ),
            FolderSyncUseCase(
                LibrarySyncUseCase(
                    repository,
                    player,
                    AddPodcastFromUrlUseCase(feedSource, repository),
                    DeletePodcastUseCase(repository, downloader),
                ),
            ),
            ExplicitContent(explicitRule),
            childSafety,
            now = { clock },
        )

    /** The time the PIN's wait sees (22.11). */
    private var clock = 0L

    private val ageSignal = FakeAgeSignalSource(countries = setOf("BR"), ageGroup = AgeGroup.ADULT)
    private val childSafety = ChildSafety(ageSignal)

    @Test
    fun wrongPinsMakeTheNextTryWaitEvenForTheRightOneAndAcrossRestarts() = runTest(dispatcher) {
        childSafety.refresh()
        preferences.setGuardianPin(GuardianPin.hash("1234"))
        val viewModel = viewModel()
        viewModel.onIntent(GuardianIntent.Open)

        repeat(PinLockout.FREE_FAILURES - 1) { viewModel.onIntent(GuardianIntent.EnterPin("0000")) }
        assertEquals(PinError.WRONG, viewModel.uiState.value.guardian.pinError)
        viewModel.onIntent(GuardianIntent.EnterPin("0000"))
        assertEquals(PinError.WAIT, viewModel.uiState.value.guardian.pinError)
        assertEquals(30, viewModel.uiState.value.guardian.pinWaitSeconds)

        // While it waits, not even the right PIN opens, and a new Settings screen remembers the wait.
        clock += 10_000
        val reopened = viewModel()
        reopened.onIntent(GuardianIntent.Open)
        reopened.onIntent(GuardianIntent.EnterPin("1234"))
        assertFalse(reopened.uiState.value.guardian.unlocked)
        assertEquals(20, reopened.uiState.value.guardian.pinWaitSeconds)

        clock += 20_000
        reopened.onIntent(GuardianIntent.EnterPin("1234"))
        assertTrue(reopened.uiState.value.guardian.unlocked)
        assertEquals(PinAttempts(), preferences.guardianPinAttempts.value)
    }

    @Test
    fun theGuardiansCreateTheirPinAndOnlyItOpensTheControls() = runTest(dispatcher) {
        childSafety.refresh()
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.guardian.inBrazil)

        viewModel.onIntent(GuardianIntent.Open)
        assertEquals(PinStep.CREATE, viewModel.uiState.value.guardian.pinStep)
        viewModel.onIntent(GuardianIntent.EnterPin("1234"))
        viewModel.onIntent(GuardianIntent.EnterPin("9999"))
        assertEquals(PinError.MISMATCH, viewModel.uiState.value.guardian.pinError)
        viewModel.onIntent(GuardianIntent.EnterPin("1234"))
        viewModel.onIntent(GuardianIntent.EnterPin("1234"))
        assertTrue(viewModel.uiState.value.guardian.unlocked)
        assertTrue(preferences.guardianPin.value?.let { GuardianPin.matches("1234", it) } == true)

        viewModel.onIntent(GuardianIntent.SetControls(GuardianControls(allowContinuousPlayback = true)))
        viewModel.onIntent(GuardianIntent.Lock)
        // Locked, nothing changes without the PIN.
        viewModel.onIntent(GuardianIntent.SetControls(GuardianControls(breakReminderMinutes = 0)))
        assertEquals(GuardianControls(allowContinuousPlayback = true), preferences.guardianControls.value)

        viewModel.onIntent(GuardianIntent.Open)
        viewModel.onIntent(GuardianIntent.EnterPin("0000"))
        assertEquals(PinError.WRONG, viewModel.uiState.value.guardian.pinError)
        viewModel.onIntent(GuardianIntent.EnterPin("1234"))
        viewModel.onIntent(GuardianIntent.RemovePin)

        assertNull(preferences.guardianPin.value)
        assertEquals(GuardianControls(), preferences.guardianControls.value)
        assertFalse(viewModel.uiState.value.guardian.unlocked)
    }

    @Test
    fun aMinorInBrazilHasNoTelemetryToChoose() = runTest(dispatcher) {
        val viewModel = viewModel()
        childSafety.refresh()
        assertFalse(viewModel.uiState.value.isMinor)

        ageSignal.ageGroup = AgeGroup.MINOR
        childSafety.refresh()

        assertTrue(viewModel.uiState.value.isMinor)
    }

    private val explicitRule = MutableStateFlow(ExplicitRule())

    @Test
    fun underTheEcaDigitalExplicitContentIsOffAndLockedWhateverWasChosen() = runTest(dispatcher) {
        explicitRule.value = ExplicitRule(hidesExplicit = true)
        val viewModel = viewModel()

        assertFalse(viewModel.uiState.value.showExplicit)
        assertTrue(viewModel.uiState.value.explicitLocked)
        assertTrue(preferences.showExplicit.value)

        explicitRule.value = ExplicitRule()
        assertTrue(viewModel.uiState.value.showExplicit)
        assertFalse(viewModel.uiState.value.explicitLocked)
    }

    @Test
    fun aSyncFolderChosenAgainKeepsThisDevicesFileAndStoppingForgetsIt() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.ChooseSyncFolder("bookmark-1", "Sync"))
        val fileName = preferences.syncFolder.value?.fileName
        viewModel.onIntent(SettingsIntent.ChooseSyncFolder("bookmark-2", "Dropbox"))

        assertEquals(SyncFolder("bookmark-2", "Dropbox", fileName!!), preferences.syncFolder.value)
        assertEquals("Dropbox", viewModel.uiState.value.syncFolderName)
        viewModel.onIntent(SettingsIntent.StopSync)
        assertEquals(null, viewModel.uiState.value.syncFolderName)
    }

    @Test
    fun theChosenThemeIsSavedAndShown() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetTheme(ThemeMode.DARK))

        assertEquals(ThemeMode.DARK, preferences.themeMode.value)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun theChosenLanguageIsSavedAndShown() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetLanguage(AppLanguage.SPANISH))

        assertEquals(AppLanguage.SPANISH, preferences.appLanguage.value)
        assertEquals(AppLanguage.SPANISH, viewModel.uiState.value.language)
    }

    @Test
    fun telemetryCanBeTurnedOff() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetTelemetry(false))

        assertFalse(preferences.telemetryEnabled.value)
        assertFalse(viewModel.uiState.value.telemetryEnabled)
    }

    @Test
    fun explicitContentCanBeHidden() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.showExplicit)

        viewModel.onIntent(SettingsIntent.SetShowExplicit(false))

        assertFalse(preferences.showExplicit.value)
        assertFalse(viewModel.uiState.value.showExplicit)
    }

    @Test
    fun wifiOnlyAndTheDownloadLimitAreSaved() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetWifiOnly(true))
        viewModel.onIntent(SettingsIntent.SetDownloadLimit(USED_BYTES * 2))

        assertEquals(true, viewModel.uiState.value.wifiOnlyDownloads)
        assertEquals(USED_BYTES * 2, preferences.downloadLimitBytes.value)

        viewModel.onIntent(SettingsIntent.SetDownloadNext(true))
        assertEquals(true, viewModel.uiState.value.downloadNextOnFinish)

        viewModel.onIntent(SettingsIntent.SetDeleteFinished(true))
        assertEquals(true, preferences.deleteFinishedDownloads.value)
        assertEquals(true, viewModel.uiState.value.deleteFinishedDownloads)

        viewModel.onIntent(SettingsIntent.SetDeleteAfter(14))
        assertEquals(14, viewModel.uiState.value.deleteDownloadsAfterDays)

        viewModel.onIntent(SettingsIntent.SetRefreshInterval(12))
        assertEquals(12, preferences.refreshIntervalHours.value)
        assertEquals(12, viewModel.uiState.value.refreshIntervalHours)

        viewModel.onIntent(SettingsIntent.SetNotifyNewEpisodes(false))
        assertEquals(false, preferences.notifyNewEpisodes.value)
        assertEquals(false, viewModel.uiState.value.notifyNewEpisodes)

        viewModel.onIntent(SettingsIntent.SetSkipSilence(true))
        viewModel.onIntent(SettingsIntent.SetBoostVoice(true))
        assertEquals(true, preferences.skipSilence.value)
        assertEquals(true, viewModel.uiState.value.skipSilence)
        assertEquals(true, viewModel.uiState.value.boostVoice)
    }

    @Test
    fun theSpaceIsReadAgainWhenADownloadEndsOrIsDeleted() = runTest(dispatcher) {
        val viewModel = viewModel()

        downloader.usedBytes = USED_BYTES * 2
        repository.episodes.value = listOf(downloaded())

        assertEquals(if (supportsDownloads) USED_BYTES * 2 else null, viewModel.uiState.value.usedBytes)
    }

    @Test
    fun deletingAllDownloadsAsksFirstThenDeletesEach() = runTest(dispatcher) {
        repository.episodes.value = listOf(downloaded(), downloaded().copy(id = "e2"), downloaded().copy(id = "e3", isDownloaded = false))
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.RequestDeleteAll)
        assertEquals(true, viewModel.uiState.value.isConfirmingDeleteAll)
        viewModel.onIntent(SettingsIntent.DismissDeleteAll)
        assertEquals(emptyList(), downloader.deletedIds)

        viewModel.onIntent(SettingsIntent.RequestDeleteAll)
        viewModel.onIntent(SettingsIntent.ConfirmDeleteAll)

        assertEquals(false, viewModel.uiState.value.isConfirmingDeleteAll)
        assertEquals(listOf("e1", "e2"), downloader.deletedIds)
    }

    @Test
    fun theSpaceTheDownloadsTakeIsShownWhereThereAreDownloads() = runTest(dispatcher) {
        // The Web has no downloads, and no line for their space.
        assertEquals(if (supportsDownloads) USED_BYTES else null, viewModel().uiState.value.usedBytes)
    }

    private fun downloaded() = Episode(
        id = "e1", podcastId = "p", title = "E1", description = null, audioUrl = "a", imageUrl = null, duration = 60,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = true, fileSize = null,
    )

    @Test
    fun importingAnOpmlFileSubscribesAndSaysHowItWent() = runTest(dispatcher) {
        feedSource.result = Result.success(FetchedFeed(podcast("https://new.example.com/feed"), emptyList()))
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.ImportOpml("https://new.example.com/feed\nnot a feed"))

        assertEquals(OpmlStatus.Imported(OpmlImport(added = 1, known = 0, failed = 1)), viewModel.uiState.value.opml)
        assertEquals(listOf("https://new.example.com/feed"), repository.podcasts.value.map { it.feedUrl })
    }

    @Test
    fun aFileThatCannotBeReadImportsNothing() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.ImportOpml(null))

        assertEquals(OpmlStatus.Imported(OpmlImport(0, 0, 0)), viewModel.uiState.value.opml)
    }

    @Test
    fun exportingHandsTheFileToTheScreenOnceAndSaysHowManyWereSaved() = runTest(dispatcher) {
        repository.podcasts.value = listOf(podcast("https://a.example.com/feed"), podcast("https://b.example.com/feed"))
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.ExportOpml)
        assertEquals(setOf("https://a.example.com/feed", "https://b.example.com/feed"), viewModel.uiState.value.opmlToSave?.lines()?.toSet())

        viewModel.onIntent(SettingsIntent.OpmlHandedOver)
        assertEquals(null, viewModel.uiState.value.opmlToSave)
        viewModel.onIntent(SettingsIntent.OpmlSaved(saved = true))
        assertEquals(OpmlStatus.Exported(2), viewModel.uiState.value.opml)
    }

    private fun podcast(feed: String) = Podcast(
        id = feed, title = feed, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = feed, siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )

    private companion object {
        const val USED_BYTES = 52_428_800L
    }

    @Test
    fun aBackupIsHandedToTheSaveDialogAndAnotherFileIsRefusedOnRestore() = runTest(dispatcher) {
        val viewModel = viewModel()
        preferences.setThemeMode(ThemeMode.DARK)

        viewModel.onIntent(SettingsIntent.ExportBackup)
        val text = viewModel.uiState.value.backupToSave.orEmpty()
        assertTrue("\"themeMode\": \"DARK\"" in text, text)
        viewModel.onIntent(SettingsIntent.BackupHandedOver)
        viewModel.onIntent(SettingsIntent.BackupSaved(saved = false))
        assertEquals(OpmlStatus.ExportFailed, viewModel.uiState.value.backup)

        viewModel.onIntent(SettingsIntent.RestoreBackup("<opml/>"))
        assertEquals(OpmlStatus.NotABackup, viewModel.uiState.value.backup)

        preferences.setThemeMode(ThemeMode.LIGHT)
        viewModel.onIntent(SettingsIntent.RestoreBackup(text))
        assertEquals(OpmlStatus.Imported(OpmlImport(0, 0, 0)), viewModel.uiState.value.backup)
        assertEquals(ThemeMode.DARK, preferences.themeMode.value)
    }
}
