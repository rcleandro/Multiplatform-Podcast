package br.com.carvalho.podcast.data.preferences

import br.com.carvalho.podcast.domain.model.AppLanguage
import br.com.carvalho.podcast.domain.model.GuardianControls
import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.RefreshInterval
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.model.SyncFolder
import br.com.carvalho.podcast.domain.model.ThemeMode
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.DeleteOldDownloads
import br.com.carvalho.podcast.domain.usecase.FolderSyncUseCase
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val LIBRARY_LAYOUT = "library_layout"
private const val LIBRARY_SORT = "library_sort"
private const val TELEMETRY_ENABLED = "telemetry_enabled"
private const val THEME_MODE = "theme_mode"
private const val WIFI_ONLY_DOWNLOADS = "wifi_only_downloads"
private const val DOWNLOAD_LIMIT_BYTES = "download_limit_bytes"
private const val DOWNLOAD_NEXT_ON_FINISH = "download_next_on_finish"
private const val DELETE_FINISHED_DOWNLOADS = "delete_finished_downloads"
private const val DELETE_DOWNLOADS_AFTER_DAYS = "delete_downloads_after_days"
private const val SKIP_SILENCE = "skip_silence"
private const val HEADPHONES_ONLY = "headphones_only"
private const val REFRESH_INTERVAL_HOURS = "refresh_interval_hours"
private const val NOTIFY_NEW_EPISODES = "notify_new_episodes"
private const val CONTINUOUS_PLAYBACK = "continuous_playback"
private const val BOOST_VOICE = "boost_voice"
private const val SHOW_EXPLICIT = "show_explicit"
private const val SKIP_BACKWARD_SECONDS = "skip_backward_seconds"
private const val APP_LANGUAGE = "app_language"
private const val SKIP_FORWARD_SECONDS = "skip_forward_seconds"
private const val SYNC_FOLDER_BOOKMARK = "sync_folder_bookmark"
private const val SYNC_FOLDER_NAME = "sync_folder_name"
private const val SYNC_FOLDER_FILE = "sync_folder_file"
private const val GUARDIAN_PIN = "guardian_pin"
private const val HIDDEN_PODCASTS = "hidden_podcasts"
private const val HIDDEN_EPISODES = "hidden_episodes"
private const val GUARDIAN_CONTINUOUS = "guardian_allow_continuous"
private const val GUARDIAN_NOTIFICATIONS = "guardian_allow_notifications"
private const val GUARDIAN_BREAK_MINUTES = "guardian_break_minutes"

/**
 * [PreferencesRepository] over multiplatform-settings (ADR 0006). Not every platform's Settings can be observed, so the
 * flows live here and this class is the only writer (a Koin singleton). [protectiveDefaults] in Brazil, for the ECA
 * Digital (26.4, ADR 0012): what nobody chose yet starts off (telemetry, new-episode notifications, the queue moving
 * on); what was chosen stays.
 */
@Suppress("TooManyFunctions") // implements PreferencesRepository
class SettingsPreferencesRepository(
    private val settings: Settings,
    private val protectiveDefaults: Boolean = false,
) : PreferencesRepository {
    private val layout = MutableStateFlow(settings.enumOrDefault(LIBRARY_LAYOUT, LibraryLayout.GRID))
    override val libraryLayout: StateFlow<LibraryLayout> = layout.asStateFlow()

    override fun setLibraryLayout(layout: LibraryLayout) {
        settings.putString(LIBRARY_LAYOUT, layout.name)
        this.layout.value = layout
    }

    private val sort = MutableStateFlow(settings.enumOrDefault(LIBRARY_SORT, LibrarySort.TITLE))
    override val librarySort: StateFlow<LibrarySort> = sort.asStateFlow()

    override fun setLibrarySort(sort: LibrarySort) {
        settings.putString(LIBRARY_SORT, sort.name)
        this.sort.value = sort
    }

    private val wifiOnly = MutableStateFlow(settings.getBoolean(WIFI_ONLY_DOWNLOADS, defaultValue = false))
    override val wifiOnlyDownloads: StateFlow<Boolean> = wifiOnly.asStateFlow()

    override fun setWifiOnlyDownloads(enabled: Boolean) {
        settings.putBoolean(WIFI_ONLY_DOWNLOADS, enabled)
        wifiOnly.value = enabled
    }

    private val downloadLimit = MutableStateFlow(settings.getLongOrNull(DOWNLOAD_LIMIT_BYTES))
    override val downloadLimitBytes: StateFlow<Long?> = downloadLimit.asStateFlow()

    override fun setDownloadLimitBytes(bytes: Long?) {
        if (bytes == null) settings.remove(DOWNLOAD_LIMIT_BYTES) else settings.putLong(DOWNLOAD_LIMIT_BYTES, bytes)
        downloadLimit.value = bytes
    }

    private val downloadNext = MutableStateFlow(settings.getBoolean(DOWNLOAD_NEXT_ON_FINISH, defaultValue = false))
    override val downloadNextOnFinish: StateFlow<Boolean> = downloadNext.asStateFlow()

    override fun setDownloadNextOnFinish(enabled: Boolean) {
        settings.putBoolean(DOWNLOAD_NEXT_ON_FINISH, enabled)
        downloadNext.value = enabled
    }

    private val continuous =
        MutableStateFlow(settings.getBoolean(CONTINUOUS_PLAYBACK, defaultValue = !protectiveDefaults))
    override val continuousPlayback: StateFlow<Boolean> = continuous.asStateFlow()

    override fun setContinuousPlayback(enabled: Boolean) {
        settings.putBoolean(CONTINUOUS_PLAYBACK, enabled)
        continuous.value = enabled
    }

    private val notify = MutableStateFlow(settings.getBoolean(NOTIFY_NEW_EPISODES, defaultValue = !protectiveDefaults))
    override val notifyNewEpisodes: StateFlow<Boolean> = notify.asStateFlow()

    override fun setNotifyNewEpisodes(enabled: Boolean) {
        settings.putBoolean(NOTIFY_NEW_EPISODES, enabled)
        notify.value = enabled
    }

    private val refreshHours = MutableStateFlow(
        settings.getInt(REFRESH_INTERVAL_HOURS, RefreshInterval.DEFAULT_HOURS)
            .takeIf { it in RefreshInterval.CHOICES } ?: RefreshInterval.DEFAULT_HOURS
    )
    override val refreshIntervalHours: StateFlow<Int> = refreshHours.asStateFlow()

    override fun setRefreshIntervalHours(hours: Int) {
        settings.putInt(REFRESH_INTERVAL_HOURS, hours)
        refreshHours.value = hours
    }

    private val silence = MutableStateFlow(settings.getBoolean(SKIP_SILENCE, defaultValue = false))
    override val skipSilence: StateFlow<Boolean> = silence.asStateFlow()

    override fun setSkipSilence(enabled: Boolean) {
        settings.putBoolean(SKIP_SILENCE, enabled)
        silence.value = enabled
    }

    private val voice = MutableStateFlow(settings.getBoolean(BOOST_VOICE, defaultValue = false))
    override val boostVoice: StateFlow<Boolean> = voice.asStateFlow()

    override fun setBoostVoice(enabled: Boolean) {
        settings.putBoolean(BOOST_VOICE, enabled)
        voice.value = enabled
    }

    private val headphones = MutableStateFlow(settings.getBoolean(HEADPHONES_ONLY, defaultValue = false))
    override val headphonesOnly: StateFlow<Boolean> = headphones.asStateFlow()

    override fun setHeadphonesOnly(enabled: Boolean) {
        settings.putBoolean(HEADPHONES_ONLY, enabled)
        headphones.value = enabled
    }

    private val deleteAfter = MutableStateFlow(
        settings.getInt(DELETE_DOWNLOADS_AFTER_DAYS, 0).takeIf { it in DeleteOldDownloads.CHOICES } ?: 0
    )
    override val deleteDownloadsAfterDays: StateFlow<Int> = deleteAfter.asStateFlow()

    override fun setDeleteDownloadsAfterDays(days: Int) {
        settings.putInt(DELETE_DOWNLOADS_AFTER_DAYS, days)
        deleteAfter.value = days
    }

    private val deleteFinished = MutableStateFlow(settings.getBoolean(DELETE_FINISHED_DOWNLOADS, defaultValue = false))
    override val deleteFinishedDownloads: StateFlow<Boolean> = deleteFinished.asStateFlow()

    override fun setDeleteFinishedDownloads(enabled: Boolean) {
        settings.putBoolean(DELETE_FINISHED_DOWNLOADS, enabled)
        deleteFinished.value = enabled
    }

    private val explicit = MutableStateFlow(settings.getBoolean(SHOW_EXPLICIT, defaultValue = true))
    override val showExplicit: StateFlow<Boolean> = explicit.asStateFlow()

    override fun setShowExplicit(enabled: Boolean) {
        settings.putBoolean(SHOW_EXPLICIT, enabled)
        explicit.value = enabled
    }

    private val skips = MutableStateFlow(
        SkipIntervals().let { default ->
            // A jump no longer offered (an older version had 15 s) goes back to the default.
            fun offered(key: String, fallback: Int) =
                settings.getInt(key, fallback).takeIf { it in SkipIntervals.CHOICES } ?: fallback
            SkipIntervals(
                backwardSeconds = offered(SKIP_BACKWARD_SECONDS, default.backwardSeconds),
                forwardSeconds = offered(SKIP_FORWARD_SECONDS, default.forwardSeconds),
            )
        }
    )
    override val skipIntervals: StateFlow<SkipIntervals> = skips.asStateFlow()

    override fun setSkipIntervals(intervals: SkipIntervals) {
        settings.putInt(SKIP_BACKWARD_SECONDS, intervals.backwardSeconds)
        settings.putInt(SKIP_FORWARD_SECONDS, intervals.forwardSeconds)
        skips.value = intervals
    }

    private val language = MutableStateFlow(settings.enumOrDefault(APP_LANGUAGE, AppLanguage.SYSTEM))
    override val appLanguage: StateFlow<AppLanguage> = language.asStateFlow()

    override fun setAppLanguage(language: AppLanguage) {
        settings.putString(APP_LANGUAGE, language.name)
        this.language.value = language
    }

    private val theme = MutableStateFlow(settings.enumOrDefault(THEME_MODE, ThemeMode.SYSTEM))
    override val themeMode: StateFlow<ThemeMode> = theme.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        settings.putString(THEME_MODE, mode.name)
        theme.value = mode
    }

    private val telemetry = MutableStateFlow(settings.getBoolean(TELEMETRY_ENABLED, defaultValue = !protectiveDefaults))
    override val telemetryEnabled: StateFlow<Boolean> = telemetry.asStateFlow()

    override fun setTelemetryEnabled(enabled: Boolean) {
        settings.putBoolean(TELEMETRY_ENABLED, enabled)
        telemetry.value = enabled
    }

    private val folder = MutableStateFlow(
        settings.getStringOrNull(SYNC_FOLDER_BOOKMARK)?.let { bookmark ->
            SyncFolder(
                bookmark,
                settings.getString(SYNC_FOLDER_NAME, defaultValue = ""),
                settings.getString(SYNC_FOLDER_FILE, defaultValue = FolderSyncUseCase.newFileName()),
            )
        }
    )
    override val syncFolder: StateFlow<SyncFolder?> = folder.asStateFlow()

    override fun setSyncFolder(folder: SyncFolder?) {
        if (folder == null) {
            listOf(SYNC_FOLDER_BOOKMARK, SYNC_FOLDER_NAME, SYNC_FOLDER_FILE).forEach(settings::remove)
        } else {
            settings.putString(SYNC_FOLDER_BOOKMARK, folder.bookmark)
            settings.putString(SYNC_FOLDER_NAME, folder.name)
            settings.putString(SYNC_FOLDER_FILE, folder.fileName)
        }
        this.folder.value = folder
    }

    private val pin = MutableStateFlow(settings.getStringOrNull(GUARDIAN_PIN))
    override val guardianPin: StateFlow<String?> = pin.asStateFlow()

    override fun setGuardianPin(hash: String?) {
        if (hash == null) settings.remove(GUARDIAN_PIN) else settings.putString(GUARDIAN_PIN, hash)
        pin.value = hash
    }

    private val guardian = MutableStateFlow(
        GuardianControls(
            allowContinuousPlayback = settings.getBoolean(GUARDIAN_CONTINUOUS, defaultValue = false),
            allowNewEpisodeNotifications = settings.getBoolean(GUARDIAN_NOTIFICATIONS, defaultValue = false),
            breakReminderMinutes = settings.getInt(GUARDIAN_BREAK_MINUTES, GuardianControls.DEFAULT_BREAK_MINUTES),
        )
    )
    override val guardianControls: StateFlow<GuardianControls> = guardian.asStateFlow()

    override fun setGuardianControls(controls: GuardianControls) {
        settings.putBoolean(GUARDIAN_CONTINUOUS, controls.allowContinuousPlayback)
        settings.putBoolean(GUARDIAN_NOTIFICATIONS, controls.allowNewEpisodeNotifications)
        settings.putInt(GUARDIAN_BREAK_MINUTES, controls.breakReminderMinutes)
        guardian.value = controls
    }

    private val podcastsHidden = MutableStateFlow(settings.idSet(HIDDEN_PODCASTS))
    override val hiddenPodcasts: StateFlow<Set<String>> = podcastsHidden.asStateFlow()
    private val episodesHidden = MutableStateFlow(settings.idSet(HIDDEN_EPISODES))
    override val hiddenEpisodes: StateFlow<Set<String>> = episodesHidden.asStateFlow()

    override fun hidePodcast(id: String) = hide(HIDDEN_PODCASTS, podcastsHidden, id)

    override fun hideEpisode(id: String) = hide(HIDDEN_EPISODES, episodesHidden, id)

    private fun hide(key: String, ids: MutableStateFlow<Set<String>>, id: String) {
        ids.value += id
        settings.putString(key, ids.value.joinToString(ID_SEPARATOR))
    }
}

// Ids are feed addresses and hashes: neither holds a line break.
private const val ID_SEPARATOR = "\n"

private fun Settings.idSet(key: String): Set<String> =
    getStringOrNull(key)?.split(ID_SEPARATOR)?.filter { it.isNotEmpty() }?.toSet().orEmpty()

// A value written by another app version, or by hand, falls back to the default instead of crashing.
private inline fun <reified T : Enum<T>> Settings.enumOrDefault(key: String, default: T): T =
    getStringOrNull(key)?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
