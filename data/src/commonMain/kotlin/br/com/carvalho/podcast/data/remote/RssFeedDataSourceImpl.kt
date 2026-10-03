package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.withContext

private const val TAG = "RssFeedDataSource"

class RssFeedDataSourceImpl(
    private val client: HttpClient,
    private val dispatchers: CoroutineDispatchers
) : RssFeedDataSource {

    override suspend fun fetchFeed(url: String): Result<RssFeed> = runCatching {
        AppLogger.d(TAG, "Fetching feed from URL: $url")
        val response = client.get(url)
        if (!response.status.isSuccess()) {
            AppLogger.e(TAG, "Server returned error: ${response.status} for URL: $url")
            throw Exception("Falha ao carregar o feed: ${response.status}")
        }
        val xmlContent = response.bodyAsText()
        
        withContext(dispatchers.default) {
            RssXmlParser.parse(xmlContent)
        }
    }

    override suspend fun validateFeedUrl(url: String): Result<Boolean> = runCatching {
        AppLogger.d(TAG, "Validating feed URL: $url")
        val response = client.head(url)
        val contentType = response.contentType()?.toString() ?: ""
        val isValid = contentType.contains("xml") || contentType.contains("rss")
        AppLogger.d(TAG, "Validation result for $url: $isValid (ContentType: $contentType)")
        isValid
    }
}
