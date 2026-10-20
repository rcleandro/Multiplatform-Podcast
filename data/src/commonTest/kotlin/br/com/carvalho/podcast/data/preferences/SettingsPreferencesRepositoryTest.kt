package br.com.carvalho.podcast.data.preferences

import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.model.SyncFolder
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
    fun theSyncFolderStartsUnsetAndTheChoiceIsKeptUntilTurnedOff() {
        val folder = SyncFolder(bookmark = "content://tree/sync", name = "Sync", fileName = "podcast-sync-1.json")
        assertNull(SettingsPreferencesRepository(settings).syncFolder.value)

        SettingsPreferencesRepository(settings).setSyncFolder(folder)
        assertEquals(folder, SettingsPreferencesRepository(settings).syncFolder.value)

        SettingsPreferencesRepository(settings).setSyncFolder(null)
        assertNull(SettingsPreferencesRepository(settings).syncFolder.value)
    }

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
        // Background refresh (18.18): every 6 hours until chosen; a value no longer offered goes back to that.
        assertEquals(6, fresh.refreshIntervalHours.value)
        fresh.setRefreshIntervalHours(0)
        assertEquals(0, SettingsPreferencesRepository(settings).refreshIntervalHours.value)
        settings.putInt("refresh_interval_hours", 5)
        assertEquals(6, SettingsPreferencesRepository(settings).refreshIntervalHours.value)
        // Old downloads (18.19): kept until chosen.
        assertEquals(0, fresh.deleteDownloadsAfterDays.value)
        fresh.setDeleteDownloadsAfterDays(30)
        assertEquals(30, SettingsPreferencesRepository(settings).deleteDownloadsAfterDays.value)
        assertTrue(fresh.notifyNewEpisodes.value)
        fresh.setNotifyNewEpisodes(false)
        assertFalse(SettingsPreferencesRepository(settings).notifyNewEpisodes.value)
        // Speech (18.16): off until chosen, then kept.
        assertFalse(fresh.skipSilence.value || fresh.boostVoice.value)
        fresh.setSkipSilence(true)
        fresh.setBoostVoice(true)
        assertTrue(SettingsPreferencesRepository(settings).skipSilence.value)
        assertTrue(SettingsPreferencesRepository(settings).boostVoice.value)
        // Headphones only (19.9f): off until chosen, then kept.
        assertFalse(fresh.headphonesOnly.value)
        fresh.setHeadphonesOnly(true)
        assertTrue(SettingsPreferencesRepository(settings).headphonesOnly.value)

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
    fun inBrazilWhatNobodyChoseStartsOffAndWhatWasChosenStays() {
        val brazil = SettingsPreferencesRepository(settings, protectiveDefaults = true)
        assertFalse(brazil.telemetryEnabled.value)
        assertFalse(brazil.notifyNewEpisodes.value)

        SettingsPreferencesRepository(settings).setTelemetryEnabled(true)

        assertTrue(SettingsPreferencesRepository(settings, protectiveDefaults = true).telemetryEnabled.value)
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
