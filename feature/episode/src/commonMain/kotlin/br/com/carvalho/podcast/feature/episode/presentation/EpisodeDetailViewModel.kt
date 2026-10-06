package br.com.carvalho.podcast.feature.episode.presentation

import androidx.lifecycle.ViewModel
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "EpisodeDetailViewModel"

class EpisodeDetailViewModel(
    private val episodeId: String,
    private val repository: PodcastRepository,
    private val playEpisode: PlayEpisodeUseCase,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpisodeDetailUiState(isLoading = true))
    val uiState: StateFlow<EpisodeDetailUiState> = _uiState.asStateFlow()

    init {
        loadEpisode()
    }

    fun onIntent(intent: EpisodeDetailIntent) {
        when (intent) {
            EpisodeDetailIntent.Play -> play()
            EpisodeDetailIntent.Retry -> loadEpisode()
        }
    }

    private fun loadEpisode() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent("load_episode_detail", mapOf("episode_id" to episodeId))
            AppLogger.d(TAG, "Loading episode detail for id: $episodeId")
            try {
                val episode = repository.getEpisodeById(episodeId)
                _uiState.value = EpisodeDetailUiState(
                    episode = episode,
                    isLoading = false
                )
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error loading episode detail", e)
                analytics.logEvent(
                    "load_episode_detail_error",
                    mapOf("episode_id" to episodeId, "error" to e::class.simpleName)
                )
                _uiState.value = EpisodeDetailUiState(isLoading = false, loadFailed = true)
            }
        }
    }

    private fun play() {
        uiState.value.episode?.let { episode ->
            viewModelScope.launch(dispatchers.io) {
                analytics.logEvent("play_episode_from_episode_detail", mapOf("episode_id" to episode.id))
                playEpisode(episode)
            }
        }
    }
}

data class EpisodeDetailUiState(
    val episode: Episode? = null,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
)

sealed interface EpisodeDetailIntent {
    data object Play : EpisodeDetailIntent
    data object Retry : EpisodeDetailIntent
}
