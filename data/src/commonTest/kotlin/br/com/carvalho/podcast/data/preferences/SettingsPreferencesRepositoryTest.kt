package br.com.carvalho.podcast.data.preferences

import br.com.carvalho.podcast.domain.model.LibraryLayout
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsPreferencesRepositoryTest {
    private val settings = MapSettings()

    @Test
    fun theLibraryStartsAsAGrid() {
        assertEquals(LibraryLayout.GRID, SettingsPreferencesRepository(settings).libraryLayout.value)
    }

    @Test
    fun theChosenLayoutIsKeptForTheNextLaunch() {
        SettingsPreferencesRepository(settings).setLibraryLayout(LibraryLayout.LIST)

        assertEquals(LibraryLayout.LIST, SettingsPreferencesRepository(settings).libraryLayout.value)
    }

    @Test
    fun theFlowEmitsTheNewLayout() {
        val repository = SettingsPreferencesRepository(settings)

        repository.setLibraryLayout(LibraryLayout.LIST)

        assertEquals(LibraryLayout.LIST, repository.libraryLayout.value)
    }

    @Test
    fun anUnknownStoredValueFallsBackToTheGrid() {
        settings.putString("library_layout", "MOSAIC")

        assertEquals(LibraryLayout.GRID, SettingsPreferencesRepository(settings).libraryLayout.value)
    }
}
