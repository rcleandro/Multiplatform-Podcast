package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.first

private const val TAG = "DeleteUseCase"

class DeletePodcastUseCase(
    private val podcastRepository: PodcastRepository,
    private val episodeDownloader: EpisodeDownloader,
) {
    /** Removes the podcast and, first, its downloaded or downloading files, which would otherwise stay on disk. */
    suspend operator fun invoke(podcastId: String) {
        AppLogger.i(TAG, "Deleting podcast with id: $podcastId")
        val active = episodeDownloader.activeDownloads.value.keys
        podcastRepository.getEpisodes(podcastId).first()
            .filter { it.isDownloaded || it.id in active }
            .forEach { episodeDownloader.cancel(it.id) }
        podcastRepository.deletePodcast(podcastId)
    }
}
