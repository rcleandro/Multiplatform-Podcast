package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** A feed is untrusted input (22.3, T2 to T4 of ADR 0014): it must not exhaust memory, read local files or nest forever. */
class HostileFeedTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(main = testDispatcher, io = testDispatcher, default = testDispatcher)

    @Test
    fun `a feed bigger than the limit stops before it is read whole`() = runTest(testDispatcher) {
        val huge = feed(item(audio = "https://cdn.example.com/a.mp3")) + " ".repeat(LIMIT)
        val source = RssFeedDataSourceImpl(HttpClient(MockEngine { respond(huge) }), dispatchers, maxFeedBytes = LIMIT.toLong())

        assertEquals(AppError.FeedTooLarge, source.fetchFeed(FEED_URL).exceptionOrNull())
    }

    @Test
    fun `a feed within the limit keeps the charset its server declares`() = runTest(testDispatcher) {
        val latin1 = feed(item(audio = "https://cdn.example.com/a.mp3", title = "Ação"))
            .map { it.code.toByte() }.toByteArray()
        val engine = MockEngine {
            respond(latin1, headers = headersOf(HttpHeaders.ContentType, "application/rss+xml; charset=ISO-8859-1"))
        }

        val parsed = RssFeedDataSourceImpl(HttpClient(engine), dispatchers).fetchFeed(FEED_URL).getOrThrow()

        assertEquals("Ação", parsed?.episodes?.single()?.title)
    }

    @Test
    fun `only web addresses survive from the feed`() {
        val parsed = RssXmlParser.parse(
            feed(
                item(audio = "file:///data/data/br.com.carvalho.podcast/databases/podcast.db"),
                item(audio = "content://contacts/people", title = "Contacts"),
                item(
                    audio = "HTTPS://cdn.example.com/ok.mp3",
                    title = "Fine",
                    extra = """
                        <link>javascript:alert(1)</link>
                        <itunes:image href="file:///etc/passwd"/>
                        <podcast:chapters url="android.resource://br.com.carvalho.podcast/raw/x" type="application/json+chapters"/>
                        <podcast:transcript url="file:///private/var/mobile/secret.vtt" type="text/vtt"/>
                    """,
                ),
                channelExtra = """
                    <link>intent://evil#Intent;end</link>
                    <itunes:image href="content://media/external/images/1"/>
                    <itunes:new-feed-url>file:///sdcard/feed.xml</itunes:new-feed-url>
                """,
            )
        )

        val episode = parsed.episodes.single()
        assertEquals("Fine", episode.title)
        assertEquals("HTTPS://cdn.example.com/ok.mp3", episode.enclosureUrl)
        assertNull(episode.link)
        assertNull(episode.imageUrl)
        assertNull(episode.chaptersUrl)
        assertNull(episode.transcript)
        assertNull(parsed.link)
        assertNull(parsed.imageUrl)
        assertNull(parsed.newFeedUrl)
    }

    @Test
    fun `a feed that declares entities is refused before they expand`() {
        val laughs = (1..LAUGH_LEVELS).joinToString("\n") { level ->
            val previous = if (level == 1) "lol" else "&lol${level - 1};".repeat(LAUGHS_PER_LEVEL)
            """<!ENTITY lol$level "$previous">"""
        }
        val xml = "<?xml version=\"1.0\"?>\n<!DOCTYPE rss [\n$laughs\n]>\n" +
            feed(item(audio = "https://cdn.example.com/a.mp3", title = "&lol$LAUGH_LEVELS;"))

        // Before the fix, the reader expanded them until the memory ran out.
        val error = runCatching { RssXmlParser.parse(xml) }.exceptionOrNull()

        assertEquals(AppError.InvalidFeed, error)
    }

    @Test
    fun `an external entity is never fetched`() {
        val xml = "<?xml version=\"1.0\"?>\n<!DOCTYPE rss [\n<!ENTITY xxe SYSTEM \"file:///etc/passwd\">\n]>\n" +
            feed(item(audio = "https://cdn.example.com/a.mp3", title = "&xxe;"))

        // The reader refuses such a declaration outright, which turns the feed into an error; either way, no file.
        val title = runCatching { RssXmlParser.parse(xml).episodes.single().title }.getOrDefault("")

        assertFalse("root:" in title)
    }

    @Test
    fun `a feed nested deeper than any real one is refused`() {
        val deep = "<a>".repeat(DEEP) + "</a>".repeat(DEEP)

        val error = runCatching { RssXmlParser.parse(feed(channelExtra = deep)) }.exceptionOrNull()

        assertEquals(AppError.InvalidFeed, error)
    }

    private fun feed(vararg items: String, channelExtra: String = "") = """
        <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:podcast="https://podcastindex.org/namespace/1.0">
        <channel><title>Feed</title>$channelExtra${items.joinToString("")}</channel></rss>
    """.trimIndent()

    private fun item(audio: String, title: String = "Episode", extra: String = "") =
        """<item><title>$title</title><enclosure url="$audio" type="audio/mpeg"/>$extra</item>"""

    private companion object {
        const val FEED_URL = "https://feed.example/rss"
        const val LIMIT = 4_096
        const val LAUGH_LEVELS = 9
        const val LAUGHS_PER_LEVEL = 10
        const val DEEP = 10_000
    }
}
