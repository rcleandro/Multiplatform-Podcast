package br.com.carvalho.podcast.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.core.util.DiskSpace
import br.com.carvalho.podcast.core.util.diskSpace
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.AppLanguage
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.model.ThemeMode
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Settings (18.5): the choices that change how the app works, each saved at once. */
class SettingsViewModel(
    private val preferences: PreferencesRepository,
    private val downloader: EpisodeDownloader,
    private val directories: AppDirectories,
    private val repository: PodcastRepository,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {
    private val usedBytes = MutableStateFlow<Long?>(null)
    private val disk = MutableStateFlow<DiskSpace?>(null)
    private val confirmingDeleteAll = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(preferences.themeMode, preferences.skipIntervals, preferences.appLanguage, ::Triple),
        preferences.telemetryEnabled,
        preferences.wifiOnlyDownloads,
        combine(
            preferences.downloadLimitBytes,
            preferences.downloadNextOnFinish,
            preferences.deleteFinishedDownloads,
            ::Triple
        ),
        combine(usedBytes, disk, confirmingDeleteAll, ::Triple),
    ) { (theme, skips, language), telemetry, wifiOnly, downloads, (bytes, space, confirming) ->
        val (limit, downloadNext, deleteFinished) = downloads
        SettingsUiState(
            themeMode = theme,
            language = language,
            telemetryEnabled = telemetry,
            usedBytes = bytes,
            wifiOnlyDownloads = wifiOnly,
            downloadLimitBytes = limit,
            downloadNextOnFinish = downloadNext,
            disk = space,
            deleteFinishedDownloads = deleteFinished,
            isConfirmingDeleteAll = confirming,
            skipIntervals = skips,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    init {
        // The downloaded list changes when a download ends or is deleted, which is when the space changes too; the
        // screen stays alive while the app runs, so reading it once would show the space at the first visit.
        if (supportsDownloads) {
            repository.getDownloadedEpisodes()
                .onEach {
                    usedBytes.value = downloader.usedBytes()
                    disk.value = diskSpace(directories.baseDir.toString())
                }
                .flowOn(dispatchers.io)
                .launchIn(viewModelScope)
        }
    }

    /** Each through the downloader, as "Delete download" does; the space is read again as the list empties. */
    private fun deleteAllDownloads() {
        confirmingDeleteAll.value = false
        viewModelScope.launch(dispatchers.io) {
            repository.getDownloadedEpisodes().first().forEach { downloader.delete(it.id) }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetTheme -> preferences.setThemeMode(intent.mode)
            is SettingsIntent.SetLanguage -> preferences.setAppLanguage(intent.language)
            is SettingsIntent.SetTelemetry -> preferences.setTelemetryEnabled(intent.enabled)
            is SettingsIntent.SetWifiOnly -> preferences.setWifiOnlyDownloads(intent.enabled)
            is SettingsIntent.SetDownloadLimit -> preferences.setDownloadLimitBytes(intent.bytes)
            is SettingsIntent.SetDownloadNext -> preferences.setDownloadNextOnFinish(intent.enabled)
            is SettingsIntent.SetDeleteFinished -> preferences.setDeleteFinishedDownloads(intent.enabled)
            is SettingsIntent.SetSkipIntervals -> preferences.setSkipIntervals(intent.intervals)
            SettingsIntent.RequestDeleteAll -> confirmingDeleteAll.value = true
            SettingsIntent.DismissDeleteAll -> confirmingDeleteAll.value = false
            SettingsIntent.ConfirmDeleteAll -> deleteAllDownloads()
        }
    }
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val telemetryEnabled: Boolean = true,
    /** Disk space the downloads take; null while it is measured, and where there are no downloads (Web). */
    val usedBytes: Long? = null,
    val wifiOnlyDownloads: Boolean = false,
    /** Null for no limit. */
    val downloadLimitBytes: Long? = null,
    val downloadNextOnFinish: Boolean = false,
    /** The disk the downloads live on; null where it is not known. */
    val disk: DiskSpace? = null,
    val deleteFinishedDownloads: Boolean = false,
    /** "Delete all downloads" waits for this confirmation. */
    val isConfirmingDeleteAll: Boolean = false,
    val skipIntervals: SkipIntervals = SkipIntervals(),
) {
    /** The limits the slider stops at, ending with no limit (null). */
    val limitStops: List<Long?> get() = limitStops(disk?.let { it.freeBytes + (usedBytes ?: 0) }, downloadLimitBytes)
}

private const val MEGABYTE = 1024L * 1024
private const val GIGABYTE = 1024L * MEGABYTE

/** The first stop, for a nearly full phone. */
private const val SMALLEST_STOP = 512 * MEGABYTE

/** Up to each size in gigabytes, the step between stops: finer where a gigabyte matters, coarser beyond. */
@Suppress("MagicNumber") // the sizes are the choices themselves
private val STEPS_UP_TO = listOf(10L to 1L, 50L to 5L, 100L to 10L, 500L to 50L, Long.MAX_VALUE to 100L)

/** When the disk is not known, stops up to this size. */
private const val UNKNOWN_CAPACITY = 20 * GIGABYTE

/**
 * Stops from half a gigabyte up to what the downloads could take ([capacity]: free space plus what they already take),
 * finer for small sizes and coarser for large ones, the [current] limit if it is not one of them, and no limit at the
 * end.
 */
internal fun limitStops(capacity: Long?, current: Long?): List<Long?> {
    val top = capacity ?: UNKNOWN_CAPACITY
    val sizes = mutableListOf(SMALLEST_STOP)
    var gigabytes = 1L
    while (gigabytes * GIGABYTE < top) {
        sizes += gigabytes * GIGABYTE
        gigabytes += STEPS_UP_TO.first { gigabytes < it.first }.second
    }
    return (sizes + listOfNotNull(current)).distinct().sorted() + null
}

sealed interface SettingsIntent {
    data class SetTheme(val mode: ThemeMode) : SettingsIntent
    data class SetLanguage(val language: AppLanguage) : SettingsIntent
    data class SetTelemetry(val enabled: Boolean) : SettingsIntent
    data class SetWifiOnly(val enabled: Boolean) : SettingsIntent
    data class SetDownloadLimit(val bytes: Long?) : SettingsIntent
    data class SetDownloadNext(val enabled: Boolean) : SettingsIntent
    data class SetDeleteFinished(val enabled: Boolean) : SettingsIntent
    data class SetSkipIntervals(val intervals: SkipIntervals) : SettingsIntent
    data object RequestDeleteAll : SettingsIntent
    data object DismissDeleteAll : SettingsIntent
    data object ConfirmDeleteAll : SettingsIntent
}
