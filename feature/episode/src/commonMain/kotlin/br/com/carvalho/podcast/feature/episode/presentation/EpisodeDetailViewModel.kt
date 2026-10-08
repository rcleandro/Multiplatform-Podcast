package br.com.carvalho.podcast.feature.episode.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.AnalyticsEvent
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "EpisodeDetailViewModel"

@Suppress("LongParameterList") // one dependency per thing the screen does (show, play, download, mark)
class EpisodeDetailViewModel(
    private val episodeId: String,
    private val repository: PodcastRepository,
    private val playEpisode: PlayEpisodeUseCase,
    private val audioPlayer: AudioPlayer,
    private val downloader: EpisodeDownloader,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpisodeDetailUiState(isLoading = true))
    val uiState: StateFlow<EpisodeDetailUiState> = _uiState.asStateFlow()

    init {
        loadEpisode()
        // Whether this episode is the one playing, and how its download goes, follow the player and the downloader.
        combine(audioPlayer.playerState, downloader.activeDownloads) { player, downloads ->
            val isCurrent = player.currentEpisode?.id == episodeId
            Triple(isCurrent && player.isPlaying, isCurrent && player.isBuffering, downloads[episodeId])
        }.onEach { (playing, buffering, download) ->
            _uiState.update {
                it.copy(isPlaying = playing, isBuffering = buffering, downloadStatus = download ?: DownloadStatus.Idle)
            }
        }.launchIn(viewModelScope)
    }

    fun onIntent(intent: EpisodeDetailIntent) {
        when (intent) {
            EpisodeDetailIntent.PlayPause -> playPause()
            EpisodeDetailIntent.Retry -> loadEpisode()
            EpisodeDetailIntent.Download -> withEpisode { downloader.download(it) }
            EpisodeDetailIntent.CancelDownload -> withEpisode { downloader.cancel(it.id) }
            EpisodeDetailIntent.DeleteDownload -> withEpisode { downloader.delete(it.id) }
            EpisodeDetailIntent.MarkPlayed -> withEpisode {
                repository.markEpisodeAsPlayed(it.id)
                reload()
            }
            EpisodeDetailIntent.MarkUnplayed -> withEpisode {
                repository.markEpisodeAsUnplayed(it.id)
                reload()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught") // any database failure shows the error state with a retry
    private fun loadEpisode() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent(AnalyticsEvent.LoadEpisodeDetail(episodeId))
            AppLogger.d(TAG, "Loading episode detail for id: $episodeId")
            try {
                val episode = repository.getEpisodeById(episodeId)
                _uiState.update { it.copy(episode = episode, isLoading = false) }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error loading episode detail", e)
                analytics.logEvent(AnalyticsEvent.LoadEpisodeDetailError(episodeId, e))
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    private suspend fun reload() {
        _uiState.update { it.copy(episode = repository.getEpisodeById(episodeId)) }
    }

    // Pause this episode while it plays, pick it up where it paused, or start it.
    private fun playPause() {
        val state = uiState.value
        when {
            state.isPlaying -> audioPlayer.pause()
            audioPlayer.playerState.value.currentEpisode?.id == episodeId -> audioPlayer.resume()
            else -> withEpisode {
                analytics.logEvent(AnalyticsEvent.PlayEpisode(it.id, AnalyticsEvent.PlaySource.EPISODE))
                playEpisode(it)
            }
        }
    }

    private fun withEpisode(block: suspend (Episode) -> Unit) {
        val episode = uiState.value.episode ?: return
        viewModelScope.launch(dispatchers.io) { block(episode) }
    }
}

data class EpisodeDetailUiState(
    val episode: Episode? = null,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val downloadStatus: DownloadStatus = DownloadStatus.Idle,
)

sealed interface EpisodeDetailIntent {
    data object PlayPause : EpisodeDetailIntent
    data object Retry : EpisodeDetailIntent
    data object Download : EpisodeDetailIntent
    data object CancelDownload : EpisodeDetailIntent
    data object DeleteDownload : EpisodeDetailIntent
    data object MarkPlayed : EpisodeDetailIntent
    data object MarkUnplayed : EpisodeDetailIntent
}
