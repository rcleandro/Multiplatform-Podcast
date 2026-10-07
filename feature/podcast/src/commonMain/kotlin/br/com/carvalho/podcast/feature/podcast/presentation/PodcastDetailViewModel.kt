package br.com.carvalho.podcast.feature.podcast.presentation

import br.com.carvalho.podcast.core.ui.generated.resources.error_refresh_episodes
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import androidx.lifecycle.ViewModel
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import br.com.carvalho.podcast.presentation.UiMessage
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.presentation.toMessage
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.presentation.failureMessages
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import androidx.paging.PagingData
import androidx.paging.cachedIn
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.urlHost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "PodcastDetailViewModel"

@Suppress("LongParameterList") // constructor injection; the screen genuinely needs all of them
class PodcastDetailViewModel(
    private val podcastId: String,
    audioPlayer: AudioPlayer,
    private val playEpisode: PlayEpisodeUseCase,
    private val refreshPodcastUseCase: RefreshPodcastUseCase,
    private val episodeDownloader: EpisodeDownloader,
    private val repository: PodcastRepository,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics
) : ViewModel() {

    val playerState = audioPlayer.playerState
    val activeDownloads = episodeDownloader.activeDownloads

    private val _uiState = MutableStateFlow(PodcastDetailUiState(isLoading = true))
    val uiState: StateFlow<PodcastDetailUiState> = _uiState

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = merge(_messages.receiveAsFlow(), episodeDownloader.failureMessages())

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedEpisodes: Flow<PagingData<Episode>> = _uiState
        .map { it.filter }
        .distinctUntilChanged()
        .flatMapLatest { filter -> repository.getEpisodesPaged(podcastId, filter) }
        .cachedIn(viewModelScope)

    init {
        repository.getPodcastByIdFlow(podcastId)
            .onEach { podcast -> _uiState.update { it.copy(podcast = podcast, isLoading = false) } }
            .launchIn(viewModelScope)
        combine(repository.getEpisodeCount(podcastId), repository.getLatestUnplayedEpisode(podcastId), ::Pair)
            .onEach { (count, latest) -> _uiState.update { it.copy(episodeCount = count, latestUnplayed = latest) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: PodcastDetailIntent) {
        when (intent) {
            PodcastDetailIntent.Refresh -> refresh()
            is PodcastDetailIntent.SetFilter -> setFilter(intent.filter)
            is PodcastDetailIntent.Play -> play(intent.episode)
            is PodcastDetailIntent.Download -> downloadEpisode(intent.episode)
            is PodcastDetailIntent.CancelDownload -> cancelDownload(intent.episode.id)
            is PodcastDetailIntent.RequestDeleteDownload ->
                _uiState.update { it.copy(deleteEpisodeConfirmation = intent.episode) }
            is PodcastDetailIntent.ConfirmDeleteDownload -> deleteDownload(intent.episode.id)
            PodcastDetailIntent.DismissDeleteDownload -> _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
            is PodcastDetailIntent.SelectEpisode -> _uiState.update { it.copy(selectedEpisode = intent.episode) }
            PodcastDetailIntent.DismissMarkPlayed -> _uiState.update { it.copy(selectedEpisode = null) }
            is PodcastDetailIntent.MarkPlayed -> markAsPlayed(intent.episode.id)
            is PodcastDetailIntent.MarkUnplayed -> markAsUnplayed(intent.episode.id)
            is PodcastDetailIntent.MarkOlderPlayed -> markOlderAsPlayed(intent.episode.publishDate)
        }
    }

    private fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent("refresh_podcast_detail", mapOf("host" to urlHost(podcastId)))
            AppLogger.i(TAG, "Refreshing podcast details for id: $podcastId")
            refreshPodcastUseCase(podcastId).onFailure { e ->
                AppLogger.e(TAG, "Error refreshing podcast $podcastId", e)
                _messages.send(UiMessage(e.toMessage(fallback = Res.string.error_refresh_episodes)))
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun setFilter(filter: EpisodeFilter) {
        analytics.logEvent("set_episode_filter", mapOf("filter" to filter.name))
        _uiState.update { it.copy(filter = filter) }
    }

    private fun play(episode: Episode) {
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent("play_episode_from_detail", mapOf("episode_id" to episode.id))
            playEpisode(episode)
        }
    }

    private fun downloadEpisode(episode: Episode) {
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent("download_episode_from_detail", mapOf("episode_id" to episode.id))
            AppLogger.i(TAG, "Starting download for episode: ${episode.title}")
            episodeDownloader.download(episode)
        }
    }

    private fun cancelDownload(episodeId: String) {
        viewModelScope.launch(dispatchers.io) {
            episodeDownloader.cancel(episodeId)
        }
    }

    private fun deleteDownload(episodeId: String) {
        analytics.logEvent("delete_download_from_detail", mapOf("episode_id" to episodeId))
        _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
        viewModelScope.launch(dispatchers.io) {
            episodeDownloader.delete(episodeId)
        }
    }

    private fun markAsPlayed(episodeId: String) {
        analytics.logEvent("mark_as_played", mapOf("episode_id" to episodeId))
        _uiState.update { it.copy(selectedEpisode = null) }
        viewModelScope.launch(dispatchers.io) {
            repository.markEpisodeAsPlayed(episodeId)
        }
    }

    private fun markAsUnplayed(episodeId: String) {
        analytics.logEvent("mark_as_unplayed", mapOf("episode_id" to episodeId))
        viewModelScope.launch(dispatchers.io) {
            repository.markEpisodeAsUnplayed(episodeId)
        }
    }

    private fun markOlderAsPlayed(publishDate: Long) {
        analytics.logEvent("mark_older_as_played", mapOf("host" to urlHost(podcastId), "publish_date" to publishDate))
        _uiState.update { it.copy(selectedEpisode = null) }
        viewModelScope.launch(dispatchers.io) {
            repository.markOlderEpisodesAsPlayed(podcastId, publishDate)
        }
    }
}


data class PodcastDetailUiState(
    val episodeCount: Int = 0,
    /** What "Latest" plays; no button while every episode was heard. */
    val latestUnplayed: Episode? = null,
    val podcast: Podcast? = null,
    val filter: EpisodeFilter = EpisodeFilter.ALL,
    val selectedEpisode: Episode? = null,
    val deleteEpisodeConfirmation: Episode? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
)

sealed interface PodcastDetailIntent {
    data object Refresh : PodcastDetailIntent
    data class SetFilter(val filter: EpisodeFilter) : PodcastDetailIntent
    data class Play(val episode: Episode) : PodcastDetailIntent
    data class Download(val episode: Episode) : PodcastDetailIntent
    data class CancelDownload(val episode: Episode) : PodcastDetailIntent
    data class RequestDeleteDownload(val episode: Episode) : PodcastDetailIntent
    data class ConfirmDeleteDownload(val episode: Episode) : PodcastDetailIntent
    data object DismissDeleteDownload : PodcastDetailIntent
    data class SelectEpisode(val episode: Episode) : PodcastDetailIntent
    data object DismissMarkPlayed : PodcastDetailIntent
    data class MarkPlayed(val episode: Episode) : PodcastDetailIntent
    data class MarkUnplayed(val episode: Episode) : PodcastDetailIntent
    data class MarkOlderPlayed(val episode: Episode) : PodcastDetailIntent
}
