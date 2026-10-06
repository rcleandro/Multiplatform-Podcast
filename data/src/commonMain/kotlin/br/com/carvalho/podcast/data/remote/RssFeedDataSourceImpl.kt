package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext

private const val TAG = "RssFeedDataSource"
private const val CHANNEL_TAG = "<channel"

class RssFeedDataSourceImpl(
    private val client: HttpClient,
    private val dispatchers: CoroutineDispatchers
) : RssFeedDataSource {

    override suspend fun fetchFeed(url: String): Result<RssFeed> = catchingAppError {
        AppLogger.d(TAG, "Fetching feed from URL: $url")
        val response = client.get(url)
        if (!response.status.isSuccess()) {
            AppLogger.e(TAG, "Server returned error: ${response.status} for URL: $url")
            throw AppError.Http(response.status.value)
        }
        val xmlContent = response.bodyAsText()
        if (CHANNEL_TAG !in xmlContent) throw AppError.InvalidFeed

        withContext(dispatchers.default) {
            RssXmlParser.parse(xmlContent)
        }
    }

}
