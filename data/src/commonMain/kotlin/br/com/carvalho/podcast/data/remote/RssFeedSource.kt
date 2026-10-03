package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.data.mapper.toEpisode
import br.com.carvalho.podcast.data.mapper.toPodcast
import br.com.carvalho.podcast.domain.repository.FeedSource
import br.com.carvalho.podcast.domain.repository.FetchedFeed

/** [FeedSource] backed by an RSS feed. */
class RssFeedSource(private val dataSource: RssFeedDataSource) : FeedSource {
    override suspend fun fetch(feedUrl: String): Result<FetchedFeed> =
        dataSource.fetchFeed(feedUrl).map { feed ->
            val podcast = feed.toPodcast(feedUrl = feedUrl)
            FetchedFeed(
                podcast = podcast,
                episodes = feed.episodes.map { it.toEpisode(podcastId = podcast.id, podcastTitle = podcast.title) },
            )
        }
}
