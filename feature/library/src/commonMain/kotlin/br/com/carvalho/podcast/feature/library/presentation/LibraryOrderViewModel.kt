package br.com.carvalho.podcast.feature.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.sortedFor
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The "Organize library" screen: the podcasts in their custom order, which this screen changes. */
class LibraryOrderViewModel(
    private val repository: PodcastRepository,
    preferences: PreferencesRepository,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {

    val podcasts: StateFlow<List<LibraryEntry>> = repository.getLibrary()
        .map { it.sortedFor(LibrarySort.CUSTOM) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Arranging the podcasts only makes sense if the library shows the arranged order.
        preferences.setLibrarySort(LibrarySort.CUSTOM)
    }

    fun reorder(podcastIds: List<String>) {
        viewModelScope.launch(dispatchers.io) { repository.reorderLibrary(podcastIds) }
    }
}
