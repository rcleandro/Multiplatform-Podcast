package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.PodcastRepository

/** What can be done to several selected episodes at once (18.11). */
enum class EpisodeAction { ADD_TO_QUEUE, DOWNLOAD, MARK_PLAYED, MARK_UNPLAYED }

/**
 * Does [EpisodeAction] to each episode, in the order given. Queued with nothing playing, the first one plays and the
 * rest follow it; an episode already downloaded is not downloaded again.
 */
class ActOnEpisodesUseCase(
    private val repository: PodcastRepository,
    private val downloader: EpisodeDownloader,
    private val enqueue: EnqueueEpisodeUseCase,
) {
    suspend operator fun invoke(episodes: List<Episode>, action: EpisodeAction) {
        when (action) {
            EpisodeAction.ADD_TO_QUEUE -> episodes.forEach { enqueue(it, next = false) }
            EpisodeAction.DOWNLOAD -> episodes.filterNot { it.isDownloaded }.forEach { downloader.download(it) }
            EpisodeAction.MARK_PLAYED -> episodes.forEach { repository.markEpisodeAsPlayed(it.id) }
            EpisodeAction.MARK_UNPLAYED -> episodes.forEach { repository.markEpisodeAsUnplayed(it.id) }
        }
    }
}
