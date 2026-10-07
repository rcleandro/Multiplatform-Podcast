package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.LibraryLayout
import kotlinx.coroutines.flow.MutableStateFlow

class FakePreferencesRepository : PreferencesRepository {
    override val libraryLayout = MutableStateFlow(LibraryLayout.GRID)

    override fun setLibraryLayout(layout: LibraryLayout) {
        libraryLayout.value = layout
    }
}
