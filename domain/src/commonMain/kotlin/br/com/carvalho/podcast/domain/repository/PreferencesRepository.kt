package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.LibraryLayout
import kotlinx.coroutines.flow.StateFlow

/** Choices the user makes once and expects to find on the next launch (ADR 0006). */
interface PreferencesRepository {
    val libraryLayout: StateFlow<LibraryLayout>

    fun setLibraryLayout(layout: LibraryLayout)
}
