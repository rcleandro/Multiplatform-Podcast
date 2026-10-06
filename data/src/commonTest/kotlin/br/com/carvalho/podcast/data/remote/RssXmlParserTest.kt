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

    @Test
    fun `missing titles and author fall back to feed data instead of fixed text`() {
        val xml = """
            <rss>
                <channel>
                    <item>
                        <description>Interview about Kotlin</description>
                        <enclosure url="https://example.com/a.mp3" />
                    </item>
                    <item>
                        <enclosure url="https://example.com/files/ep-42.mp3?token=x" />
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val feed = RssXmlParser.parse(xml)

        assertEquals("", feed.title)
        assertEquals(null, feed.author)
        assertEquals("Interview about Kotlin", feed.episodes[0].title)
        assertEquals("ep-42.mp3", feed.episodes[1].title)
    }
}

class RssXmlParserFieldsTest {
    private val xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:atom="http://www.w3.org/2005/Atom"
             xmlns:content="http://purl.org/rss/1.0/modules/content/">
          <channel>
            <title>Show</title>
            <atom:link href="https://feeds.example.com/rss" rel="self"/>
            <link>https://example.com</link>
            <language>pt-BR</language>
            <ttl>60</ttl>
            <itunes:category text="Technology"><itunes:category text="Tech News"/></itunes:category>
            <itunes:category text="Technology"/>
            <item>
              <title>One</title>
              <link>https://example.com/one</link>
              <content:encoded><![CDATA[<p>Rich</p>]]></content:encoded>
              <enclosure url="https://cdn.example.com/1.m4a" type="audio/x-m4a" length="10"/>
              <itunes:explicit>Yes</itunes:explicit>
              <itunes:season>2</itunes:season>
              <itunes:episode>7</itunes:episode>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    @Test
    fun `reads the channel fields from the channel only`() {
        val feed = RssXmlParser.parse(xml)

        assertEquals("https://example.com", feed.link)
        assertEquals("pt-BR", feed.language)
        assertEquals(60, feed.ttl)
        assertEquals(listOf("Technology", "Tech News"), feed.categories)
    }

    @Test
    fun `reads the item fields the old parser hard-coded`() {
        val episode = RssXmlParser.parse(xml).episodes.single()

        assertEquals("<p>Rich</p>", episode.description)
        assertEquals("audio/x-m4a", episode.enclosureType)
        assertEquals(true, episode.explicit)
        assertEquals(2, episode.season)
        assertEquals(7, episode.episode)
    }

    @Test
    fun `HTML entities that XML does not declare stay as written`() {
        val feed = RssXmlParser.parse("<rss><channel><title>A&nbsp;B &amp; C</title></channel></rss>")

        assertEquals("A&nbsp;B & C", feed.title)
    }
}
