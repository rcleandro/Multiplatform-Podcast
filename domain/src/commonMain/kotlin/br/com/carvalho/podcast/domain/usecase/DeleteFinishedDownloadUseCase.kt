package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository

/** "Delete episodes when finished" (18.5, a part of 18.19 brought forward): a heard download frees its space. */
class DeleteFinishedDownloadUseCase(
    private val podcastRepository: PodcastRepository,
    private val downloader: EpisodeDownloader,
    private val preferences: PreferencesRepository,
) {
    suspend operator fun invoke(finished: Episode) {
        if (!preferences.deleteFinishedDownloads.value) return
        // Read again: the player holds the episode as it was when it started.
        if (podcastRepository.getEpisodeById(finished.id)?.isDownloaded == true) downloader.delete(finished.id)
    }
}
