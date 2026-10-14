package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.AppLanguage
import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.RefreshInterval
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow

class FakePreferencesRepository : PreferencesRepository {
    override val libraryLayout = MutableStateFlow(LibraryLayout.GRID)

    override fun setLibraryLayout(layout: LibraryLayout) {
        libraryLayout.value = layout
    }

    override val librarySort = MutableStateFlow(LibrarySort.TITLE)

    override fun setLibrarySort(sort: LibrarySort) {
        librarySort.value = sort
    }

    override val wifiOnlyDownloads = MutableStateFlow(false)

    override fun setWifiOnlyDownloads(enabled: Boolean) {
        wifiOnlyDownloads.value = enabled
    }

    override val downloadLimitBytes = MutableStateFlow<Long?>(null)

    override fun setDownloadLimitBytes(bytes: Long?) {
        downloadLimitBytes.value = bytes
    }

    override val showExplicit = MutableStateFlow(true)

    override fun setShowExplicit(enabled: Boolean) {
        showExplicit.value = enabled
    }

    override val downloadNextOnFinish = MutableStateFlow(false)

    override fun setDownloadNextOnFinish(enabled: Boolean) {
        downloadNextOnFinish.value = enabled
    }

    override val deleteFinishedDownloads = MutableStateFlow(false)

    override fun setDeleteFinishedDownloads(enabled: Boolean) {
        deleteFinishedDownloads.value = enabled
    }

    override val refreshIntervalHours = MutableStateFlow(RefreshInterval.DEFAULT_HOURS)

    override fun setRefreshIntervalHours(hours: Int) {
        refreshIntervalHours.value = hours
    }

    override val skipSilence = MutableStateFlow(false)

    override fun setSkipSilence(enabled: Boolean) {
        skipSilence.value = enabled
    }

    override val boostVoice = MutableStateFlow(false)

    override fun setBoostVoice(enabled: Boolean) {
        boostVoice.value = enabled
    }

    override val skipIntervals = MutableStateFlow(SkipIntervals())

    override fun setSkipIntervals(intervals: SkipIntervals) {
        skipIntervals.value = intervals
    }

    override val appLanguage = MutableStateFlow(AppLanguage.SYSTEM)

    override fun setAppLanguage(language: AppLanguage) {
        appLanguage.value = language
    }

    override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)

    override fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    override val telemetryEnabled = MutableStateFlow(true)

    override fun setTelemetryEnabled(enabled: Boolean) {
        telemetryEnabled.value = enabled
    }
}
