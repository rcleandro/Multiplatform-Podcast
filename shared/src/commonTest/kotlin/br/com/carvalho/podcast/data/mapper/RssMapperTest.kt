package br.com.carvalho.podcast.data.mapper

import br.com.carvalho.podcast.data.remote.model.RssEpisode
import br.com.carvalho.podcast.data.remote.model.RssFeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RssMapperTest {

    @Test
    fun `RssFeed toPodcast maps correctly`() {
        val rssFeed = RssFeed(
            title = "Podcast Title",
            description = "Description",
            imageUrl = "image_url",
            author = "Author Name",
            language = "en",
            categories = listOf("Technology"),
            link = "site_link",
            ttl = null,
            episodes = emptyList()
        )

        val podcast = rssFeed.toPodcast("feed_url")

        assertEquals("feed_url", podcast.id)
        assertEquals("Podcast Title", podcast.title)
        assertEquals("image_url", podcast.imageUrl)
        assertEquals("site_link", podcast.siteUrl)
        assertTrue(podcast.lastUpdated > 0)
    }

    @Test
    fun `RssEpisode toEpisode maps correctly with duration and pubDate`() {
        val rssEpisode = RssEpisode(
            guid = "guid_123",
            title = "Episode Title",
            description = "Episode Description",
            enclosureUrl = "audio_url",
            enclosureType = "audio/mpeg",
            duration = "01:30:00",
            publishDate = "Fri, 15 May 2026 10:00:00 GMT",
            imageUrl = "ep_image",
            explicit = false,
            season = null,
            episode = null
        )

        val episode = rssEpisode.toEpisode("podcast_id", "Podcast Title")

        assertEquals("guid_123", episode.id)
        assertEquals("podcast_id", episode.podcastId)
        assertEquals("Podcast Title", episode.podcastTitle)
        assertEquals(5400L, episode.duration) // 1h 30m = 5400s
        // 2026-05-15 10:00:00 UTC = 1778839200000 ms
        assertEquals(1778839200000L, episode.publishDate)
    }

    @Test
    fun `parseDuration handles different formats`() {
        val rssEp1 = createRssEpisode(duration = "60")
        assertEquals(60L, rssEp1.toEpisode("p").duration)

        val rssEp2 = createRssEpisode(duration = "05:00")
        assertEquals(300L, rssEp2.toEpisode("p").duration)

        val rssEp3 = createRssEpisode(duration = "01:00:00")
        assertEquals(3600L, rssEp3.toEpisode("p").duration)
        
        val rssEp4 = createRssEpisode(duration = null)
        assertEquals(0L, rssEp4.toEpisode("p").duration)
    }

    @Test
    fun `parsePubDate handles various RFC 822 formats`() {
        val ep1 = createRssEpisode(pubDate = "Fri, 15 May 2026 10:00:00 GMT")
        assertEquals(1778839200000L, ep1.toEpisode("p").publishDate)

        val ep2 = createRssEpisode(pubDate = "15 May 2026 10:00:00 GMT")
        assertEquals(1778839200000L, ep2.toEpisode("p").publishDate)

        val ep3 = createRssEpisode(pubDate = "Fri, 5 May 2026 10:00:00 GMT")
        assertEquals(1777975200000L, ep3.toEpisode("p").publishDate)
    }

    private fun createRssEpisode(duration: String? = null, pubDate: String = "") = RssEpisode(
        guid = "id",
        title = "Title",
        description = null,
        enclosureUrl = "",
        enclosureType = null,
        duration = duration,
        publishDate = pubDate,
        imageUrl = null,
        explicit = false,
        season = null,
        episode = null
    )
}
