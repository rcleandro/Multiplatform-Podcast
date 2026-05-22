package br.com.carvalho.podcast.feature.episode.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.AudioPlayer
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
    private val audioPlayer: AudioPlayer,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpisodeDetailUiState(isLoading = true))
    val uiState: StateFlow<EpisodeDetailUiState> = _uiState.asStateFlow()

    init {
        loadEpisode()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadEpisode() {
        viewModelScope.launch(dispatchers.io) {
            AppLogger.d(TAG, "Loading episode detail for id: $episodeId")
            try {
                val episode = repository.getEpisodeById(episodeId)
                _uiState.value = EpisodeDetailUiState(
                    episode = episode,
                    isLoading = false
                )
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error loading episode detail", e)
                _uiState.value = EpisodeDetailUiState(
                    isLoading = false,
                    error = "Erro ao carregar detalhes do episódio"
                )
            }
        }
    }

    fun playEpisode() {
        uiState.value.episode?.let { episode ->
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

                AppLogger.i(TAG, "Playing episode: ${episode.title}")
                audioPlayer.play(episode)
            }
        }
    }
}

data class EpisodeDetailUiState(
    val episode: Episode? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
