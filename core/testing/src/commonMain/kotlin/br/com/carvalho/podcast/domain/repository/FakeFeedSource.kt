package br.com.carvalho.podcast.domain.repository

import kotlinx.coroutines.delay

class FakeFeedSource : FeedSource {
    var result: Result<FetchedFeed> = Result.failure(IllegalStateException("No feed configured"))
    var fetchCalledWith: String? = null
    var delayMs = 0L

    /** Overrides [result] for one feed URL. */
    val resultsByUrl = mutableMapOf<String, Result<FetchedFeed>>()

    override suspend fun fetch(feedUrl: String): Result<FetchedFeed> {
        fetchCalledWith = feedUrl
        delay(delayMs)
        return resultsByUrl[feedUrl] ?: result
    }
}
