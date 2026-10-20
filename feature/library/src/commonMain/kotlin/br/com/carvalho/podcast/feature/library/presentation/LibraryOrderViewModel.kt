package br.com.carvalho.podcast.feature.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.sortedFor
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.ExplicitContent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The "Organize library" screen: the podcasts in their custom order, which this screen changes. */
class LibraryOrderViewModel(
    private val repository: PodcastRepository,
    preferences: PreferencesRepository,
    private val dispatchers: CoroutineDispatchers,
    /** Explicit podcasts stay out under Brazil's ECA Digital (26.3). */
    private val explicitContent: ExplicitContent = ExplicitContent(),
) : ViewModel() {

    private val library = repository.getLibrary()
        .map { it.sortedFor(LibrarySort.CUSTOM) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val podcasts: StateFlow<List<LibraryEntry>> = combine(library, explicitContent.rule) { all, rule ->
        all.filter { rule.allows(it.podcast) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Arranging the podcasts only makes sense if the library shows the arranged order.
        preferences.setLibrarySort(LibrarySort.CUSTOM)
    }

    fun reorder(podcastIds: List<String>) {
        // The hidden ones keep their turn after the ones shown, so the order stays whole.
        val hidden = library.value.map { it.podcast.id }.filterNot { it in podcastIds }
        viewModelScope.launch(dispatchers.io) { repository.reorderLibrary(podcastIds + hidden) }
    }
}
