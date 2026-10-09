package br.com.carvalho.podcast.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.ThemeMode
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Settings (18.5): the choices that change how the app works, each saved at once. */
class SettingsViewModel(
    private val preferences: PreferencesRepository,
    private val downloader: EpisodeDownloader,
    dispatchers: CoroutineDispatchers,
) : ViewModel() {
    private val usedBytes = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.themeMode,
        preferences.telemetryEnabled,
        preferences.wifiOnlyDownloads,
        preferences.downloadLimitBytes,
        usedBytes,
    ) { theme, telemetry, wifiOnly, limit, bytes ->
        SettingsUiState(theme, telemetry, bytes, wifiOnly, limit)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    init {
        if (supportsDownloads) viewModelScope.launch(dispatchers.io) { usedBytes.value = downloader.usedBytes() }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetTheme -> preferences.setThemeMode(intent.mode)
            is SettingsIntent.SetTelemetry -> preferences.setTelemetryEnabled(intent.enabled)
            is SettingsIntent.SetWifiOnly -> preferences.setWifiOnlyDownloads(intent.enabled)
            is SettingsIntent.SetDownloadLimit -> preferences.setDownloadLimitBytes(intent.bytes)
        }
    }
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val telemetryEnabled: Boolean = true,
    /** Disk space the downloads take; null while it is measured, and where there are no downloads (Web). */
    val usedBytes: Long? = null,
    val wifiOnlyDownloads: Boolean = false,
    /** Null for no limit. */
    val downloadLimitBytes: Long? = null,
)

sealed interface SettingsIntent {
    data class SetTheme(val mode: ThemeMode) : SettingsIntent
    data class SetTelemetry(val enabled: Boolean) : SettingsIntent
    data class SetWifiOnly(val enabled: Boolean) : SettingsIntent
    data class SetDownloadLimit(val bytes: Long?) : SettingsIntent
}
