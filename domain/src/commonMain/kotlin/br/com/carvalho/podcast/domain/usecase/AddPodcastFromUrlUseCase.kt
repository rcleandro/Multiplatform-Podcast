package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FeedSource
import br.com.carvalho.podcast.domain.repository.PodcastRepository

class AddPodcastFromUrlUseCase(
    private val feedSource: FeedSource,
    private val podcastRepository: PodcastRepository
) {
    suspend operator fun invoke(input: String): Result<Podcast> {
        val url = validFeedUrl(input)
        return when {
            url == null -> Result.failure(AppError.InvalidUrl)
            podcastRepository.getPodcastById(url) != null -> Result.failure(AppError.AlreadyExists)
            else -> feedSource.fetch(url).mapCatching { feed ->
                podcastRepository.saveFeed(feed.podcast, feed.episodes)
                feed.podcast
            }
        }
    }
}
