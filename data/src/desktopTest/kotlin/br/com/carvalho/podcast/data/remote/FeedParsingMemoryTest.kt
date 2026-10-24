package br.com.carvalho.podcast.data.remote

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A feed as large as the biggest one in the user's library (4,561 episodes, about 13 MB) is read in a heap that holds
 * its text and the episodes, but not a tree of the whole document as well (23.6). The parse runs in a child JVM, since
 * only there the heap can be capped; a reader that keeps every item until the end runs out of memory.
 */
class FeedParsingMemoryTest {
    @Test
    fun theBiggestFeedIsReadWithoutHoldingTheWholeDocument() {
        val java = File(System.getProperty("java.home"), "bin/java").path
        val classpath = System.getProperty("java.class.path")
        val process = ProcessBuilder(java, "-Xmx$HEAP_MB" + "m", "-cp", classpath, BigFeedParse::class.java.name)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.waitFor(), output)
    }

    private companion object {
        const val HEAP_MB = 46
    }
}

/** The child JVM's entry point: builds the feed, parses it and exits non-zero if memory ran out. */
object BigFeedParse {
    private const val EPISODES = 4561
    private const val NOTE_REPEATS = 20
    private const val XML_CHARS = 14_000_000

    @JvmStatic
    fun main(args: Array<String>) {
        val notes = "Show notes with <b>html</b> and links https://example.com/a/b/c. ".repeat(NOTE_REPEATS)
        // Sized once, so growing the builder does not take memory the parse is measured against.
        val xml = buildString(XML_CHARS) {
            append("<rss xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\" ")
            append("xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"><channel><title>Big</title>")
            repeat(EPISODES) { i ->
                append("<item><title>Episode $i</title><guid>ep-$i</guid><description><![CDATA[$notes]]></description>")
                append("<content:encoded><![CDATA[$notes]]></content:encoded>")
                append("<enclosure url=\"https://cdn.example.com/$i.mp3\" length=\"1000\" type=\"audio/mpeg\"/>")
                append("<pubDate>Fri, 15 May 2026 10:00:00 GMT</pubDate><itunes:duration>01:02:03</itunes:duration></item>")
            }
            append("<itunes:image href=\"https://cdn.example.com/cover.jpg\"/></channel></rss>")
        }
        val feed = RssXmlParser.parse(xml)
        check(feed.episodes.size == EPISODES)
        check(feed.episodes.all { it.imageUrl == "https://cdn.example.com/cover.jpg" })
    }
}
