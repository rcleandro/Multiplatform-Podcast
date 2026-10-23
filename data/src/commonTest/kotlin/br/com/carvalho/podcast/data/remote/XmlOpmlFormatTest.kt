package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.domain.model.Podcast
import kotlin.test.Test
import kotlin.test.assertEquals

class XmlOpmlFormatTest {
    @Test
    fun readsTheFeedsAtAnyDepthOnceEach() {
        val opml = """
            <?xml version="1.0" encoding="utf-8"?>
            <opml version="1.0">
              <head><title>Exported</title></head>
              <body>
                <outline text="feeds">
                  <outline type="rss" text="One" xmlUrl="https://one.example.com/feed?a=1&amp;b=2" />
                  <outline text="Folder"><outline type="rss" text="Two" xmlURL=" https://two.example.com/rss "/></outline>
                </outline>
                <outline type="rss" text="One again" xmlUrl="https://one.example.com/feed?a=1&amp;b=2"/>
                <outline type="link" text="Not a feed" url="https://example.com"/>
              </body>
            </opml>
        """.trimIndent()

        assertEquals(
            listOf("https://one.example.com/feed?a=1&b=2", "https://two.example.com/rss"),
            XmlOpmlFormat.read(opml),
        )
    }

    @Test
    fun whatItWritesItReadsBack() {
        val podcasts = listOf(
            podcast("Tom & Jerry's \"Show\"", "https://feeds.example.com/a?x=1&y=2", site = "https://example.com"),
            podcast("<Plain>", "https://feeds.example.com/b", site = null),
        )

        val opml = XmlOpmlFormat.write(podcasts)

        assertEquals(podcasts.map { it.feedUrl }, XmlOpmlFormat.read(opml))
    }

    @Test
    fun somethingThatIsNotOpmlListsNothing() {
        assertEquals(emptyList(), XmlOpmlFormat.read("not xml at all"))
    }

    private fun podcast(title: String, feed: String, site: String?) = Podcast(
        id = feed, title = title, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = feed, siteUrl = site, lastUpdated = 0, isSubscribed = true,
    )

    @Test
    fun anOpmlFileThatDeclaresEntitiesImportsNothing() {
        // The same billion laughs as a feed (22.3): before the fix, reading it filled the memory.
        val laughs = (1..LAUGH_LEVELS).joinToString("\n") { level ->
            val previous = if (level == 1) "lol" else "&lol${level - 1};".repeat(LAUGHS_PER_LEVEL)
            """<!ENTITY lol$level "$previous">"""
        }
        val opml = "<?xml version=\"1.0\"?>\n<!DOCTYPE opml [\n$laughs\n]>\n" +
            """<opml version="2.0"><body><outline xmlUrl="https://feed.example/&lol$LAUGH_LEVELS;"/></body></opml>"""

        assertEquals(emptyList(), XmlOpmlFormat.read(opml))
    }

    @Test
    fun onlyWebAddressesAreImported() {
        val opml = """
            <opml version="2.0"><body>
            <outline xmlUrl="file:///data/data/br.com.carvalho.podcast/databases/podcast.db"/>
            <outline xmlUrl="content://contacts/people"/>
            <outline xmlUrl=" https://feed.example/rss "/>
            </body></opml>
        """.trimIndent()

        assertEquals(listOf("https://feed.example/rss"), XmlOpmlFormat.read(opml))
    }

    private companion object {
        const val LAUGH_LEVELS = 9
        const val LAUGHS_PER_LEVEL = 10
    }
}
