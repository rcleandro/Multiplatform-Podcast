package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.LibrarySort
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

    override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)

    override fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    override val telemetryEnabled = MutableStateFlow(true)

    override fun setTelemetryEnabled(enabled: Boolean) {
        telemetryEnabled.value = enabled
    }
}
