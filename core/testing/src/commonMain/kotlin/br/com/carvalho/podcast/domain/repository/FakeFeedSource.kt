package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.FeedVersion
import kotlinx.coroutines.delay

class FakeFeedSource : FeedSource {
    var result: Result<FetchedFeed> = Result.failure(IllegalStateException("No feed configured"))
    var fetchCalledWith: String? = null
    var delayMs = 0L

    /** Overrides [result] for one feed URL. */
    val resultsByUrl = mutableMapOf<String, Result<FetchedFeed>>()

    /** When true, [fetchIfChanged] answers "not modified". */
    var notModified = false
    var versionAskedFor: FeedVersion? = null
    var maxConcurrentFetches = 0
        private set
    private var concurrentFetches = 0

    override suspend fun fetch(feedUrl: String): Result<FetchedFeed> {
        fetchCalledWith = feedUrl
        concurrentFetches++
        maxConcurrentFetches = maxOf(maxConcurrentFetches, concurrentFetches)
        delay(delayMs)
        concurrentFetches--
        return resultsByUrl[feedUrl] ?: result
    }

    override suspend fun fetchIfChanged(feedUrl: String, version: FeedVersion?): Result<FetchedFeed?> {
        versionAskedFor = version
        val fetched = fetch(feedUrl)
        return if (notModified) Result.success(null) else fetched
    }
}
