package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.domain.model.FeedVersion

interface RssFeedDataSource {
    /** `null` when the server says the feed has not changed since [version]. */
    suspend fun fetchFeed(url: String, version: FeedVersion? = null): Result<RssFeed?>
}
