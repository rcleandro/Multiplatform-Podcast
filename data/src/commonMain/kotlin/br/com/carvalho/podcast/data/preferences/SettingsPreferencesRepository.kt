package br.com.carvalho.podcast.data.preferences

import br.com.carvalho.podcast.domain.model.AppLanguage
import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.model.ThemeMode
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
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
private const val SKIP_BACKWARD_SECONDS = "skip_backward_seconds"
private const val APP_LANGUAGE = "app_language"
private const val SKIP_FORWARD_SECONDS = "skip_forward_seconds"

/**
 * [PreferencesRepository] over multiplatform-settings (ADR 0006). Not every platform's Settings can be observed, so the
 * flows live here and this class is the only writer (a Koin singleton).
 */
class SettingsPreferencesRepository(private val settings: Settings) : PreferencesRepository {
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

    private val deleteFinished = MutableStateFlow(settings.getBoolean(DELETE_FINISHED_DOWNLOADS, defaultValue = false))
    override val deleteFinishedDownloads: StateFlow<Boolean> = deleteFinished.asStateFlow()

    override fun setDeleteFinishedDownloads(enabled: Boolean) {
        settings.putBoolean(DELETE_FINISHED_DOWNLOADS, enabled)
        deleteFinished.value = enabled
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

    private val telemetry = MutableStateFlow(settings.getBoolean(TELEMETRY_ENABLED, defaultValue = true))
    override val telemetryEnabled: StateFlow<Boolean> = telemetry.asStateFlow()

    override fun setTelemetryEnabled(enabled: Boolean) {
        settings.putBoolean(TELEMETRY_ENABLED, enabled)
        telemetry.value = enabled
    }
}

// A value written by another app version, or by hand, falls back to the default instead of crashing.
private inline fun <reified T : Enum<T>> Settings.enumOrDefault(key: String, default: T): T =
    getStringOrNull(key)?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
