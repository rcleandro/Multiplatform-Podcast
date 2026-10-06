package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.domain.model.FeedVersion

class FakeRssFeedDataSource : RssFeedDataSource {
    var feedResult: Result<RssFeed?> = Result.failure(Exception("Not set"))
    var fetchFeedCalledWith: String? = null

    var delayMs: Long = 0

    override suspend fun fetchFeed(url: String, version: FeedVersion?): Result<RssFeed?> {
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
        fetchFeedCalledWith = url
        return feedResult
    }
}
