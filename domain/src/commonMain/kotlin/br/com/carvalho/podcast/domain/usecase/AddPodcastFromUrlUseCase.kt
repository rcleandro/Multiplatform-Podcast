package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FeedSource
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.first

class AddPodcastFromUrlUseCase(
    private val feedSource: FeedSource,
    private val podcastRepository: PodcastRepository
) {
    suspend operator fun invoke(input: String): Result<Podcast> {
        val url = validFeedUrl(input)
        // The same feed may be saved under another spelling (http, www., a trailing slash): compare keys.
        val saved = podcastRepository.getPodcasts().first().map { feedUrlKey(it.feedUrl) }.toSet()
        return when {
            url == null -> Result.failure(AppError.InvalidUrl)
            feedUrlKey(url) in saved -> Result.failure(AppError.AlreadyExists)
            else -> feedSource.fetch(url).mapCatching { feed ->
                // An old or mirror address of a saved feed: the feed itself says where it lives.
                if (feed.declaredUrls.any { feedUrlKey(it) in saved }) throw AppError.AlreadyExists
                val podcast = feed.podcast.copy(feedUrl = movedFeedUrl(url, feed.movedTo) ?: url)
                podcastRepository.saveFeed(podcast, feed.episodes)
                podcast
            }
        }
    }
}
