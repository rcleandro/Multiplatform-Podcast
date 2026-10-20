package br.com.carvalho.podcast.feature.podcast.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.AnalyticsEvent
import br.com.carvalho.podcast.core.observability.urlHost
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.error_refresh_episodes
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastPlayback
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.ActOnEpisodesUseCase
import br.com.carvalho.podcast.domain.usecase.EnqueueEpisodeUseCase
import br.com.carvalho.podcast.domain.usecase.EpisodeAction
import br.com.carvalho.podcast.domain.usecase.ExplicitContent
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.presentation.UiMessage
import br.com.carvalho.podcast.presentation.component.OlderMark
import br.com.carvalho.podcast.presentation.component.message
import br.com.carvalho.podcast.presentation.component.toggled
import br.com.carvalho.podcast.presentation.failureMessages
import br.com.carvalho.podcast.presentation.message
import br.com.carvalho.podcast.presentation.toMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PodcastDetailViewModel"

// Constructor injection (the screen needs all of them), and one function per screen intent.
@Suppress("LongParameterList", "TooManyFunctions")
class PodcastDetailViewModel(
    private val podcastId: String,
    audioPlayer: AudioPlayer,
    private val playEpisode: PlayEpisodeUseCase,
    private val enqueueEpisode: EnqueueEpisodeUseCase,
    private val actOnEpisodes: ActOnEpisodesUseCase,
    private val refreshPodcastUseCase: RefreshPodcastUseCase,
    private val episodeDownloader: EpisodeDownloader,
    private val repository: PodcastRepository,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics,
    /** An explicit podcast or episode stays out under Brazil's ECA Digital (26.3). */
    private val explicitContent: ExplicitContent = ExplicitContent(),
) : ViewModel() {

    val playerState = audioPlayer.playerState
    val activeDownloads = episodeDownloader.activeDownloads

    private val _uiState = MutableStateFlow(PodcastDetailUiState(isLoading = true))
    val uiState: StateFlow<PodcastDetailUiState> = _uiState

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = merge(_messages.receiveAsFlow(), episodeDownloader.failureMessages())

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedEpisodes: Flow<PagingData<Episode>> = _uiState
        .map { it.filter to it.oldestFirst }
        .distinctUntilChanged()
        .flatMapLatest { (filter, oldestFirst) -> repository.getEpisodesPaged(podcastId, filter, oldestFirst) }
        .combine(explicitContent.rule) { episodes, rule -> episodes.filter { rule.allows(it) } }
        .cachedIn(viewModelScope)

    init {
        // A hidden podcast opens as one the library does not have.
        combine(repository.getPodcastByIdFlow(podcastId), explicitContent.rule) { podcast, rule ->
            podcast?.takeIf { rule.allows(it) }
        }
            .onEach { podcast -> _uiState.update { it.copy(podcast = podcast, isLoading = false) } }
            .launchIn(viewModelScope)
        val latest = combine(repository.getLatestUnplayedEpisode(podcastId), explicitContent.rule) { episode, rule ->
            episode?.takeIf { rule.allows(it) }
        }
        combine(repository.getEpisodeCount(podcastId), latest, ::Pair)
            .onEach { (count, latest) -> _uiState.update { it.copy(episodeCount = count, latestUnplayed = latest) } }
            .launchIn(viewModelScope)
    }

    @Suppress("CyclomaticComplexMethod") // one branch per intent
    fun onIntent(intent: PodcastDetailIntent) {
        when (intent) {
            is PodcastDetailIntent.ToggleSelection ->
                _uiState.update { it.copy(selection = it.selection.toggled(intent.episode)) }
            PodcastDetailIntent.ClearSelection -> _uiState.update { it.copy(selection = emptyList()) }
            is PodcastDetailIntent.ActOnSelection -> actOnSelection(intent.action)
            is PodcastDetailIntent.SetSaved -> setSaved(intent.episode, intent.saved)
            PodcastDetailIntent.Refresh -> refresh()
            is PodcastDetailIntent.SetFilter -> setFilter(intent.filter)
            is PodcastDetailIntent.SetOldestFirst -> setOldestFirst(intent.oldestFirst)
            is PodcastDetailIntent.SetAutoDownload -> viewModelScope.launch(dispatchers.io) {
                repository.setPodcastAutoDownload(podcastId, intent.count)
                // The refresh downloads what the new choice asks for, after reading the feed once more.
                refreshPodcastUseCase(podcastId)
            }
            is PodcastDetailIntent.SetNotifications -> viewModelScope.launch(dispatchers.io) {
                repository.setPodcastNotifications(podcastId, intent.enabled)
            }
            is PodcastDetailIntent.SetPlayback -> {
                analytics.logEvent(AnalyticsEvent.SetPodcastPlayback)
                viewModelScope.launch(dispatchers.io) { repository.setPodcastPlayback(podcastId, intent.playback) }
            }
            is PodcastDetailIntent.Play -> play(intent.episode)
            is PodcastDetailIntent.Enqueue -> enqueue(intent.episode, intent.next)
            is PodcastDetailIntent.Download -> downloadEpisode(intent.episode)
            is PodcastDetailIntent.CancelDownload -> cancelDownload(intent.episode.id)
            is PodcastDetailIntent.RequestDeleteDownload ->
                _uiState.update { it.copy(deleteEpisodeConfirmation = intent.episode) }
            is PodcastDetailIntent.ConfirmDeleteDownload -> deleteDownload(intent.episode.id)
            PodcastDetailIntent.DismissDeleteDownload -> _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
            is PodcastDetailIntent.RequestMarkOlder -> _uiState.update { it.copy(olderMark = intent.mark) }
            PodcastDetailIntent.DismissMarkOlder -> _uiState.update { it.copy(olderMark = null) }
            is PodcastDetailIntent.MarkPlayed -> markAsPlayed(intent.episode.id)
            is PodcastDetailIntent.MarkUnplayed -> markAsUnplayed(intent.episode.id)
            is PodcastDetailIntent.ConfirmMarkOlder -> markOlder(intent.mark)
        }
    }

    private fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent(AnalyticsEvent.RefreshPodcast(urlHost(podcastId)))
            AppLogger.i(TAG, "Refreshing podcast details for id: $podcastId")
            refreshPodcastUseCase(podcastId).onFailure { e ->
                AppLogger.e(TAG, "Error refreshing podcast $podcastId", e)
                _messages.send(UiMessage(e.toMessage(fallback = Res.string.error_refresh_episodes)))
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun setFilter(filter: EpisodeFilter) {
        analytics.logEvent(AnalyticsEvent.SetEpisodeFilter(filter.name))
        _uiState.update { it.copy(filter = filter) }
    }

    private fun setOldestFirst(oldestFirst: Boolean) {
        analytics.logEvent(AnalyticsEvent.SetEpisodeOrder(oldestFirst))
        viewModelScope.launch(dispatchers.io) { repository.setEpisodeOrder(podcastId, oldestFirst) }
    }

    private fun setSaved(episode: Episode, saved: Boolean) {
        analytics.logEvent(AnalyticsEvent.SaveEpisode(saved))
        viewModelScope.launch(dispatchers.io) { repository.setEpisodeSaved(episode.id, saved) }
    }

    private fun actOnSelection(action: EpisodeAction) {
        val episodes = _uiState.value.selection
        _uiState.update { it.copy(selection = emptyList()) }
        analytics.logEvent(AnalyticsEvent.ActOnEpisodes(action.name, episodes.size))
        viewModelScope.launch(dispatchers.io) {
            actOnEpisodes(episodes, action)
            _messages.send(action.message(episodes.size))
        }
    }

    private fun enqueue(episode: Episode, next: Boolean) = viewModelScope.launch(dispatchers.io) {
        enqueueEpisode(episode, next).message()?.let { _messages.send(it) }
    }

    private fun play(episode: Episode) {
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent(AnalyticsEvent.PlayEpisode(episode.id, AnalyticsEvent.PlaySource.PODCAST))
            playEpisode(episode)
        }
    }

    private fun downloadEpisode(episode: Episode) {
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent(AnalyticsEvent.DownloadEpisode(episode.id))
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
        analytics.logEvent(AnalyticsEvent.DeleteDownload(episodeId))
        _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
        viewModelScope.launch(dispatchers.io) {
            episodeDownloader.delete(episodeId)
        }
    }

    private fun markAsPlayed(episodeId: String) {
        analytics.logEvent(AnalyticsEvent.MarkPlayed(episodeId, played = true))
        viewModelScope.launch(dispatchers.io) {
            repository.markEpisodeAsPlayed(episodeId)
        }
    }

    private fun markAsUnplayed(episodeId: String) {
        analytics.logEvent(AnalyticsEvent.MarkPlayed(episodeId, played = false))
        viewModelScope.launch(dispatchers.io) {
            repository.markEpisodeAsUnplayed(episodeId)
        }
    }

    private fun markOlder(mark: OlderMark) {
        val publishDate = mark.episode.publishDate
        analytics.logEvent(AnalyticsEvent.MarkOlderPlayed(urlHost(podcastId), publishDate, mark.played))
        _uiState.update { it.copy(olderMark = null) }
        viewModelScope.launch(dispatchers.io) {
            if (mark.played) {
                repository.markOlderEpisodesAsPlayed(podcastId, publishDate)
            } else {
                repository.markOlderEpisodesAsUnplayed(podcastId, publishDate)
            }
        }
    }
}

data class PodcastDetailUiState(
    val episodeCount: Int = 0,
    /** What "Latest" plays; no button while every episode was heard. */
    val latestUnplayed: Episode? = null,
    val podcast: Podcast? = null,
    val filter: EpisodeFilter = EpisodeFilter.ALL,
    /** Marking many episodes at once, waiting for confirmation. */
    val olderMark: OlderMark? = null,
    val deleteEpisodeConfirmation: Episode? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    /** The episodes chosen to act on together, in the order chosen; empty when not selecting (18.11). */
    val selection: List<Episode> = emptyList(),
) {
    /** Before the podcast loads, newest first, as most shows want. */
    val oldestFirst: Boolean get() = podcast?.episodesOldestFirst ?: false
}

sealed interface PodcastDetailIntent {
    /** Several episodes chosen at once (18.11): a long press starts, each tap adds or drops one. */
    data class ToggleSelection(val episode: Episode) : PodcastDetailIntent
    data object ClearSelection : PodcastDetailIntent
    data class ActOnSelection(val action: EpisodeAction) : PodcastDetailIntent
    data class SetSaved(val episode: Episode, val saved: Boolean) : PodcastDetailIntent
    data object Refresh : PodcastDetailIntent
    data class SetFilter(val filter: EpisodeFilter) : PodcastDetailIntent
    data class SetOldestFirst(val oldestFirst: Boolean) : PodcastDetailIntent

    /** How many of the newest unheard episodes stay downloaded (18.19). */
    data class SetAutoDownload(val count: Int) : PodcastDetailIntent

    /** Whether this podcast's new episodes notify (18.18). */
    data class SetNotifications(val enabled: Boolean) : PodcastDetailIntent

    /** The podcast's own speed and skipped start and end (18.14). */
    data class SetPlayback(val playback: PodcastPlayback) : PodcastDetailIntent
    data class Play(val episode: Episode) : PodcastDetailIntent

    /** "Play next" when [next], else "Add to queue" (18.6). */
    data class Enqueue(val episode: Episode, val next: Boolean) : PodcastDetailIntent
    data class Download(val episode: Episode) : PodcastDetailIntent
    data class CancelDownload(val episode: Episode) : PodcastDetailIntent
    data class RequestDeleteDownload(val episode: Episode) : PodcastDetailIntent
    data class ConfirmDeleteDownload(val episode: Episode) : PodcastDetailIntent
    data object DismissDeleteDownload : PodcastDetailIntent
    data class RequestMarkOlder(val mark: OlderMark) : PodcastDetailIntent
    data object DismissMarkOlder : PodcastDetailIntent
    data class MarkPlayed(val episode: Episode) : PodcastDetailIntent
    data class MarkUnplayed(val episode: Episode) : PodcastDetailIntent
    data class ConfirmMarkOlder(val mark: OlderMark) : PodcastDetailIntent
}
