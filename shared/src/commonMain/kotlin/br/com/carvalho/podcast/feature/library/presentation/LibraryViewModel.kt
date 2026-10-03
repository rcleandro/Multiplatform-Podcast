package br.com.carvalho.podcast.feature.library.presentation

import br.com.carvalho.podcast.domain.model.PodcastError
import br.com.carvalho.podcast.shared.error_unexpected
import br.com.carvalho.podcast.shared.error_add_podcast
import br.com.carvalho.podcast.shared.error_podcast_exists
import br.com.carvalho.podcast.shared.error_refresh_podcasts
import br.com.carvalho.podcast.shared.Res
import org.jetbrains.compose.resources.StringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.core.observability.Analytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "LibraryViewModel"

class LibraryViewModel(
    private val repository: PodcastRepository,
    private val addPodcastUseCase: AddPodcastFromUrlUseCase,
    private val refreshPodcastUseCase: RefreshPodcastUseCase,
    private val deletePodcastUseCase: DeletePodcastUseCase,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics
) : ViewModel() {


    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState

    init {
        viewModelScope.launch(dispatchers.io) {
            repository.getPodcasts().onStart { emit(emptyList()) }.collect { podcasts ->
                _uiState.update { it.copy(podcasts = podcasts, isLoading = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onDeleteClicked(podcast: Podcast) {
        _uiState.update { it.copy(podcastToDelete = podcast) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(podcastToDelete = null) }
    }

    fun confirmDelete() {
        val podcast = _uiState.value.podcastToDelete ?: return
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent("delete_podcast", mapOf(
                "podcast_id" to podcast.id,
                "podcast_title" to podcast.title
            ))
            deletePodcastUseCase(podcast.id)
            _uiState.update { it.copy(podcastToDelete = null) }
        }
    }

    fun onRefreshAll() {
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent("refresh_all_podcasts")
            _uiState.update { it.copy(isRefreshing = true) }
            AppLogger.i(TAG, "Refreshing all podcasts")
            try {
                refreshPodcastUseCase.refreshAll()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error refreshing all podcasts", e)
                _uiState.update { it.copy(error = Res.string.error_refresh_podcasts) }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onAddClicked() {
        _uiState.update { it.copy(isAddDialogOpen = true) }
    }

    fun onDismissAddDialog() {
        _uiState.update { it.copy(isAddDialogOpen = false, addUrl = "") }
    }

    fun onUrlChanged(url: String) {
        _uiState.update { it.copy(addUrl = url) }
    }

    fun addPodcast() {
        val url = _uiState.value.addUrl.trim()
        if (url.isBlank()) return

        val finalUrl = if (!url.startsWith("http")) "https://$url" else url

        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent("add_podcast_attempt", mapOf("url" to finalUrl))
            _uiState.update { it.copy(isRefreshing = true, isAddDialogOpen = false, error = null) }
            AppLogger.i(TAG, "Adding podcast from URL: $finalUrl")
            try {
                addPodcastUseCase(finalUrl).onSuccess {
                    analytics.logEvent("add_podcast_success", mapOf("url" to finalUrl))
                }.onFailure { e ->
                    AppLogger.e(TAG, "Failed to add podcast from URL: $finalUrl", e)
                    analytics.logEvent("add_podcast_failure", mapOf("url" to finalUrl, "error" to e.message))
                    _uiState.update {
                        it.copy(
                            error = if (e is PodcastError.AlreadyExists) {
                                Res.string.error_podcast_exists
                            } else {
                                Res.string.error_add_podcast
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Unexpected error adding podcast", e)
                _uiState.update { it.copy(error = Res.string.error_unexpected) }
            } finally {
                _uiState.update { it.copy(isRefreshing = false, addUrl = "") }
            }
        }
    }
}

data class LibraryUiState(
    val podcasts: List<Podcast> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isAddDialogOpen: Boolean = false,
    val podcastToDelete: Podcast? = null,
    val addUrl: String = "",
    val error: StringResource? = null
)
