package br.com.carvalho.podcast.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import androidx.paging.PagingData
import androidx.paging.cachedIn
import br.com.carvalho.podcast.core.observability.Analytics
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
    private val audioPlayer: AudioPlayer,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    val activeDownloads = episodeDownloader.activeDownloads
    val playerState = audioPlayer.playerState

    val pagedResults: Flow<PagingData<Episode>> = _uiState.map { it.searchQuery }
        .distinctUntilChanged()
        .combine(_refreshTrigger) { query, _ -> query }
        .debounce(AppConfig.SEARCH_DEBOUNCE_MS)
        .flatMapLatest { query ->
            AppLogger.d(TAG, "Search query changed or refreshed: $query")
            repository.searchEpisodesPaged(query.takeIf { it.isNotBlank() })
        }
        .cachedIn(viewModelScope)

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.ChangeQuery -> _uiState.update { it.copy(searchQuery = intent.query) }
            SearchIntent.Refresh -> _refreshTrigger.value += 1
            is SearchIntent.Play -> playEpisode(intent.episode)
            is SearchIntent.Download -> downloadEpisode(intent.episode)
            is SearchIntent.CancelDownload -> cancelDownload(intent.episode.id)
            is SearchIntent.RequestDeleteDownload ->
                _uiState.update { it.copy(deleteEpisodeConfirmation = intent.episode) }
            is SearchIntent.ConfirmDeleteDownload -> deleteDownload(intent.episode.id)
            SearchIntent.DismissDeleteDownload -> _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
        }
    }

    private fun playEpisode(episode: Episode) {
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent("play_episode_from_search", mapOf(
                "episode_id" to episode.id,
                "episode_title" to episode.title
            ))
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

    private fun downloadEpisode(episode: Episode) {
        viewModelScope.launch(dispatchers.io) {
            AppLogger.i(TAG, "Starting download for episode from search: ${episode.title}")
            episodeDownloader.download(episode)
        }
    }

    private fun cancelDownload(episodeId: String) {
        viewModelScope.launch(dispatchers.io) {
            episodeDownloader.cancel(episodeId)
        }
    }

    private fun deleteDownload(episodeId: String) {
        _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
        viewModelScope.launch(dispatchers.io) {
            episodeDownloader.delete(episodeId)
        }
    }
}

data class SearchUiState(
    val searchQuery: String = "",
    val deleteEpisodeConfirmation: Episode? = null
)

sealed interface SearchIntent {
    data class ChangeQuery(val query: String) : SearchIntent
    data object Refresh : SearchIntent
    data class Play(val episode: Episode) : SearchIntent
    data class Download(val episode: Episode) : SearchIntent
    data class CancelDownload(val episode: Episode) : SearchIntent
    data class RequestDeleteDownload(val episode: Episode) : SearchIntent
    data class ConfirmDeleteDownload(val episode: Episode) : SearchIntent
    data object DismissDeleteDownload : SearchIntent
}
