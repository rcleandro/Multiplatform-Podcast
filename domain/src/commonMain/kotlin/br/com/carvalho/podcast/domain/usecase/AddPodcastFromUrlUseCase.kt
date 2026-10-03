package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FeedSource
import br.com.carvalho.podcast.domain.repository.PodcastRepository

class AddPodcastFromUrlUseCase(
    private val feedSource: FeedSource,
    private val podcastRepository: PodcastRepository
) {
    suspend operator fun invoke(url: String): Result<Podcast> {
        if (podcastRepository.getPodcastById(url) != null) {
            return Result.failure(AppError.AlreadyExists)
        }

        return feedSource.fetch(url)
            .mapCatching { feed ->
                podcastRepository.savePodcast(feed.podcast)
                podcastRepository.saveEpisodes(feed.episodes)
                feed.podcast
            }
    }
}
