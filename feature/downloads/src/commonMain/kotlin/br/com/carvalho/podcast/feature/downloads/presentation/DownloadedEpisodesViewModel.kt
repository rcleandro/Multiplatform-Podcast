package br.com.carvalho.podcast.feature.downloads.presentation

import br.com.carvalho.podcast.core.ui.generated.resources.download_deleted
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import androidx.lifecycle.ViewModel
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import br.com.carvalho.podcast.presentation.UiMessage
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
    audioPlayer: AudioPlayer,
    private val playEpisode: PlayEpisodeUseCase,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    val playerState = audioPlayer.playerState
    val activeDownloads = episodeDownloader.activeDownloads

    private val _uiState = MutableStateFlow(DownloadedEpisodesUiState())
    val uiState: StateFlow<DownloadedEpisodesUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    init {
        repository.getDownloadedEpisodes()
            .onEach { episodes ->
                // The list changes when a download ends or is deleted, which is when the space changes too.
                val usedBytes = episodeDownloader.usedBytes()
                _uiState.update { it.copy(episodes = episodes, usedBytes = usedBytes) }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: DownloadsIntent) {
        when (intent) {
            is DownloadsIntent.Play -> play(intent.episode)
            is DownloadsIntent.RequestDelete -> _uiState.update { it.copy(deleteEpisodeConfirmation = intent.episode) }
            is DownloadsIntent.ConfirmDelete -> deleteDownload(intent.episode.id)
            DownloadsIntent.DismissDelete -> _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
        }
    }

    private fun play(episode: Episode) {
        viewModelScope.launch(dispatchers.io) { playEpisode(episode, queue = uiState.value.episodes) }
    }

    private fun deleteDownload(episodeId: String) {
        _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
        viewModelScope.launch(dispatchers.io) {
            episodeDownloader.delete(episodeId)
            _messages.send(UiMessage(Res.string.download_deleted))
        }
    }
}

data class DownloadedEpisodesUiState(
    val episodes: List<Episode> = emptyList(),
    val deleteEpisodeConfirmation: Episode? = null,
    /** Disk space the downloads take. */
    val usedBytes: Long = 0,
)

sealed interface DownloadsIntent {
    data class Play(val episode: Episode) : DownloadsIntent
    data class RequestDelete(val episode: Episode) : DownloadsIntent
    data class ConfirmDelete(val episode: Episode) : DownloadsIntent
    data object DismissDelete : DownloadsIntent
}
