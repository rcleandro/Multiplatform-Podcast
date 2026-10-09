package br.com.carvalho.podcast.feature.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.AnalyticsEvent
import br.com.carvalho.podcast.core.observability.Metrics
import br.com.carvalho.podcast.core.observability.urlHost
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.error_refresh_podcasts
import br.com.carvalho.podcast.core.ui.generated.resources.error_refresh_some_podcasts
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.model.sortedFor
import br.com.carvalho.podcast.domain.model.topCategories
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.presentation.UiMessage
import br.com.carvalho.podcast.presentation.toMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "LibraryViewModel"

@Suppress("LongParameterList") // one dependency per thing the screen does (list, refresh, delete, layout, play)
class LibraryViewModel(
    private val repository: PodcastRepository,
    private val refreshPodcastUseCase: RefreshPodcastUseCase,
    private val deletePodcastUseCase: DeletePodcastUseCase,
    private val preferences: PreferencesRepository,
    private val playEpisode: PlayEpisodeUseCase,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true, layout = preferences.libraryLayout.value))
    val uiState: StateFlow<LibraryUiState> = _uiState

    /** The category chip picked above the podcasts (18.2); null shows them all. */
    private val chosenCategory = MutableStateFlow<PodcastCategory?>(null)

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            val library = repository.getLibrary().onEach { Metrics.recordAppStart("podcasts" to it.size) }
            combine(
                library.onStart { emit(emptyList()) },
                preferences.librarySort,
                chosenCategory
            ) { all, sort, chosen ->
                val categories = all.flatMap { it.podcast.topCategories }
                    .groupingBy { it }.eachCount()
                    .entries.sortedByDescending { it.value }.map { it.key }
                // A category whose last podcast left the library goes back to all of them.
                val category = chosen?.takeIf { it in categories }
                val shown = if (category == null) all else all.filter { category in it.podcast.topCategories }
                LibraryUiState(
                    podcasts = shown.sortedFor(sort),
                    sort = sort,
                    categories = categories,
                    category = category
                )
            }.collect { listed ->
                _uiState.update {
                    it.copy(
                        podcasts = listed.podcasts,
                        sort = listed.sort,
                        categories = listed.categories,
                        category = listed.category,
                        isLoading = false,
                    )
                }
            }
        }
        repository.getInProgressEpisodes()
            .onEach { episodes -> _uiState.update { it.copy(inProgress = episodes) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.RequestDelete -> _uiState.update { it.copy(podcastToDelete = intent.podcast) }
            LibraryIntent.DismissDelete -> _uiState.update { it.copy(podcastToDelete = null) }
            LibraryIntent.ConfirmDelete -> confirmDelete()
            LibraryIntent.RefreshAll -> refreshAll()
            LibraryIntent.ToggleLayout -> toggleLayout()
            is LibraryIntent.ChangeSort -> preferences.setLibrarySort(intent.sort)
            is LibraryIntent.SelectCategory -> chosenCategory.value = intent.category
            is LibraryIntent.Play -> viewModelScope.launch(dispatchers.io) {
                analytics.logEvent(AnalyticsEvent.PlayEpisode(intent.episode.id, AnalyticsEvent.PlaySource.LIBRARY))
                playEpisode(intent.episode)
            }
        }
    }

    private fun toggleLayout() {
        val layout = if (_uiState.value.layout == LibraryLayout.GRID) LibraryLayout.LIST else LibraryLayout.GRID
        preferences.setLibraryLayout(layout)
        _uiState.update { it.copy(layout = layout) }
    }

    private fun confirmDelete() {
        val podcast = _uiState.value.podcastToDelete ?: return
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent(AnalyticsEvent.DeletePodcast(urlHost(podcast.feedUrl)))
            deletePodcastUseCase(podcast.id)
            _uiState.update { it.copy(podcastToDelete = null) }
        }
    }

    private fun refreshAll() {
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent(AnalyticsEvent.RefreshAllPodcasts)
            _uiState.update { it.copy(isRefreshing = true) }
            AppLogger.i(TAG, "Refreshing all podcasts")
            val summary = refreshPodcastUseCase.refreshAll()
            summary.failures.firstOrNull()?.let { firstFailure ->
                AppLogger.e(TAG, "${summary.failures.size} of ${summary.total} feeds failed", firstFailure)
                // All failed: the cause (offline, server…) says more than a count. Some failed: say how many.
                val message = if (summary.allFailed) {
                    UiMessage(firstFailure.toMessage(fallback = Res.string.error_refresh_podcasts))
                } else {
                    UiMessage(Res.string.error_refresh_some_podcasts, listOf(summary.failures.size, summary.total))
                }
                _messages.send(message)
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}

data class LibraryUiState(
    val podcasts: List<LibraryEntry> = emptyList(),
    /** "Continue listening": started episodes, shown above the podcasts while there is any. */
    val inProgress: List<Episode> = emptyList(),
    val layout: LibraryLayout = LibraryLayout.GRID,
    val sort: LibrarySort = LibrarySort.TITLE,
    /** The categories of the podcasts in the library, the most common first. */
    val categories: List<PodcastCategory> = emptyList(),
    val category: PodcastCategory? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val podcastToDelete: Podcast? = null,
)

sealed interface LibraryIntent {
    data object RefreshAll : LibraryIntent
    data object ToggleLayout : LibraryIntent
    data class ChangeSort(val sort: LibrarySort) : LibraryIntent
    data class SelectCategory(val category: PodcastCategory?) : LibraryIntent
    data class Play(val episode: Episode) : LibraryIntent
    data class RequestDelete(val podcast: Podcast) : LibraryIntent
    data object ConfirmDelete : LibraryIntent
    data object DismissDelete : LibraryIntent
}
