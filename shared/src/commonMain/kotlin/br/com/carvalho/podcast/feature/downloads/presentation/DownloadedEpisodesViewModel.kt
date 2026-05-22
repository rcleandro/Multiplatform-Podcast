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

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _deleteEpisodeConfirmation = MutableStateFlow<Episode?>(null)
    val deleteEpisodeConfirmation: StateFlow<Episode?> = _deleteEpisodeConfirmation.asStateFlow()

    val episodes: StateFlow<List<Episode>> = repository.getDownloadedEpisodes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
            audioPlayer.setQueue(episodes.value)
            audioPlayer.play(resolvedEpisode)
        }
    }

    fun deleteDownload(episodeId: String) {
        _deleteEpisodeConfirmation.value = null
        viewModelScope.launch(ioDispatcher()) {
            episodeDownloader.delete(episodeId)
            _snackbarMessage.value = "Download excluído"
        }
    }

    fun showDeleteConfirmation(episode: Episode) {
        _deleteEpisodeConfirmation.value = episode
    }

    fun hideDeleteConfirmation() {
        _deleteEpisodeConfirmation.value = null
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}
