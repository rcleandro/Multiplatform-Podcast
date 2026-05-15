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
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "PodcastDetailViewModel"

class PodcastDetailViewModel(
    private val podcastId: String,
    private val audioPlayer: AudioPlayer,
    private val refreshPodcastUseCase: RefreshPodcastUseCase,
    private val episodeDownloader: EpisodeDownloader,
    repository: PodcastRepository
) : ViewModel() {

    val playerState = audioPlayer.playerState
    val activeDownloads = episodeDownloader.activeDownloads

    private val _filter = MutableStateFlow(EpisodeFilter.ALL)
    private val _isRefreshing = MutableStateFlow(false)

    private val _error = MutableStateFlow<String?>(null)

    val pagedEpisodes: Flow<PagingData<Episode>> = repository.getEpisodesPaged(podcastId)
        .cachedIn(viewModelScope)

    val uiState: StateFlow<PodcastDetailUiState> = combine(
        repository.getPodcastByIdFlow(podcastId),
        repository.getEpisodes(podcastId),
        _filter,
        _isRefreshing,
        _error
    ) { podcast, episodes, filter, isRefreshing, error ->
        val filteredEpisodes = when (filter) {
            EpisodeFilter.ALL -> episodes
            EpisodeFilter.UNPLAYED -> episodes.filter { !it.isPlayed }
            EpisodeFilter.DOWNLOADED -> episodes.filter { it.isDownloaded }
        }
        PodcastDetailUiState(
            podcast = podcast,
            episodes = filteredEpisodes,
            filter = filter,
            isLoading = false,
            isRefreshing = isRefreshing,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PodcastDetailUiState(isLoading = true)
    )

    fun clearError() {
        _error.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            AppLogger.i(TAG, "Refreshing podcast details for id: $podcastId")
            try {
                refreshPodcastUseCase(podcastId)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error refreshing podcast $podcastId", e)
                _error.value = "Erro ao atualizar episódios"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setFilter(filter: EpisodeFilter) {
        _filter.value = filter
    }

    fun playEpisode(episodeId: String) {
        viewModelScope.launch {
            val allEpisodes = uiState.value.episodes
            val selectedIndex = allEpisodes.indexOfFirst { it.id == episodeId }
            if (selectedIndex == -1) {
                AppLogger.e(TAG, "Episode $episodeId not found in current list")
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

    fun downloadEpisode(episodeId: String) {
        viewModelScope.launch {
            val episode = uiState.value.episodes.find { it.id == episodeId } ?: return@launch
            AppLogger.i(TAG, "Starting download for episode: ${episode.title}")
            episodeDownloader.download(episode)
        }
    }

    fun deleteDownload(episodeId: String) {
        viewModelScope.launch {
            episodeDownloader.delete(episodeId)
        }
    }
}


data class PodcastDetailUiState(
    val podcast: Podcast? = null,
    val episodes: List<Episode> = emptyList(),
    val filter: EpisodeFilter = EpisodeFilter.ALL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

enum class EpisodeFilter { ALL, UNPLAYED, DOWNLOADED }
