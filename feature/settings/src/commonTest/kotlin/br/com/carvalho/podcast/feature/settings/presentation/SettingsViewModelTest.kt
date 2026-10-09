package br.com.carvalho.podcast.feature.settings.presentation

import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
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

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        SettingsViewModel(preferences, downloader, CoroutineDispatchers(main = dispatcher, io = dispatcher))

    @Test
    fun theChosenThemeIsSavedAndShown() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetTheme(ThemeMode.DARK))

        assertEquals(ThemeMode.DARK, preferences.themeMode.value)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun telemetryCanBeTurnedOff() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetTelemetry(false))

        assertFalse(preferences.telemetryEnabled.value)
        assertFalse(viewModel.uiState.value.telemetryEnabled)
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
    }

    @Test
    fun theSpaceTheDownloadsTakeIsShownWhereThereAreDownloads() = runTest(dispatcher) {
        // The Web has no downloads, and no line for their space.
        assertEquals(if (supportsDownloads) USED_BYTES else null, viewModel().uiState.value.usedBytes)
    }

    private companion object {
        const val USED_BYTES = 52_428_800L
    }
}
