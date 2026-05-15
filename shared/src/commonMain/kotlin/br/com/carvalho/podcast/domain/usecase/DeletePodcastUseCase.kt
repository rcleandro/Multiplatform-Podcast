package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.core.util.AppLogger

private const val TAG = "DeleteUseCase"

class DeletePodcastUseCase(
    private val podcastRepository: PodcastRepository
) {
    suspend operator fun invoke(podcastId: String) {
        AppLogger.i(TAG, "Deleting podcast with id: $podcastId")
        podcastRepository.deletePodcast(podcastId)
    }
}
