package br.com.carvalho.podcast.feature.downloads.presentation

import br.com.carvalho.podcast.core.ui.generated.resources.download_deleted
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DownloadedEpisodesViewModel(
    repository: PodcastRepository,
    private val episodeDownloader: EpisodeDownloader,
    private val audioPlayer: AudioPlayer,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    val playerState = audioPlayer.playerState
    val activeDownloads = episodeDownloader.activeDownloads

    private val _uiState = MutableStateFlow(DownloadedEpisodesUiState())
    val uiState: StateFlow<DownloadedEpisodesUiState> = _uiState.asStateFlow()

    private val _messages = Channel<StringResource>(Channel.BUFFERED)
    val messages: Flow<StringResource> = _messages.receiveAsFlow()

    init {
        repository.getDownloadedEpisodes()
            .onEach { episodes ->
                _uiState.update { it.copy(episodes = episodes) }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: DownloadsIntent) {
        when (intent) {
            is DownloadsIntent.Play -> playEpisode(intent.episode)
            is DownloadsIntent.RequestDelete -> _uiState.update { it.copy(deleteEpisodeConfirmation = intent.episode) }
            is DownloadsIntent.ConfirmDelete -> deleteDownload(intent.episode.id)
            DownloadsIntent.DismissDelete -> _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
        }
    }

    private fun playEpisode(episode: Episode) {
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

            val resolvedEpisode = episode.copy(localPath = episodeDownloader.getLocalPath(episode.id))
            audioPlayer.setQueue(uiState.value.episodes)
            audioPlayer.play(resolvedEpisode)
        }
    }

    private fun deleteDownload(episodeId: String) {
        _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
        viewModelScope.launch(dispatchers.io) {
            episodeDownloader.delete(episodeId)
            _messages.send(Res.string.download_deleted)
        }
    }
}

data class DownloadedEpisodesUiState(
    val episodes: List<Episode> = emptyList(),
    val deleteEpisodeConfirmation: Episode? = null,
)

sealed interface DownloadsIntent {
    data class Play(val episode: Episode) : DownloadsIntent
    data class RequestDelete(val episode: Episode) : DownloadsIntent
    data class ConfirmDelete(val episode: Episode) : DownloadsIntent
    data object DismissDelete : DownloadsIntent
}
