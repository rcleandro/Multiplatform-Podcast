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
import io.ktor.utils.io.ioDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "SearchViewModel"

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: PodcastRepository,
    private val episodeDownloader: EpisodeDownloader,
    val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    private val _deleteEpisodeConfirmation = MutableStateFlow<Episode?>(null)
    val deleteEpisodeConfirmation: StateFlow<Episode?> = _deleteEpisodeConfirmation.asStateFlow()

    val activeDownloads = episodeDownloader.activeDownloads

    val pagedResults: Flow<PagingData<Episode>> = combine(_searchQuery, _refreshTrigger) { query, _ -> query }
        .debounce(300)
        .flatMapLatest { query ->
            AppLogger.d(TAG, "Search query changed or refreshed: $query")
            repository.searchEpisodesPaged(query.takeIf { it.isNotBlank() })
        }
        .cachedIn(viewModelScope)

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun playEpisode(episode: Episode) {
        viewModelScope.launch(ioDispatcher()) {
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
        _error.value = null
    }

    fun setError(message: String) {
        _error.value = message
    }

    fun downloadEpisode(episode: Episode) {
        viewModelScope.launch(ioDispatcher()) {
            AppLogger.i(TAG, "Starting download for episode from search: ${episode.title}")
            episodeDownloader.download(episode)
        }
    }

    fun deleteDownload(episodeId: String) {
        _deleteEpisodeConfirmation.value = null
        viewModelScope.launch(ioDispatcher()) {
            episodeDownloader.delete(episodeId)
        }
    }

    fun showDeleteConfirmation(episode: Episode) {
        _deleteEpisodeConfirmation.value = episode
    }

    fun hideDeleteConfirmation() {
        _deleteEpisodeConfirmation.value = null
    }
}
