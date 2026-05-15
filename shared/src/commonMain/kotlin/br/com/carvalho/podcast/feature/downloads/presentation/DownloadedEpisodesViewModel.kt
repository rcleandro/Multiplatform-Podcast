package br.com.carvalho.podcast.feature.downloads.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DownloadedEpisodesViewModel(
    private val repository: PodcastRepository,
    private val episodeDownloader: EpisodeDownloader,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    val playerState = audioPlayer.playerState
    val activeDownloads = episodeDownloader.activeDownloads

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val episodes: StateFlow<List<Episode>> = repository.getDownloadedEpisodes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun playEpisode(episode: Episode) {
        viewModelScope.launch {
            val resolvedEpisode = episode.copy(localPath = episodeDownloader.getLocalPath(episode.id))
            audioPlayer.setQueue(episodes.value)
            audioPlayer.play(resolvedEpisode)
        }
    }

    fun deleteDownload(episodeId: String) {
        viewModelScope.launch {
            episodeDownloader.delete(episodeId)
            _snackbarMessage.value = "Download excluído"
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}
