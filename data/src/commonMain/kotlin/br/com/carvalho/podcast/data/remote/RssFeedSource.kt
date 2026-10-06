package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.data.mapper.toEpisode
import br.com.carvalho.podcast.data.mapper.toPodcast
import br.com.carvalho.podcast.domain.model.FeedVersion
import br.com.carvalho.podcast.domain.repository.FeedSource
import br.com.carvalho.podcast.domain.repository.FetchedFeed

/** [FeedSource] backed by an RSS feed. */
class RssFeedSource(private val dataSource: RssFeedDataSource) : FeedSource {
    // Without a version the request is not conditional, so the server cannot answer "not modified".
    override suspend fun fetch(feedUrl: String): Result<FetchedFeed> =
        fetchIfChanged(feedUrl, version = null).map { it ?: throw AppError.InvalidFeed }

    override suspend fun fetchIfChanged(feedUrl: String, version: FeedVersion?): Result<FetchedFeed?> =
        dataSource.fetchFeed(feedUrl, version).map { feed ->
            feed ?: return@map null
            val podcast = feed.toPodcast(feedUrl = feedUrl)
            FetchedFeed(
                podcast = podcast,
                episodes = feed.episodes.map { it.toEpisode(podcastId = podcast.id, podcastTitle = podcast.title) },
                declaredUrls = listOfNotNull(feed.selfUrl, feed.newFeedUrl),
            )
        }
}
