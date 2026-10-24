package br.com.carvalho.podcast.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedUrlTest {
    @Test
    fun `accepts feed addresses and adds https when the scheme is missing`() {
        mapOf(
            "https://www.hipsters.tech/feed/podcast/" to "https://www.hipsters.tech/feed/podcast/",
            "  http://feeds.example.com/rss?x=1&y=2  " to "http://feeds.example.com/rss?x=1&y=2",
            "HTTPS://Feeds.Example.com/rss" to "HTTPS://Feeds.Example.com/rss",
            "anchor.fm/s/4f366e84/podcast/rss" to "https://anchor.fm/s/4f366e84/podcast/rss",
            "https://user:pass@feeds.example.com:8443/rss" to "https://user:pass@feeds.example.com:8443/rss",
            "https://feeds.example.com/rss%20show" to "https://feeds.example.com/rss%20show",
        ).forEach { (input, expected) -> assertEquals(expected, validFeedUrl(input), input) }
    }

    @Test
    fun `rejects what cannot be a feed address`() {
        listOf(
            "",
            "   ",
            "nerdcast",
            "ftp://feeds.example.com/rss",
            "https://",
            "https:///rss",
            "https://feeds example.com/rss",
            "https://feeds.example.com/my show",
            "https://jovemnerd.com.br/feed-nerdcast|",
            "https://feeds.example.com:port/rss",
            "https://-.example..com/rss",
        ).forEach { assertNull(validFeedUrl(it), it) }
    }

    @Test
    fun `addresses of the same feed share a key`() {
        val key = feedUrlKey("https://www.hipsters.tech/feed/podcast/")
        listOf(
            "http://hipsters.tech/feed/podcast",
            "https://WWW.Hipsters.Tech/feed/podcast/",
            "https://hipsters.tech/feed/podcast/#latest",
            "HTTPS://hipsters.tech/feed/podcast//",
        ).forEach { assertEquals(key, feedUrlKey(it), it) }
    }

    @Test
    fun `different feeds keep different keys`() {
        listOf(
            "https://feeds.example.com/rss" to "https://feeds.example.com/RSS",
            "https://feeds.example.com/rss?show=1" to "https://feeds.example.com/rss?show=2",
            "https://feeds.example.com/rss" to "https://other.example.com/rss",
            "https://feeds.example.com/a/rss" to "https://feeds.example.com/b/rss",
        ).forEach { (a, b) -> assertNotEquals(feedUrlKey(a), feedUrlKey(b), "$a vs $b") }
    }

    @Test
    fun `a moved feed takes the declared address only when it is valid and really new`() {
        assertEquals("https://new.example.com/rss", movedFeedUrl("https://old.example.com/rss", "https://new.example.com/rss"))
        assertNull(movedFeedUrl("https://old.example.com/rss", null))
        assertNull(movedFeedUrl("https://old.example.com/rss", "not a url"))
        assertNull(movedFeedUrl("https://www.old.example.com/rss/", "http://old.example.com/rss"))
    }

    @Test
    fun aPrivateFeedsSecretIsSpottedWhereverItSits() {
        // Token in the user info, the query or a long random part of the path (22.6, ADR 0014).
        listOf(
            "https://member:pass@feeds.example.com/rss",
            "https://www.patreon.com/rss/creator?auth=Xy12abCD34",
            "https://feeds.supercast.com/episodes/2c9f8b7a6d5e4f3a2b1c0d9e8f7a6b5c.rss",
            "https://example.memberful.com/rss/feed/1234abcd5678efgh9012",
            "https://cdn.example.com/private/e1.mp3?key=abc",
        ).forEach { assertTrue(mayCarrySecret(it), it) }

        listOf(
            "https://feeds.npr.org/510289/podcast.xml",
            "https://feeds.megaphone.fm/GLT1412515089",
            "https://feeds.simplecast.com/54nAGcIl",
            "https://anchor.fm/s/12ab34cd/podcast/rss",
            "https://hipsters.tech/feed",
            "https://traffic.libsyn.com/secure/show/episode-128-a-very-long-but-readable-title.mp3",
        ).forEach { assertFalse(mayCarrySecret(it), it) }
    }
}
