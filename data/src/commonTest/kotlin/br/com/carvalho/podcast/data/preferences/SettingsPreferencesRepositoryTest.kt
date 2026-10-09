package br.com.carvalho.podcast.data.preferences

import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.ThemeMode
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsPreferencesRepositoryTest {
    private val settings = MapSettings()

    @Test
    fun theLibraryStartsAsAGrid() {
        assertEquals(LibraryLayout.GRID, SettingsPreferencesRepository(settings).libraryLayout.value)
    }

    @Test
    fun theChosenLayoutIsKeptForTheNextLaunch() {
        SettingsPreferencesRepository(settings).setLibraryLayout(LibraryLayout.LIST)

        assertEquals(LibraryLayout.LIST, SettingsPreferencesRepository(settings).libraryLayout.value)
    }

    @Test
    fun theFlowEmitsTheNewLayout() {
        val repository = SettingsPreferencesRepository(settings)

        repository.setLibraryLayout(LibraryLayout.LIST)

        assertEquals(LibraryLayout.LIST, repository.libraryLayout.value)
    }

    @Test
    fun theChosenSortIsKeptAndStartsByTitle() {
        assertEquals(LibrarySort.TITLE, SettingsPreferencesRepository(settings).librarySort.value)

        SettingsPreferencesRepository(settings).setLibrarySort(LibrarySort.LATEST_EPISODE)

        assertEquals(LibrarySort.LATEST_EPISODE, SettingsPreferencesRepository(settings).librarySort.value)
    }

    @Test
    fun theThemeFollowsTheSystemUntilChosen() {
        assertEquals(ThemeMode.SYSTEM, SettingsPreferencesRepository(settings).themeMode.value)

        SettingsPreferencesRepository(settings).setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, SettingsPreferencesRepository(settings).themeMode.value)
    }

    @Test
    fun downloadsUseAnyNetworkWithoutLimitUntilChosen() {
        val fresh = SettingsPreferencesRepository(settings)
        assertFalse(fresh.wifiOnlyDownloads.value)
        assertNull(fresh.downloadLimitBytes.value)

        fresh.setWifiOnlyDownloads(true)
        fresh.setDownloadLimitBytes(LIMIT)
        assertTrue(SettingsPreferencesRepository(settings).wifiOnlyDownloads.value)
        assertEquals(LIMIT, SettingsPreferencesRepository(settings).downloadLimitBytes.value)

        SettingsPreferencesRepository(settings).setDownloadLimitBytes(null)
        assertNull(SettingsPreferencesRepository(settings).downloadLimitBytes.value)
    }

    @Test
    fun telemetryStartsOnAndTurningItOffIsKept() {
        assertTrue(SettingsPreferencesRepository(settings).telemetryEnabled.value)

        SettingsPreferencesRepository(settings).setTelemetryEnabled(false)

        assertFalse(SettingsPreferencesRepository(settings).telemetryEnabled.value)
    }

    @Test
    fun anUnknownStoredValueFallsBackToTheGrid() {
        settings.putString("library_layout", "MOSAIC")

        assertEquals(LibraryLayout.GRID, SettingsPreferencesRepository(settings).libraryLayout.value)
    }

    private companion object {
        const val LIMIT = 5_368_709_120L
    }
}
