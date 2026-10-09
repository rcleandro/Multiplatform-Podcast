package br.com.carvalho.podcast.feature.settings.presentation

import kotlin.test.assertTrue
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.AppLanguage
import br.com.carvalho.podcast.domain.model.ThemeMode
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val preferences = FakePreferencesRepository()
    private val downloader = FakeEpisodeDownloader().apply { usedBytes = USED_BYTES }
    private val repository = FakePodcastRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        SettingsViewModel(
            preferences, downloader, AppDirectories(FakeFileSystem(), "/app".toPath()), repository,
            CoroutineDispatchers(main = dispatcher, io = dispatcher),
        )

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

    private companion object {
        const val USED_BYTES = 52_428_800L
    }
}
