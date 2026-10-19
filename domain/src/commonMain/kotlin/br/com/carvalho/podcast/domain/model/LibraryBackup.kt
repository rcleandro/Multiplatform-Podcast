package br.com.carvalho.podcast.domain.model

import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import kotlinx.serialization.Serializable

/**
 * Everything the user built in the app, in one file (19.11): the podcasts in library order with their own
 * settings, what was heard, the queue, the bookmarks, the smart playlists and the preferences. Feeds and downloads
 * are not in it: the feeds are read again on restore, and downloads can be made again.
 */
@Serializable
data class LibraryBackup(
    /** Tells this app's backups from any other JSON file. */
    val format: String = FORMAT,
    val version: Int = VERSION,
    val podcasts: List<PodcastBackup> = emptyList(),
    val episodes: List<EpisodeBackup> = emptyList(),
    val queue: List<EpisodeRef> = emptyList(),
    val bookmarks: List<BookmarkBackup> = emptyList(),
    /** The smart playlists (19.4); restored ones keep their filters and get new ids. */
    val playlists: List<SmartPlaylist> = emptyList(),
    val preferences: PreferencesBackup = PreferencesBackup(),
) {
    companion object {
        const val FORMAT = "br.com.carvalho.podcast.backup"
        const val VERSION = 1
    }
}

@Serializable
data class PodcastBackup(
    val feedUrl: String,
    val title: String,
    val oldestFirst: Boolean? = null,
    val playback: PodcastPlayback = PodcastPlayback(),
    val notifyNewEpisodes: Boolean = true,
    val autoDownloadCount: Int = 0,
)

/**
 * An episode as another install can find it: by its id, which comes from the feed's address and the item's guid, or
 * by its audio when the feed was added under another address.
 */
@Serializable
data class EpisodeRef(val id: String, val feedUrl: String, val audioUrl: String)

@Serializable
data class EpisodeBackup(
    val episode: EpisodeRef,
    val isPlayed: Boolean = false,
    val positionMs: Long = 0,
    val isSaved: Boolean = false,
    val lastPlayedAt: Long? = null,
)

@Serializable
data class BookmarkBackup(val episode: EpisodeRef, val positionMs: Long, val note: String, val createdAt: Long)

/** Every choice in Settings and the library's look; restoring sets them all. */
@Serializable
data class PreferencesBackup(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val libraryLayout: LibraryLayout = LibraryLayout.GRID,
    val librarySort: LibrarySort = LibrarySort.TITLE,
    val skipIntervals: SkipIntervals = SkipIntervals(),
    val skipSilence: Boolean = false,
    val boostVoice: Boolean = false,
    val headphonesOnly: Boolean = false,
    val wifiOnlyDownloads: Boolean = false,
    val downloadLimitBytes: Long? = null,
    val downloadNextOnFinish: Boolean = false,
    val deleteFinishedDownloads: Boolean = false,
    val deleteDownloadsAfterDays: Int = 0,
    val showExplicit: Boolean = true,
    val notifyNewEpisodes: Boolean = true,
    val refreshIntervalHours: Int = RefreshInterval.DEFAULT_HOURS,
    val telemetryEnabled: Boolean = true,
) {
    fun applyTo(preferences: PreferencesRepository) {
        preferences.setThemeMode(themeMode)
        preferences.setAppLanguage(appLanguage)
        preferences.setLibraryLayout(libraryLayout)
        preferences.setLibrarySort(librarySort)
        preferences.setSkipIntervals(skipIntervals)
        preferences.setSkipSilence(skipSilence)
        preferences.setBoostVoice(boostVoice)
        preferences.setHeadphonesOnly(headphonesOnly)
        preferences.setWifiOnlyDownloads(wifiOnlyDownloads)
        preferences.setDownloadLimitBytes(downloadLimitBytes)
        preferences.setDownloadNextOnFinish(downloadNextOnFinish)
        preferences.setDeleteFinishedDownloads(deleteFinishedDownloads)
        preferences.setDeleteDownloadsAfterDays(deleteDownloadsAfterDays)
        preferences.setShowExplicit(showExplicit)
        preferences.setNotifyNewEpisodes(notifyNewEpisodes)
        preferences.setRefreshIntervalHours(refreshIntervalHours)
        preferences.setTelemetryEnabled(telemetryEnabled)
    }

    companion object {
        fun of(preferences: PreferencesRepository) = let {
            PreferencesBackup(
                themeMode = preferences.themeMode.value,
                appLanguage = preferences.appLanguage.value,
                libraryLayout = preferences.libraryLayout.value,
                librarySort = preferences.librarySort.value,
                skipIntervals = preferences.skipIntervals.value,
                skipSilence = preferences.skipSilence.value,
                boostVoice = preferences.boostVoice.value,
                headphonesOnly = preferences.headphonesOnly.value,
                wifiOnlyDownloads = preferences.wifiOnlyDownloads.value,
                downloadLimitBytes = preferences.downloadLimitBytes.value,
                downloadNextOnFinish = preferences.downloadNextOnFinish.value,
                deleteFinishedDownloads = preferences.deleteFinishedDownloads.value,
                deleteDownloadsAfterDays = preferences.deleteDownloadsAfterDays.value,
                showExplicit = preferences.showExplicit.value,
                notifyNewEpisodes = preferences.notifyNewEpisodes.value,
                refreshIntervalHours = preferences.refreshIntervalHours.value,
                telemetryEnabled = preferences.telemetryEnabled.value,
            )
        }
    }
}
