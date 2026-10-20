package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.AppLanguage
import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.model.SyncFolder
import br.com.carvalho.podcast.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/** Choices the user makes once and expects to find on the next launch (ADR 0006). */
@Suppress("TooManyFunctions") // a value and a setter per preference
interface PreferencesRepository {
    val libraryLayout: StateFlow<LibraryLayout>

    fun setLibraryLayout(layout: LibraryLayout)

    val librarySort: StateFlow<LibrarySort>

    fun setLibrarySort(sort: LibrarySort)

    /** Downloads wait for Wi-Fi on phones and tablets (18.5); off by default. */
    val wifiOnlyDownloads: StateFlow<Boolean>

    fun setWifiOnlyDownloads(enabled: Boolean)

    /** Space the downloads may take, in bytes; null for no limit, the default (18.5). */
    val downloadLimitBytes: StateFlow<Long?>

    fun setDownloadLimitBytes(bytes: Long?)

    /** When an episode ends, download the next one to hear (18.5, part of 18.19); off by default. */
    val downloadNextOnFinish: StateFlow<Boolean>

    fun setDownloadNextOnFinish(enabled: Boolean)

    /** When a downloaded episode ends, delete its file (18.5, part of 18.19); off by default. */
    /** Days a download stays before it is deleted (18.19); 0 keeps them. */
    val deleteDownloadsAfterDays: StateFlow<Int>

    fun setDeleteDownloadsAfterDays(days: Int)

    val deleteFinishedDownloads: StateFlow<Boolean>

    fun setDeleteFinishedDownloads(enabled: Boolean)

    /** Whether discovery shows podcasts marked as explicit (18.5); on by default. */
    val showExplicit: StateFlow<Boolean>

    fun setShowExplicit(enabled: Boolean)

    /** Whether new episodes found in the background say so in a notification (18.18); on by default. */
    val notifyNewEpisodes: StateFlow<Boolean>

    fun setNotifyNewEpisodes(enabled: Boolean)

    /** When an episode ends, the next in the queue starts by itself (26.5); off in Brazil until chosen. */
    val continuousPlayback: StateFlow<Boolean>

    fun setContinuousPlayback(enabled: Boolean)

    /** Hours between looks for new episodes while the app is closed (18.18); 0 never looks. */
    val refreshIntervalHours: StateFlow<Int>

    fun setRefreshIntervalHours(hours: Int)

    /** Silences played through quicker, and voices made louder (18.16); off by default, Android only. */
    val skipSilence: StateFlow<Boolean>

    fun setSkipSilence(enabled: Boolean)

    val boostVoice: StateFlow<Boolean>

    fun setBoostVoice(enabled: Boolean)

    /** On a watch, play only through headphones or a speaker, never the watch's own (19.9f); off by default. */
    val headphonesOnly: StateFlow<Boolean>

    fun setHeadphonesOnly(enabled: Boolean)

    val skipIntervals: StateFlow<SkipIntervals>

    fun setSkipIntervals(intervals: SkipIntervals)

    val appLanguage: StateFlow<AppLanguage>

    fun setAppLanguage(language: AppLanguage)

    val themeMode: StateFlow<ThemeMode>

    fun setThemeMode(mode: ThemeMode)

    /** Analytics and crash reports; on by default, the user can turn them off (16.4). */
    val telemetryEnabled: StateFlow<Boolean>

    fun setTelemetryEnabled(enabled: Boolean)

    /** The folder the library syncs through (19.12); null for none, the default. Not backed up: it is this device's. */
    val syncFolder: StateFlow<SyncFolder?>

    fun setSyncFolder(folder: SyncFolder?)
}
