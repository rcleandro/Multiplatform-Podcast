package br.com.carvalho.podcast.domain.repository

class FakeFeedSource : FeedSource {
    var result: Result<FetchedFeed> = Result.failure(IllegalStateException("No feed configured"))
    var fetchCalledWith: String? = null

    override suspend fun fetch(feedUrl: String): Result<FetchedFeed> {
        fetchCalledWith = feedUrl
        return result
    }
}
