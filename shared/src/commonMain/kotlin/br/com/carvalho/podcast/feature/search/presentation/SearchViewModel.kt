package br.com.carvalho.podcast.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import androidx.paging.PagingData
import androidx.paging.cachedIn
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "SearchViewModel"

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: PodcastRepository,
    private val episodeDownloader: EpisodeDownloader,
    val audioPlayer: AudioPlayer,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    val activeDownloads = episodeDownloader.activeDownloads

    val pagedResults: Flow<PagingData<Episode>> = _uiState.map { it.searchQuery }
        .distinctUntilChanged()
        .combine(_refreshTrigger) { query, _ -> query }
        .debounce(300)
        .flatMapLatest { query ->
            AppLogger.d(TAG, "Search query changed or refreshed: $query")
            repository.searchEpisodesPaged(query.takeIf { it.isNotBlank() })
        }
        .cachedIn(viewModelScope)

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun playEpisode(episode: Episode) {
        viewModelScope.launch(dispatchers.io) {
            val currentPlayerState = audioPlayer.playerState.value
            if (currentPlayerState.currentEpisode?.id == episode.id) {
                if (currentPlayerState.isPlaying) {
                    audioPlayer.pause()
                } else {
                    audioPlayer.resume()
                }
                return@launch
            }

            audioPlayer.setQueue(listOf(episode))
            audioPlayer.play(episode)
        }
    }

    fun refresh() {
        _refreshTrigger.value += 1
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun downloadEpisode(episode: Episode) {
        viewModelScope.launch(dispatchers.io) {
            AppLogger.i(TAG, "Starting download for episode from search: ${episode.title}")
            episodeDownloader.download(episode)
        }
    }

    fun deleteDownload(episodeId: String) {
        _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
        viewModelScope.launch(dispatchers.io) {
            episodeDownloader.delete(episodeId)
        }
    }

    fun showDeleteConfirmation(episode: Episode) {
        _uiState.update { it.copy(deleteEpisodeConfirmation = episode) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
    }
}

data class SearchUiState(
    val searchQuery: String = "",
    val error: String? = null,
    val deleteEpisodeConfirmation: Episode? = null
)
