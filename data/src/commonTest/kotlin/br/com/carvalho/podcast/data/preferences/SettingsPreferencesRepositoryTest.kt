package br.com.carvalho.podcast.data.preferences

import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.SkipIntervals
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
        assertFalse(fresh.downloadNextOnFinish.value)
        fresh.setDownloadNextOnFinish(true)
        assertTrue(SettingsPreferencesRepository(settings).downloadNextOnFinish.value)
        assertFalse(fresh.deleteFinishedDownloads.value)
        fresh.setDeleteFinishedDownloads(true)
        assertTrue(SettingsPreferencesRepository(settings).deleteFinishedDownloads.value)
        // Speech (18.16): off until chosen, then kept.
        assertFalse(fresh.skipSilence.value || fresh.boostVoice.value)
        fresh.setSkipSilence(true)
        fresh.setBoostVoice(true)
        assertTrue(SettingsPreferencesRepository(settings).skipSilence.value)
        assertTrue(SettingsPreferencesRepository(settings).boostVoice.value)

        fresh.setWifiOnlyDownloads(true)
        fresh.setDownloadLimitBytes(LIMIT)
        assertTrue(SettingsPreferencesRepository(settings).wifiOnlyDownloads.value)
        assertEquals(LIMIT, SettingsPreferencesRepository(settings).downloadLimitBytes.value)

        SettingsPreferencesRepository(settings).setDownloadLimitBytes(null)
        assertNull(SettingsPreferencesRepository(settings).downloadLimitBytes.value)
    }

    @Test
    fun theJumpsStartAtTenBackAndThirtyForwardAndTheChoiceIsKept() {
        assertEquals(SkipIntervals(10, 30), SettingsPreferencesRepository(settings).skipIntervals.value)

        SettingsPreferencesRepository(settings).setSkipIntervals(SkipIntervals(5, 5))

        assertEquals(SkipIntervals(5, 5), SettingsPreferencesRepository(settings).skipIntervals.value)
    }

    @Test
    fun aJumpNoLongerOfferedGoesBackToTheDefault() {
        settings.putInt("skip_forward_seconds", 15)

        assertEquals(30, SettingsPreferencesRepository(settings).skipIntervals.value.forwardSeconds)
    }

    @Test
    fun explicitContentStartsShownAndHidingItIsKept() {
        assertTrue(SettingsPreferencesRepository(settings).showExplicit.value)

        SettingsPreferencesRepository(settings).setShowExplicit(false)

        assertFalse(SettingsPreferencesRepository(settings).showExplicit.value)
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
