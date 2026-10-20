package br.com.carvalho.podcast.presentation.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportMailTest {
    @Test
    fun onlyTheFeedsHostGoesOutNeverItsToken() {
        assertEquals("feeds.example.com", feedHost("https://user:secret@feeds.example.com/private/abc123?token=xyz"))
        assertEquals("example.com", feedHost("https://example.com?key=1"))
    }

    @Test
    fun theSubjectAndBodyAreEncodedForTheEmailApp() {
        val mailto = reportMailto("Denúncia: A&B", "Motivo: x\nPodcast: y")

        assertTrue(mailto.startsWith("mailto:"))
        assertTrue("subject=Den%C3%BAncia%3A%20A%26B" in mailto)
        assertTrue("body=Motivo%3A%20x%0APodcast%3A%20y" in mailto)
        assertFalse(" " in mailto)
    }
}
