package br.com.carvalho.podcast.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals

class PodcastLinkTest {
    @Test
    fun aFeedAddressIsItself() {
        assertEquals(PodcastLink.Feed("https://hipsters.tech/feed/podcast/"), podcastLink(" https://hipsters.tech/feed/podcast/ "))
        assertEquals(PodcastLink.Feed("http://example.com/rss"), podcastLink("http://example.com/rss"))
    }

    @Test
    fun aPodcastAppSchemeBecomesHttps() {
        assertEquals(PodcastLink.Feed("https://example.com/rss"), podcastLink("pcast://example.com/rss"))
        assertEquals(PodcastLink.Feed("https://example.com/rss"), podcastLink("podcast://example.com/rss"))
        assertEquals(PodcastLink.Feed("https://example.com/rss"), podcastLink("itpc://example.com/rss"))
        assertEquals(PodcastLink.Feed("https://example.com/rss"), podcastLink("FEED://example.com/rss"))
    }

    @Test
    fun aSchemeBeforeAFullAddressKeepsIt() {
        assertEquals(PodcastLink.Feed("http://example.com/rss"), podcastLink("feed:http://example.com/rss"))
        assertEquals(PodcastLink.Feed("https://example.com/rss"), podcastLink("podcast://https://example.com/rss"))
        // A host that only starts like a scheme is still a host.
        assertEquals(PodcastLink.Feed("https://httpcast.example.com/rss"), podcastLink("pcast://httpcast.example.com/rss"))
    }

    @Test
    fun anApplePodcastsShowGivesItsId() {
        assertEquals(
            PodcastLink.Apple(1_200_361_736),
            podcastLink("https://podcasts.apple.com/br/podcast/hipsters-ponto-tech/id1200361736?i=1000650000000"),
        )
        assertEquals(PodcastLink.Apple(42), podcastLink("podcasts://podcasts.apple.com/us/podcast/id42"))
        assertEquals(PodcastLink.Apple(42), podcastLink("https://itunes.apple.com/podcast/id42"))
    }

    @Test
    fun anythingElseIsNotAPodcastLink() {
        assertEquals(null, podcastLink("https://podcasts.apple.com/br/browse"))
        assertEquals(null, podcastLink("mailto:someone@example.com"))
        assertEquals(null, podcastLink("pcast://not a host"))
        assertEquals(null, podcastLink(""))
    }
}
