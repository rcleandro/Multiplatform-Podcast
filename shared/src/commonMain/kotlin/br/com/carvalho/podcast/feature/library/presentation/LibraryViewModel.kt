package br.com.carvalho.podcast.feature.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import br.com.carvalho.podcast.core.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "LibraryViewModel"

class LibraryViewModel(
    repository: PodcastRepository,
    private val addPodcastUseCase: AddPodcastFromUrlUseCase,
    private val refreshPodcastUseCase: RefreshPodcastUseCase,
    private val deletePodcastUseCase: DeletePodcastUseCase
) : ViewModel() {

    private val _isAddDialogOpen = MutableStateFlow(false)
    private val _addUrl = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)
    private val _podcastToDelete = MutableStateFlow<Podcast?>(null)

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.getPodcasts().onStart { emit(emptyList()) },
        _isAddDialogOpen,
        _addUrl,
        _isRefreshing,
        _podcastToDelete,
        _error
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val podcasts = args[0] as List<Podcast>
        val isAddDialogOpen = args[1] as Boolean
        val addUrl = args[2] as String
        val isRefreshing = args[3] as Boolean
        @Suppress("UNCHECKED_CAST")
        val podcastToDelete = args[4] as Podcast?
        val error = args[5] as String?

        LibraryUiState(
            podcasts = podcasts,
            isLoading = false,
            isAddDialogOpen = isAddDialogOpen,
            addUrl = addUrl,
            isRefreshing = isRefreshing,
            podcastToDelete = podcastToDelete,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState(isLoading = true)
    )

    fun clearError() {
        _error.value = null
    }

    fun onDeleteClicked(podcast: Podcast) {
        _podcastToDelete.value = podcast
    }

    fun onDismissDeleteDialog() {
        _podcastToDelete.value = null
    }

    fun confirmDelete() {
        val podcast = _podcastToDelete.value ?: return
        viewModelScope.launch {
            deletePodcastUseCase(podcast.id)
            _podcastToDelete.value = null
        }
    }

    fun onRefreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            AppLogger.i(TAG, "Refreshing all podcasts")
            try {
                refreshPodcastUseCase.refreshAll()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error refreshing all podcasts", e)
                _error.value = "Erro ao atualizar podcasts."
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onAddClicked() {
        _isAddDialogOpen.value = true
    }

    fun onDismissAddDialog() {
        _isAddDialogOpen.value = false
        _addUrl.value = ""
    }

    fun onUrlChanged(url: String) {
        _addUrl.value = url
    }

    fun addPodcast() {
        val url = _addUrl.value.trim()
        if (url.isBlank()) return

        val finalUrl = if (!url.startsWith("http")) "https://$url" else url

        viewModelScope.launch {
            _isRefreshing.value = true
            _isAddDialogOpen.value = false
            _error.value = null
            AppLogger.i(TAG, "Adding podcast from URL: $finalUrl")
            try {
                addPodcastUseCase(finalUrl).onFailure { e ->
                    AppLogger.e(TAG, "Failed to add podcast from URL: $finalUrl", e)
                    _error.value = "Erro ao adicionar podcast. Verifique a URL e a conexão (CORS pode bloquear no Wasm)."
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Unexpected error adding podcast", e)
                _error.value = "Ocorreu um erro inesperado: ${e.message}"
            } finally {
                _addUrl.value = ""
                _isRefreshing.value = false
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
    val error: String? = null
)
