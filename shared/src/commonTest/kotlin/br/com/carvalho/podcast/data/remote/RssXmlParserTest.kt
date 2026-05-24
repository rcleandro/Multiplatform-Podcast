package br.com.carvalho.podcast.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

class RssXmlParserTest {

    @Test
    fun `parses simple rss feed correctly`() {
        val xml = """
            <rss>
                <channel>
                    <title>Test Podcast</title>
                    <description>Description</description>
                    <itunes:author>Author</itunes:author>
                    <item>
                        <title>Episode 1</title>
                        <guid>ep1</guid>
                        <enclosure url="https://example.com/audio.mp3" type="audio/mpeg" />
                        <pubDate>Fri, 15 May 2026 10:00:00 GMT</pubDate>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val feed = RssXmlParser.parse(xml)

        assertEquals("Test Podcast", feed.title)
        assertEquals(1, feed.episodes.size)
        assertEquals("Episode 1", feed.episodes[0].title)
        assertEquals("ep1", feed.episodes[0].guid)
        assertEquals("https://example.com/audio.mp3", feed.episodes[0].enclosureUrl)
    }

    @Test
    fun `parses guid with attributes correctly`() {
        val xml = """
            <rss>
                <channel>
                    <item>
                        <title>Episode 1</title>
                        <guid isPermaLink="false">ep1-guid</guid>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val feed = RssXmlParser.parse(xml)
        assertEquals("ep1-guid", feed.episodes[0].guid)
    }
}
