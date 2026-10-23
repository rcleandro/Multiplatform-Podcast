package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.domain.model.FeedVersion
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.charset
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.decode
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.withContext

private const val TAG = "RssFeedDataSource"
private const val CHANNEL_TAG = "<channel"

// ponytail: one fixed cap, decompressed; the biggest real feeds are a few MB, and the watch refreshes feeds too.
internal const val MAX_FEED_BYTES = 30L * 1024 * 1024

// A feed request follows redirects itself to tell permanent ones (the feed moved) from temporary ones.
private const val MAX_REDIRECTS = 5
private val PERMANENT_REDIRECTS = setOf(HttpStatusCode.MovedPermanently, HttpStatusCode.PermanentRedirect)
private val TEMPORARY_REDIRECTS = setOf(HttpStatusCode.Found, HttpStatusCode.SeeOther, HttpStatusCode.TemporaryRedirect)

class RssFeedDataSourceImpl(
    client: HttpClient,
    private val dispatchers: CoroutineDispatchers,
    private val maxFeedBytes: Long = MAX_FEED_BYTES,
) : RssFeedDataSource {
    // Same engine and plugins; only feeds stop following redirects automatically (downloads still do).
    private val client = client.config { followRedirects = false }

    override suspend fun fetchFeed(url: String, version: FeedVersion?): Result<RssFeed?> = catchingAppError {
        AppLogger.d(TAG, "Fetching feed from URL: $url")
        var target = url
        var permanentRedirect: String? = null
        var allPermanent = true
        var response = get(target, version)
        repeat(MAX_REDIRECTS) {
            val location = response.headers[HttpHeaders.Location]
            val permanent = response.status in PERMANENT_REDIRECTS
            if (location == null || !permanent && response.status !in TEMPORARY_REDIRECTS) return@repeat
            target = URLBuilder(target).takeFrom(location).buildString()
            allPermanent = allPermanent && permanent
            permanentRedirect = target.takeIf { allPermanent }
            response = get(target, version)
        }
        if (response.status == HttpStatusCode.NotModified) return@catchingAppError null
        if (!response.status.isSuccess()) {
            AppLogger.e(TAG, "Server returned error: ${response.status} for URL: $target")
            throw AppError.Http(response.status.value)
        }
        val xmlContent = response.feedText()
        if (CHANNEL_TAG !in xmlContent) throw AppError.InvalidFeed

        val etag = response.headers[HttpHeaders.ETag]
        val lastModified = response.headers[HttpHeaders.LastModified]
        val received = if (etag == null && lastModified == null) null else FeedVersion(etag, lastModified)
        withContext(dispatchers.default) {
            RssXmlParser.parse(xmlContent).copy(version = received, permanentRedirect = permanentRedirect)
        }
    }

    /**
     * The body as text, at most [maxFeedBytes] of it (22.3, T2 of ADR 0014). The length the server declares can be
     * missing or false, and a gzip body grows when unpacked, so the cap counts what arrives, already decompressed.
     */
    private suspend fun HttpResponse.feedText(): String {
        val body = bodyAsChannel().readBuffer(maxFeedBytes + 1)
        if (body.size > maxFeedBytes) throw AppError.FeedTooLarge
        return (charset() ?: Charsets.UTF_8).newDecoder().decode(body)
    }

    private suspend fun get(url: String, version: FeedVersion?): HttpResponse = client.get(url) {
        version?.etag?.let { header(HttpHeaders.IfNoneMatch, it) }
        version?.lastModified?.let { header(HttpHeaders.IfModifiedSince, it) }
    }
}
