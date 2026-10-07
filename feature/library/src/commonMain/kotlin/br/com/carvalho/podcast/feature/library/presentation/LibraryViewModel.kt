package br.com.carvalho.podcast.feature.library.presentation

import br.com.carvalho.podcast.core.ui.generated.resources.error_add_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.error_refresh_podcasts
import br.com.carvalho.podcast.core.ui.generated.resources.error_refresh_some_podcasts
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import androidx.lifecycle.ViewModel
import br.com.carvalho.podcast.presentation.UiMessage
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.sortedFor
import kotlinx.coroutines.flow.combine
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.presentation.toMessage
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.validFeedUrl
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.urlHost
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "LibraryViewModel"

@Suppress("LongParameterList") // one dependency per thing the screen does (list, add, refresh, delete, layout)
class LibraryViewModel(
    private val repository: PodcastRepository,
    private val addPodcastUseCase: AddPodcastFromUrlUseCase,
    private val refreshPodcastUseCase: RefreshPodcastUseCase,
    private val deletePodcastUseCase: DeletePodcastUseCase,
    private val preferences: PreferencesRepository,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics
) : ViewModel() {


    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true, layout = preferences.libraryLayout.value))
    val uiState: StateFlow<LibraryUiState> = _uiState

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            combine(repository.getLibrary().onStart { emit(emptyList()) }, preferences.librarySort) { podcasts, sort ->
                podcasts.sortedFor(sort) to sort
            }.collect { (podcasts, sort) ->
                _uiState.update { it.copy(podcasts = podcasts, sort = sort, isLoading = false) }
            }
        }
    }

    fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.RequestDelete -> _uiState.update { it.copy(podcastToDelete = intent.podcast) }
            LibraryIntent.DismissDelete -> _uiState.update { it.copy(podcastToDelete = null) }
            LibraryIntent.ConfirmDelete -> confirmDelete()
            LibraryIntent.RefreshAll -> refreshAll()
            LibraryIntent.OpenAddDialog -> _uiState.update { it.copy(isAddDialogOpen = true) }
            LibraryIntent.DismissAddDialog -> _uiState.update { it.copy(isAddDialogOpen = false, addUrl = "") }
            is LibraryIntent.ChangeUrl -> _uiState.update { it.copy(addUrl = intent.url) }
            LibraryIntent.ConfirmAdd -> addPodcast()
            LibraryIntent.ToggleLayout -> toggleLayout()
            is LibraryIntent.ChangeSort -> preferences.setLibrarySort(intent.sort)
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
            analytics.logEvent("delete_podcast", mapOf("host" to urlHost(podcast.feedUrl)))
            deletePodcastUseCase(podcast.id)
            _uiState.update { it.copy(podcastToDelete = null) }
        }
    }

    private fun refreshAll() {
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent("refresh_all_podcasts")
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

    private fun addPodcast() {
        val url = _uiState.value.addUrl.trim()
        if (url.isBlank()) return

        viewModelScope.launch(dispatchers.io) {
            // Only the host: the typed text may lack the scheme, which the logger needs to spot (and cut) a URL.
            val host = urlHost(validFeedUrl(url) ?: url)
            analytics.logEvent("add_podcast_attempt", mapOf("host" to host))
            _uiState.update { it.copy(isRefreshing = true, isAddDialogOpen = false) }
            AppLogger.i(TAG, "Adding podcast from host: $host")
            addPodcastUseCase(url).onSuccess {
                analytics.logEvent("add_podcast_success", mapOf("host" to host))
            }.onFailure { e ->
                AppLogger.e(TAG, "Failed to add podcast from host: $host", e)
                analytics.logEvent("add_podcast_failure", mapOf("host" to host, "error" to e::class.simpleName))
                _messages.send(UiMessage(e.toMessage(fallback = Res.string.error_add_podcast)))
            }
            _uiState.update { it.copy(isRefreshing = false, addUrl = "") }
        }
    }
}

data class LibraryUiState(
    val podcasts: List<LibraryEntry> = emptyList(),
    val layout: LibraryLayout = LibraryLayout.GRID,
    val sort: LibrarySort = LibrarySort.TITLE,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isAddDialogOpen: Boolean = false,
    val podcastToDelete: Podcast? = null,
    val addUrl: String = "",
)

sealed interface LibraryIntent {
    data object RefreshAll : LibraryIntent
    data object OpenAddDialog : LibraryIntent
    data class ChangeUrl(val url: String) : LibraryIntent
    data object ConfirmAdd : LibraryIntent
    data object ToggleLayout : LibraryIntent
    data class ChangeSort(val sort: LibrarySort) : LibraryIntent
    data object DismissAddDialog : LibraryIntent
    data class RequestDelete(val podcast: Podcast) : LibraryIntent
    data object ConfirmDelete : LibraryIntent
    data object DismissDelete : LibraryIntent
}
