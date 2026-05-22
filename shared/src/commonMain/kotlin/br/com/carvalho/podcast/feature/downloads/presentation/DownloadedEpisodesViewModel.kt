package br.com.carvalho.podcast.feature.downloads.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import io.ktor.utils.io.ioDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DownloadedEpisodesViewModel(
    repository: PodcastRepository,
    private val episodeDownloader: EpisodeDownloader,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    val playerState = audioPlayer.playerState
    val activeDownloads = episodeDownloader.activeDownloads

    private val _uiState = MutableStateFlow(DownloadedEpisodesUiState())
    val uiState: StateFlow<DownloadedEpisodesUiState> = _uiState.asStateFlow()

    init {
        repository.getDownloadedEpisodes()
            .onEach { episodes ->
                _uiState.update { it.copy(episodes = episodes) }
            }
            .launchIn(viewModelScope)
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

            val resolvedEpisode = episode.copy(localPath = episodeDownloader.getLocalPath(episode.id))
            audioPlayer.setQueue(uiState.value.episodes)
            audioPlayer.play(resolvedEpisode)
        }
    }

    fun deleteDownload(episodeId: String) {
        _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
        viewModelScope.launch(ioDispatcher()) {
            episodeDownloader.delete(episodeId)
            _uiState.update { it.copy(snackbarMessage = "Download excluído") }
        }
    }

    fun showDeleteConfirmation(episode: Episode) {
        _uiState.update { it.copy(deleteEpisodeConfirmation = episode) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}

data class DownloadedEpisodesUiState(
    val episodes: List<Episode> = emptyList(),
    val deleteEpisodeConfirmation: Episode? = null,
    val snackbarMessage: String? = null
)
