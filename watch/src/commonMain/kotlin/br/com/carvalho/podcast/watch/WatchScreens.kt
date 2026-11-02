package br.com.carvalho.podcast.watch

import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.DirectoryResult
import br.com.carvalho.podcast.domain.usecase.ExplicitContent
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import br.com.carvalho.podcast.domain.usecase.SearchPodcastsUseCase
import br.com.carvalho.podcast.presentation.component.remainingDuration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Episodes a podcast's list on the watch shows, newest first unless the podcast says otherwise. */
private const val EPISODE_LIMIT = 100

/**
 * Where an episode stands on the watch lists (25.14b), as the Wear OS EpisodeButton (19.9g): playing, downloading
 * (with its progress once known), heard, how much is left, or its length. Swift names each with the shared strings.
 */
sealed interface EpisodeStatus {
    data object Playing : EpisodeStatus

    data class Downloading(val progress: Float?) : EpisodeStatus

    data object Played : EpisodeStatus

    data class Remaining(val time: String) : EpisodeStatus

    data class Length(val time: String) : EpisodeStatus

    data object Unknown : EpisodeStatus
}

/** A row of an episode list: the episode, where it stands, and whether it is on the watch. */
data class WatchEpisode(val episode: Episode, val status: EpisodeStatus, val isOnWatch: Boolean)

fun episodeStatus(episode: Episode, isPlaying: Boolean, download: DownloadStatus?): EpisodeStatus {
    val remaining = episode.remainingDuration()
    return when {
        isPlaying -> EpisodeStatus.Playing
        download is DownloadStatus.Downloading -> EpisodeStatus.Downloading(download.progress)
        download is DownloadStatus.Queued -> EpisodeStatus.Downloading(null)
        episode.isPlayed -> EpisodeStatus.Played
        remaining != null -> EpisodeStatus.Remaining(remaining)
        episode.duration > 0 -> EpisodeStatus.Length(episode.duration.toDuration())
        else -> EpisodeStatus.Unknown
    }
}

/** The watch's home (19.9a): the episode playing and the library, nothing explicit under the ECA Digital (26.3). */
class WatchLibrary(
    private val repository: PodcastRepository,
    private val player: AudioPlayer,
    private val explicitContent: ExplicitContent = ExplicitContent(),
) {
    val podcasts: Flow<List<Podcast>> =
        combine(repository.getPodcasts(), explicitContent.rule) { podcasts, rule -> podcasts.filter(rule::allows) }

    val playing: Flow<Episode?> = player.playerState.map { it.currentEpisode }.distinctUntilChanged()
}

/** A podcast's page on the watch: its title and its episodes, each with where it stands. */
data class WatchPodcastState(val title: String, val episodes: List<WatchEpisode>)

class WatchPodcast(
    private val podcastId: String,
    private val repository: PodcastRepository,
    private val player: AudioPlayer,
    private val downloader: EpisodeDownloader,
    private val explicitContent: ExplicitContent = ExplicitContent(),
) {
    val state: Flow<WatchPodcastState> = combine(
        repository.getPodcastByIdFlow(podcastId),
        repository.getEpisodes(podcastId),
        player.playerState.map { state -> state.currentEpisode?.id?.takeIf { state.isPlaying } }.distinctUntilChanged(),
        downloader.activeDownloads,
        explicitContent.rule,
    ) { podcast, episodes, playingId, downloads, rule ->
        val ordered = episodes.filter(rule::allows).sortedBy { it.publishDate }
            .let { if (podcast?.episodesOldestFirst == true) it else it.reversed() }
            .take(EPISODE_LIMIT)
        WatchPodcastState(
            title = podcast?.title.orEmpty(),
            episodes = ordered.map { episode ->
                val download = downloads[episode.id]
                val status = episodeStatus(episode, episode.id == playingId, download)
                WatchEpisode(episode, status, episode.isDownloaded && status !is EpisodeStatus.Downloading)
            },
        )
    }

    /** Plays [episode], or pauses and resumes it when it is the one playing. */
    suspend fun play(episode: Episode) = PlayEpisodeUseCase(player)(episode)
}

/** The popular podcasts, subscribed with a tap (19.9a): on the watch there is no keyboard for a feed address. */
data class WatchPopularState(
    val results: List<DirectoryResult> = emptyList(),
    val subscribing: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val failed: Boolean = false,
)

class WatchPopular(
    private val search: SearchPodcastsUseCase,
    private val addPodcast: AddPodcastFromUrlUseCase,
) {
    private val _state = MutableStateFlow(WatchPopularState())
    val state: StateFlow<WatchPopularState> = _state.asStateFlow()

    suspend fun load() {
        _state.update { it.copy(isLoading = true, failed = false) }
        val results = search.top(category = null)
        _state.update {
            it.copy(results = results.getOrDefault(emptyList()), isLoading = false, failed = results.isFailure)
        }
    }

    /** While the feed is read the row says "Subscribing…": a tap must show it was heard. */
    suspend fun subscribe(result: DirectoryResult) {
        val feedUrl = result.podcast.feedUrl
        if (result.isSubscribed || feedUrl in _state.value.subscribing) return
        _state.update { it.copy(subscribing = it.subscribing + feedUrl) }
        val added = addPodcast(feedUrl).isSuccess
        _state.update { state ->
            state.copy(
                subscribing = state.subscribing - feedUrl,
                results = state.results.map {
                    if (added && it.podcast.feedUrl == feedUrl) it.copy(isSubscribed = true) else it
                },
            )
        }
    }
}
