package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.data.mapper.toEpisode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

/**
 * A feed with 1,000 episodes (long-running shows have that many) is read and mapped within a budget (17.6). The
 * budget leaves room for the slowest target and a busy CI runner; a regression that makes parsing quadratic or
 * re-reads the document per item still blows past it.
 */
class LargeFeedParsingTest {
    @Test
    fun aThousandEpisodeFeedIsReadWithinTheBudget() {
        val xml = largeFeed(EPISODES)
        RssXmlParser.parse(xml) // warm up: the first run also loads and compiles the reader

        var parsed = 0
        val took = measureTime {
            val feed = RssXmlParser.parse(xml)
            parsed = feed.episodes.map { it.toEpisode(podcastId = "big") }.size
        }

        println("1000-episode feed: ${took.inWholeMilliseconds} ms")
        assertEquals(EPISODES, parsed)
        assertTrue(took < BUDGET, "Took $took, budget $BUDGET")
    }

    private fun largeFeed(count: Int) = buildString {
        append("<rss xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\"><channel><title>Big</title>")
        repeat(count) { i ->
            append("<item><title>Episode $i &amp; friends</title><guid>ep-$i</guid>")
            append("<description><![CDATA[<p>Show notes for episode $i, with <a href=\"https://example.com\">links</a>.</p>]]>")
            append("</description><enclosure url=\"https://cdn.example.com/$i.mp3\" length=\"1000\" type=\"audio/mpeg\"/>")
            append("<pubDate>Fri, 15 May 2026 10:00:00 GMT</pubDate><itunes:duration>01:02:03</itunes:duration></item>")
        }
        append("</channel></rss>")
    }

    private companion object {
        const val EPISODES = 1000
        val BUDGET = 2000.milliseconds
    }
}
