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
import br.com.carvalho.podcast.domain.model.RefreshInterval
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.model.SyncFolder
import br.com.carvalho.podcast.domain.model.ThemeMode
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.ExportBackupUseCase
import br.com.carvalho.podcast.domain.usecase.ExportOpmlUseCase
import br.com.carvalho.podcast.domain.usecase.FolderSyncStatus
import br.com.carvalho.podcast.domain.usecase.FolderSyncUseCase
import br.com.carvalho.podcast.domain.usecase.ImportOpmlUseCase
import br.com.carvalho.podcast.domain.usecase.OpmlImport
import br.com.carvalho.podcast.domain.usecase.RestoreBackupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Settings (18.5): the choices that change how the app works, each saved at once. */
@Suppress("LongParameterList") // one dependency per thing the screen does
class SettingsViewModel(
    private val preferences: PreferencesRepository,
    private val downloader: EpisodeDownloader,
    private val directories: AppDirectories,
    private val repository: PodcastRepository,
    private val dispatchers: CoroutineDispatchers,
    private val importOpml: ImportOpmlUseCase,
    private val exportOpml: ExportOpmlUseCase,
    private val exportBackup: ExportBackupUseCase,
    private val restoreBackup: RestoreBackupUseCase,
    private val folderSync: FolderSyncUseCase,
) : ViewModel() {
    private val usedBytes = MutableStateFlow<Long?>(null)
    private val disk = MutableStateFlow<DiskSpace?>(null)
    private val confirmingDeleteAll = MutableStateFlow(false)
    private val opml = MutableStateFlow(OpmlState())
    private val backup = MutableStateFlow(OpmlState())

    private val choices = combine(
        combine(preferences.themeMode, preferences.skipIntervals, preferences.appLanguage, ::Triple),
        combine(
            preferences.telemetryEnabled,
            preferences.showExplicit,
            combine(preferences.skipSilence, preferences.boostVoice, ::Pair),
            ::Triple,
        ),
        combine(
            preferences.wifiOnlyDownloads,
            preferences.refreshIntervalHours,
            preferences.notifyNewEpisodes,
            ::Triple,
        ),
        combine(
            preferences.downloadLimitBytes,
            preferences.downloadNextOnFinish,
            combine(preferences.deleteFinishedDownloads, preferences.deleteDownloadsAfterDays, ::Pair),
            ::Triple
        ),
        combine(usedBytes, disk, confirmingDeleteAll, opml, backup, ::Measured),
    ) { (theme, skips, language), (telemetry, explicit, speech), updates, downloads, measured ->
        val (wifiOnly, refreshHours, notify) = updates
        val (limit, downloadNext, deleting) = downloads
        val (deleteFinished, deleteAfterDays) = deleting
        SettingsUiState(
            themeMode = theme,
            language = language,
            telemetryEnabled = telemetry,
            showExplicit = explicit,
            skipSilence = speech.first,
            boostVoice = speech.second,
            usedBytes = measured.bytes,
            wifiOnlyDownloads = wifiOnly,
            refreshIntervalHours = refreshHours,
            notifyNewEpisodes = notify,
            downloadLimitBytes = limit,
            downloadNextOnFinish = downloadNext,
            disk = measured.disk,
            deleteFinishedDownloads = deleteFinished,
            deleteDownloadsAfterDays = deleteAfterDays,
            isConfirmingDeleteAll = measured.confirming,
            skipIntervals = skips,
            opml = measured.opml.status,
            opmlToSave = measured.opml.toSave,
            backup = measured.backup.status,
            backupToSave = measured.backup.toSave,
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        choices,
        preferences.syncFolder,
        folderSync.status,
    ) { state, folder, sync -> state.copy(syncFolderName = folder?.name, folderSync = sync) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

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

    /** A file that cannot be read, or lists no feed, adds nothing and says so. */
    private fun import(text: String?) {
        if (opml.value.status is OpmlStatus.Importing) return
        viewModelScope.launch(dispatchers.io) {
            val result = text?.let {
                importOpml(it) { done, total ->
                    opml.update { state -> state.copy(status = OpmlStatus.Importing(done, total)) }
                }
            }
            opml.update { it.copy(status = OpmlStatus.Imported(result ?: OpmlImport(0, 0, 0))) }
        }
    }

    private fun export() = viewModelScope.launch(dispatchers.io) {
        val export = exportOpml()
        opml.update { it.copy(toSave = export.opml, exportedCount = export.podcasts) }
    }

    /** Like an OPML import, with the backup's progress and settings after the podcasts (19.11). */
    private fun restore(text: String?) {
        if (backup.value.status is OpmlStatus.Importing) return
        viewModelScope.launch(dispatchers.io) {
            val result = text?.let {
                restoreBackup(it) { done, total ->
                    backup.update { state -> state.copy(status = OpmlStatus.Importing(done, total)) }
                }
            }
            backup.update { it.copy(status = result?.let(OpmlStatus::Imported) ?: OpmlStatus.NotABackup) }
        }
    }

    private fun exportAll() = viewModelScope.launch(dispatchers.io) {
        val export = exportBackup()
        backup.update { it.copy(toSave = export.text, exportedCount = export.podcasts) }
    }

    @Suppress("CyclomaticComplexMethod") // one branch per intent
    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetTheme -> preferences.setThemeMode(intent.mode)
            is SettingsIntent.SetLanguage -> preferences.setAppLanguage(intent.language)
            is SettingsIntent.SetTelemetry -> preferences.setTelemetryEnabled(intent.enabled)
            is SettingsIntent.SetShowExplicit -> preferences.setShowExplicit(intent.enabled)
            is SettingsIntent.SetSkipSilence -> preferences.setSkipSilence(intent.enabled)
            is SettingsIntent.SetRefreshInterval -> preferences.setRefreshIntervalHours(intent.hours)
            is SettingsIntent.SetNotifyNewEpisodes -> preferences.setNotifyNewEpisodes(intent.enabled)
            is SettingsIntent.SetBoostVoice -> preferences.setBoostVoice(intent.enabled)
            is SettingsIntent.SetWifiOnly -> preferences.setWifiOnlyDownloads(intent.enabled)
            is SettingsIntent.SetDownloadLimit -> preferences.setDownloadLimitBytes(intent.bytes)
            is SettingsIntent.SetDownloadNext -> preferences.setDownloadNextOnFinish(intent.enabled)
            is SettingsIntent.SetDeleteFinished -> preferences.setDeleteFinishedDownloads(intent.enabled)
            is SettingsIntent.SetDeleteAfter -> preferences.setDeleteDownloadsAfterDays(intent.days)
            is SettingsIntent.SetSkipIntervals -> preferences.setSkipIntervals(intent.intervals)
            SettingsIntent.RequestDeleteAll -> confirmingDeleteAll.value = true
            SettingsIntent.DismissDeleteAll -> confirmingDeleteAll.value = false
            SettingsIntent.ConfirmDeleteAll -> deleteAllDownloads()
            is SettingsIntent.ImportOpml -> import(intent.text)
            SettingsIntent.ExportOpml -> export()
            SettingsIntent.OpmlHandedOver -> opml.update { it.copy(toSave = null) }
            is SettingsIntent.OpmlSaved -> opml.update { it.saved(intent.saved) }
            is SettingsIntent.RestoreBackup -> restore(intent.text)
            SettingsIntent.ExportBackup -> exportAll()
            SettingsIntent.BackupHandedOver -> backup.update { it.copy(toSave = null) }
            is SettingsIntent.BackupSaved -> backup.update { it.saved(intent.saved) }
            // Choosing again keeps this device's file, so the folder does not fill with old ones.
            is SettingsIntent.ChooseSyncFolder -> preferences.setSyncFolder(
                SyncFolder(
                    intent.bookmark,
                    intent.name,
                    preferences.syncFolder.value?.fileName ?: FolderSyncUseCase.newFileName(),
                ),
            )
            SettingsIntent.StopSync -> preferences.setSyncFolder(null)
        }
    }
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val telemetryEnabled: Boolean = true,
    val showExplicit: Boolean = true,
    val skipSilence: Boolean = false,
    val boostVoice: Boolean = false,
    /** Disk space the downloads take; null while it is measured, and where there are no downloads (Web). */
    val usedBytes: Long? = null,
    val wifiOnlyDownloads: Boolean = false,
    /** Hours between looks for new episodes while closed (18.18); 0 never looks. */
    val refreshIntervalHours: Int = RefreshInterval.DEFAULT_HOURS,
    val notifyNewEpisodes: Boolean = true,
    /** Null for no limit. */
    val downloadLimitBytes: Long? = null,
    val downloadNextOnFinish: Boolean = false,
    /** The disk the downloads live on; null where it is not known. */
    val disk: DiskSpace? = null,
    val deleteFinishedDownloads: Boolean = false,
    /** Days a download stays before it is deleted (18.19); 0 keeps them. */
    val deleteDownloadsAfterDays: Int = 0,
    /** "Delete all downloads" waits for this confirmation. */
    val isConfirmingDeleteAll: Boolean = false,
    val skipIntervals: SkipIntervals = SkipIntervals(),
    /** What the last OPML import or export did (18.13). */
    val opml: OpmlStatus = OpmlStatus.Idle,
    /** An exported OPML waiting for the screen to ask where to save it. */
    val opmlToSave: String? = null,
    /** What the last backup restore or export did (19.11). */
    val backup: OpmlStatus = OpmlStatus.Idle,
    /** An exported backup waiting for the screen to ask where to save it. */
    val backupToSave: String? = null,
    /** The folder the library syncs through (19.12); null for none. */
    val syncFolderName: String? = null,
    val folderSync: FolderSyncStatus = FolderSyncStatus.Idle,
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
    data class SetShowExplicit(val enabled: Boolean) : SettingsIntent
    data class SetSkipSilence(val enabled: Boolean) : SettingsIntent
    data class SetRefreshInterval(val hours: Int) : SettingsIntent
    data class SetNotifyNewEpisodes(val enabled: Boolean) : SettingsIntent
    data class SetBoostVoice(val enabled: Boolean) : SettingsIntent
    data class SetWifiOnly(val enabled: Boolean) : SettingsIntent
    data class SetDownloadLimit(val bytes: Long?) : SettingsIntent
    data class SetDownloadNext(val enabled: Boolean) : SettingsIntent
    data class SetDeleteFinished(val enabled: Boolean) : SettingsIntent
    data class SetDeleteAfter(val days: Int) : SettingsIntent
    data class SetSkipIntervals(val intervals: SkipIntervals) : SettingsIntent
    data object RequestDeleteAll : SettingsIntent
    data object DismissDeleteAll : SettingsIntent
    data object ConfirmDeleteAll : SettingsIntent

    /** The text of the file the user picked, or null when it could not be read (18.13). */
    data class ImportOpml(val text: String?) : SettingsIntent
    data object ExportOpml : SettingsIntent

    /** The screen took [SettingsUiState.opmlToSave] to the save dialog. */
    data object OpmlHandedOver : SettingsIntent
    data class OpmlSaved(val saved: Boolean) : SettingsIntent

    /** The same steps for the backup (19.11). */
    data class RestoreBackup(val text: String?) : SettingsIntent
    data object ExportBackup : SettingsIntent
    data object BackupHandedOver : SettingsIntent
    data class BackupSaved(val saved: Boolean) : SettingsIntent

    /** The folder picked for library sync (19.12), with the platform's lasting permission to it. */
    data class ChooseSyncFolder(val bookmark: String, val name: String) : SettingsIntent
    data object StopSync : SettingsIntent
}

/** OPML import and export (18.13), shown under their rows. */
sealed interface OpmlStatus {
    data object Idle : OpmlStatus
    data class Importing(val done: Int, val total: Int) : OpmlStatus

    /** All zeros when the file had no feed to import. */
    data class Imported(val result: OpmlImport) : OpmlStatus
    data class Exported(val podcasts: Int) : OpmlStatus
    data object ExportFailed : OpmlStatus

    /** The file picked to restore is not one of this app's backups. */
    data object NotABackup : OpmlStatus
}

/** An import or export of a file, and the exported text waiting to be saved. */
private data class OpmlState(
    val status: OpmlStatus = OpmlStatus.Idle,
    val toSave: String? = null,
    val exportedCount: Int = 0,
) {
    fun saved(saved: Boolean) =
        copy(status = if (saved) OpmlStatus.Exported(exportedCount) else OpmlStatus.ExportFailed)
}

private data class Measured(
    val bytes: Long?,
    val disk: DiskSpace?,
    val confirming: Boolean,
    val opml: OpmlState,
    val backup: OpmlState,
)
