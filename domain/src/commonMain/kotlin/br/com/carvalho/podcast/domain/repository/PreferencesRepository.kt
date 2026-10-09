package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/** Choices the user makes once and expects to find on the next launch (ADR 0006). */
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

    val themeMode: StateFlow<ThemeMode>

    fun setThemeMode(mode: ThemeMode)

    /** Analytics and crash reports; on by default, the user can turn them off (16.4). */
    val telemetryEnabled: StateFlow<Boolean>

    fun setTelemetryEnabled(enabled: Boolean)
}
