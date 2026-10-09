package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first

/**
 * "Download the next episode when one ends" (18.5, a part of 18.19 brought forward): once [finished] ends, the
 * podcast's newest episode is downloaded; when it is already downloaded or heard, the next one in the [queue] that is
 * neither. Only with the setting on, and through the downloader, which keeps to Wi-Fi only and the space limit.
 */
class DownloadNextEpisodeUseCase(
    private val podcastRepository: PodcastRepository,
    private val downloader: EpisodeDownloader,
    private val preferences: PreferencesRepository,
) {
    suspend operator fun invoke(finished: Episode, queue: List<Episode>) {
        if (!preferences.downloadNextOnFinish.value) return
        val newest = podcastRepository.getEpisodes(finished.podcastId).first().maxByOrNull { it.publishDate }
        val afterFinished = queue.dropWhile { it.id != finished.id }.drop(1)
        for (candidate in listOfNotNull(newest) + afterFinished) {
            // Read again: the queue may hold an episode as it was before it was downloaded or heard.
            val current = podcastRepository.getEpisodeById(candidate.id)
            if (current != null && current.id != finished.id && current.isWanted()) {
                downloader.download(current)
                return
            }
        }
    }

    private fun Episode.isWanted(): Boolean {
        val status = downloader.activeDownloads.value[id]
        val inProgress = status is DownloadStatus.Queued || status is DownloadStatus.Downloading
        return !isDownloaded && !isPlayed && !inProgress
    }
}
