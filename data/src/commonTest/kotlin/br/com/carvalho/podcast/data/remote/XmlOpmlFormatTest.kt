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
}
