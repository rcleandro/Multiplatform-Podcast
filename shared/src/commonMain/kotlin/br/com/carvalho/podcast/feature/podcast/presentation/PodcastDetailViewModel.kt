package br.com.carvalho.podcast.feature.podcast.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "PodcastDetailViewModel"

class PodcastDetailViewModel(
    private val podcastId: String,
    private val audioPlayer: AudioPlayer,
    private val refreshPodcastUseCase: RefreshPodcastUseCase,
    private val episodeDownloader: EpisodeDownloader,
    private val repository: PodcastRepository,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    val playerState = audioPlayer.playerState
    val activeDownloads = episodeDownloader.activeDownloads

    private val _uiState = MutableStateFlow(PodcastDetailUiState(isLoading = true))
    val uiState: StateFlow<PodcastDetailUiState> = _uiState

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedEpisodes: Flow<PagingData<Episode>> = _uiState
        .flatMapLatest { state ->
            repository.getEpisodesPaged(podcastId)
                .map { pagingData ->
                    pagingData.filter { episode ->
                        when (state.filter) {
                            EpisodeFilter.ALL -> true
                            EpisodeFilter.UNPLAYED -> !episode.isPlayed
                            EpisodeFilter.DOWNLOADED -> episode.isDownloaded
                        }
                    }
                }
        }.cachedIn(viewModelScope)

    init {
        combine(
            repository.getPodcastByIdFlow(podcastId),
            repository.getEpisodes(podcastId)
        ) { podcast, episodes ->
            PodcastDetailUiState(podcast = podcast, episodes = episodes, isLoading = false)
        }.onEach { newState ->
            _uiState.update { newState }
        }.launchIn(viewModelScope)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(dispatchers.io) {
            AppLogger.i(TAG, "Refreshing podcast details for id: $podcastId")
            try {
                refreshPodcastUseCase(podcastId)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error refreshing podcast $podcastId", e)
                _uiState.update { it.copy(error = "Erro ao atualizar episódios") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setFilter(filter: EpisodeFilter) {
        _uiState.update { it.copy(filter = filter) }
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

            val allEpisodes = uiState.value.episodes
            val selectedIndex = allEpisodes.indexOfFirst { it.id == episode.id }
            if (selectedIndex == -1) {
                AppLogger.e(TAG, "Episode ${episode.id} not found in current list")
                return@launch
            }

            val selectedEpisode = allEpisodes[selectedIndex]
            val resolvedEpisode = selectedEpisode.copy(localPath = episodeDownloader.getLocalPath(selectedEpisode.id))

            AppLogger.i(TAG, "Playing episode: ${resolvedEpisode.title} (Local: ${resolvedEpisode.localPath != null})")

            val queue = allEpisodes.subList(0, selectedIndex + 1).reversed()
            audioPlayer.setQueue(queue)
            audioPlayer.play(resolvedEpisode)
        }
    }

    fun downloadEpisode(episode: Episode) {
        viewModelScope.launch(dispatchers.io) {
            AppLogger.i(TAG, "Starting download for episode: ${episode.title}")
            episodeDownloader.download(episode)
        }
    }

    fun deleteDownload(episodeId: String) {
        _uiState.update { it.copy(isLoading = true, deleteEpisodeConfirmation = null) }
        viewModelScope.launch(dispatchers.io) {
            episodeDownloader.delete(episodeId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun showDeleteConfirmation(episode: Episode) {
        _uiState.update { it.copy(deleteEpisodeConfirmation = episode) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
    }

    fun markAsPlayed(episodeId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(dispatchers.io) {
            repository.markEpisodeAsPlayed(episodeId)
            _uiState.update { it.copy(isLoading = false, selectedEpisode = null) }
        }
    }

    fun markOlderAsPlayed(publishDate: Long) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(dispatchers.io) {
            repository.markOlderEpisodesAsPlayed(podcastId, publishDate)
            _uiState.update { it.copy(isLoading = false, selectedEpisode = null) }
        }
    }

    fun onSelectEpisode(episode: Episode? = null) {
        _uiState.update { it.copy(selectedEpisode = episode) }
    }
}


data class PodcastDetailUiState(
    val podcast: Podcast? = null,
    val episodes: List<Episode> = emptyList(),
    val filter: EpisodeFilter = EpisodeFilter.ALL,
    val selectedEpisode: Episode? = null,
    val deleteEpisodeConfirmation: Episode? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

enum class EpisodeFilter { ALL, UNPLAYED, DOWNLOADED }
