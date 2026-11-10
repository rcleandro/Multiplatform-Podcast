package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppConfig
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
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.decode
import io.ktor.utils.io.charsets.forName
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.readByteArray

private const val TAG = "RssFeedDataSource"
private const val CHANNEL_TAG = "<channel"

// A feed request follows redirects itself to tell permanent ones (the feed moved) from temporary ones.
private const val MAX_REDIRECTS = 5
private val PERMANENT_REDIRECTS = setOf(HttpStatusCode.MovedPermanently, HttpStatusCode.PermanentRedirect)
private val TEMPORARY_REDIRECTS = setOf(HttpStatusCode.Found, HttpStatusCode.SeeOther, HttpStatusCode.TemporaryRedirect)

class RssFeedDataSourceImpl(
    client: HttpClient,
    private val dispatchers: CoroutineDispatchers,
    private val maxFeedBytes: Long = AppConfig.MAX_TEXT_BYTES,
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
            val next = URLBuilder(target).takeFrom(location).buildString()
            // From https to http the next request, and a private feed's token, would travel in clear text (22.8);
            // Ktor's own redirects refuse it, so the feed stops here as an error, as with too many redirects.
            if (target.startsWith("https://", ignoreCase = true) && !next.startsWith("https://", ignoreCase = true)) {
                return@repeat
            }
            target = next
            allPermanent = allPermanent && permanent
            permanentRedirect = target.takeIf { allPermanent }
            response = get(target, version)
        }
        if (response.status == HttpStatusCode.NotModified) return@catchingAppError null
        if (!response.status.isSuccess()) {
            AppLogger.e(TAG, "Server returned error: ${response.status} for URL: $target")
            throw AppError.Http(response.status.value)
        }
        val xmlContent = response.textWithin(maxFeedBytes)
        if (CHANNEL_TAG !in xmlContent) throw AppError.InvalidFeed

        val etag = response.headers[HttpHeaders.ETag]
        val lastModified = response.headers[HttpHeaders.LastModified]
        val received = if (etag == null && lastModified == null) null else FeedVersion(etag, lastModified)
        withContext(dispatchers.default) {
            RssXmlParser.parse(xmlContent).copy(version = received, permanentRedirect = permanentRedirect)
        }
    }

    private suspend fun get(url: String, version: FeedVersion?): HttpResponse = client.get(url) {
        version?.etag?.let { header(HttpHeaders.IfNoneMatch, it) }
        version?.lastModified?.let { header(HttpHeaders.IfModifiedSince, it) }
    }
}

/**
 * The body as text, at most [maxBytes] of it (22.3, T2 of ADR 0014): a feed, or the chapters and the transcript a feed
 * points to. The length the server declares can be missing or false, and a gzip body grows when unpacked, so the cap
 * counts what arrives, already decompressed. Read in the encoding the server names, else the one an XML document
 * declares in its first line (older feeds are ISO-8859-1 and say so only there), else UTF-8.
 */
internal suspend fun HttpResponse.textWithin(maxBytes: Long): String {
    val body = bodyAsChannel().readBuffer(maxBytes + 1)
    if (body.size > maxBytes) throw AppError.FeedTooLarge
    return (charset() ?: declaredCharset(body) ?: Charsets.UTF_8).newDecoder().decode(body)
}

private val XML_ENCODING = Regex("""<\?xml[^>]*\sencoding\s*=\s*["']([A-Za-z0-9._-]+)["']""")

/** The XML declaration is the document's first line; this much holds it with room to spare. */
private const val XML_DECLARATION_BYTES = 200L

/** The declaration is ASCII in every encoding feeds come in, so it reads before the encoding is known. */
private fun declaredCharset(body: Buffer): Charset? {
    val head = body.peek().readByteArray(minOf(body.size, XML_DECLARATION_BYTES).toInt()).decodeToString()
    val name = XML_ENCODING.find(head)?.groupValues?.get(1) ?: return null
    // A name this platform does not know is read as UTF-8, as before.
    return runCatching { Charsets.forName(name) }.getOrNull()
}
