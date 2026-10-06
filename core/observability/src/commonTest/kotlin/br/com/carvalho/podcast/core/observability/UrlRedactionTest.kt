package br.com.carvalho.podcast.core.observability

import kotlin.test.Test
import kotlin.test.assertEquals

class UrlRedactionTest {
    @Test
    fun keepsOnlySchemeAndHost() {
        assertEquals("GET https://cdn.example.com/… 200", redactUrls("GET https://cdn.example.com/ep.mp3?k=1 200"))
        assertEquals("see http://example.com", redactUrls("see http://example.com"))
        assertEquals("no url here", redactUrls("no url here"))
    }

    @Test
    fun hostDropsCredentialsPortAndCase() {
        assertEquals("feeds.example.com", urlHost("https://u:p@Feeds.Example.com:8443/rss?token=1"))
        assertEquals("", urlHost("not a url"))
    }
}
