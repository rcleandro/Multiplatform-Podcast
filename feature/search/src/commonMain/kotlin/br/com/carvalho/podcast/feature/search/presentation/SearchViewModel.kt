package br.com.carvalho.podcast.feature.search.presentation

import androidx.lifecycle.ViewModel
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeListFilter
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.download_deleted
import kotlinx.coroutines.channels.Channel
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.presentation.UiMessage
import br.com.carvalho.podcast.presentation.failureMessages
import br.com.carvalho.podcast.presentation.component.OlderMark
import androidx.paging.PagingData
import androidx.paging.cachedIn
import br.com.carvalho.podcast.core.observability.Analytics
import br.com.carvalho.podcast.core.observability.AnalyticsEvent
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.core.util.NetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

private const val TAG = "SearchViewModel"

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList") // one dependency per thing the tab does (list, download, play, offline start)
class SearchViewModel(
    private val repository: PodcastRepository,
    private val episodeDownloader: EpisodeDownloader,
    audioPlayer: AudioPlayer,
    private val playEpisode: PlayEpisodeUseCase,
    networkMonitor: NetworkMonitor,
    private val dispatchers: CoroutineDispatchers,
    private val analytics: Analytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    val activeDownloads = episodeDownloader.activeDownloads

    private val removed = Channel<UiMessage>(Channel.BUFFERED)

    /** Why a download failed, and that one was removed, shown as a snackbar. */
    val messages: Flow<UiMessage> = merge(episodeDownloader.failureMessages(), removed.receiveAsFlow())
    val playerState = audioPlayer.playerState

    val pagedResults: Flow<PagingData<Episode>> = _uiState.map { it.searchQuery to it.filter }
        .distinctUntilChanged()
        .combine(_refreshTrigger) { search, _ -> search }
        .debounce(AppConfig.SEARCH_DEBOUNCE_MS)
        .flatMapLatest { (query, filter) ->
            AppLogger.d(TAG, "Episodes query or filter changed: $query, $filter")
            repository.searchEpisodesPaged(query.takeIf { it.isNotBlank() }, filter)
        }
        .cachedIn(viewModelScope)

    init {
        // The downloaded list changes when a download ends or is removed, which is when the space changes too.
        repository.getDownloadedEpisodes()
            .onEach { _uiState.update { it.copy(usedBytes = episodeDownloader.usedBytes()) } }
            .launchIn(viewModelScope)
        // Without network the downloaded episodes are the ones that play (ADR 0005).
        viewModelScope.launch {
            if (!networkMonitor.isOnline()) _uiState.update { it.copy(filter = EpisodeListFilter.DOWNLOADED) }
        }
    }

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.ChangeQuery -> _uiState.update { it.copy(searchQuery = intent.query) }
            is SearchIntent.ChangeFilter -> _uiState.update { it.copy(filter = intent.filter) }
            SearchIntent.Refresh -> _refreshTrigger.value += 1
            is SearchIntent.Play -> play(intent.episode)
            is SearchIntent.Download -> downloadEpisode(intent.episode)
            is SearchIntent.CancelDownload -> cancelDownload(intent.episode.id)
            is SearchIntent.RequestDeleteDownload ->
                _uiState.update { it.copy(deleteEpisodeConfirmation = intent.episode) }
            is SearchIntent.ConfirmDeleteDownload -> deleteDownload(intent.episode.id)
            SearchIntent.DismissDeleteDownload -> _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
            is SearchIntent.SetPlayed -> setPlayed(intent.episode, intent.played)
            is SearchIntent.RequestMarkOlder -> _uiState.update { it.copy(olderMark = intent.mark) }
            SearchIntent.DismissMarkOlder -> _uiState.update { it.copy(olderMark = null) }
            is SearchIntent.ConfirmMarkOlder -> markOlder(intent.mark)
        }
    }

    private fun setPlayed(episode: Episode, played: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            if (played) repository.markEpisodeAsPlayed(episode.id) else repository.markEpisodeAsUnplayed(episode.id)
        }
    }

    private fun markOlder(mark: OlderMark) {
        _uiState.update { it.copy(olderMark = null) }
        val episode = mark.episode
        viewModelScope.launch(dispatchers.io) {
            if (mark.played) {
                repository.markOlderEpisodesAsPlayed(episode.podcastId, episode.publishDate)
            } else {
                repository.markOlderEpisodesAsUnplayed(episode.podcastId, episode.publishDate)
            }
        }
    }

    private fun play(episode: Episode) {
        viewModelScope.launch(dispatchers.io) {
            analytics.logEvent(AnalyticsEvent.PlayEpisode(episode.id, AnalyticsEvent.PlaySource.SEARCH))
            playEpisode(episode)
        }
    }

    private fun downloadEpisode(episode: Episode) {
        viewModelScope.launch(dispatchers.io) {
            AppLogger.i(TAG, "Starting download for episode from search: ${episode.title}")
            episodeDownloader.download(episode)
        }
    }

    private fun cancelDownload(episodeId: String) {
        viewModelScope.launch(dispatchers.io) {
            episodeDownloader.cancel(episodeId)
        }
    }

    private fun deleteDownload(episodeId: String) {
        _uiState.update { it.copy(deleteEpisodeConfirmation = null) }
        viewModelScope.launch(dispatchers.io) {
            episodeDownloader.delete(episodeId)
            removed.send(UiMessage(Res.string.download_deleted))
        }
    }
}

data class SearchUiState(
    val searchQuery: String = "",
    val filter: EpisodeListFilter = EpisodeListFilter.ALL,
    /** Disk space the downloads take, shown with the "Downloaded" filter. */
    val usedBytes: Long = 0,
    val deleteEpisodeConfirmation: Episode? = null,
    /** Marking many episodes at once, waiting for confirmation. */
    val olderMark: OlderMark? = null,
)

sealed interface SearchIntent {
    data class ChangeQuery(val query: String) : SearchIntent
    data class ChangeFilter(val filter: EpisodeListFilter) : SearchIntent
    data object Refresh : SearchIntent
    data class Play(val episode: Episode) : SearchIntent
    data class Download(val episode: Episode) : SearchIntent
    data class CancelDownload(val episode: Episode) : SearchIntent
    data class RequestDeleteDownload(val episode: Episode) : SearchIntent
    data class ConfirmDeleteDownload(val episode: Episode) : SearchIntent
    data object DismissDeleteDownload : SearchIntent
    data class SetPlayed(val episode: Episode, val played: Boolean) : SearchIntent
    data class RequestMarkOlder(val mark: OlderMark) : SearchIntent
    data class ConfirmMarkOlder(val mark: OlderMark) : SearchIntent
    data object DismissMarkOlder : SearchIntent
}
