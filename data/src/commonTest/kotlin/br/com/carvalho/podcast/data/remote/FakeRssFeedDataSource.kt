package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.data.remote.model.RssFeed

class FakeRssFeedDataSource : RssFeedDataSource {
    var feedResult: Result<RssFeed> = Result.failure(Exception("Not set"))
    var fetchFeedCalledWith: String? = null
    var validateResult: Result<Boolean> = Result.success(true)

    var delayMs: Long = 0

    override suspend fun fetchFeed(url: String): Result<RssFeed> {
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
        fetchFeedCalledWith = url
        return feedResult
    }

    override suspend fun validateFeedUrl(url: String): Result<Boolean> {
        return validateResult
    }
}
