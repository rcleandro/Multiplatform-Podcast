package br.com.carvalho.podcast.data.preferences

import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val LIBRARY_LAYOUT = "library_layout"
private const val LIBRARY_SORT = "library_sort"

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
}

// A value written by another app version, or by hand, falls back to the default instead of crashing.
private inline fun <reified T : Enum<T>> Settings.enumOrDefault(key: String, default: T): T =
    getStringOrNull(key)?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
