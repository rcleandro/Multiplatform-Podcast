package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExplicitRuleTest {
    private val clean = Episode(
        id = "e", podcastId = "p", title = "e", description = null, audioUrl = "a", imageUrl = null, duration = 0,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
    )

    @Test
    fun hiddenMeansTheEpisodeOrItsWholePodcastMarked() {
        val rule = ExplicitRule(hidesExplicit = true, explicitPodcasts = setOf("adult"))

        assertTrue(rule.allows(clean))
        assertFalse(rule.allows(clean.copy(explicit = true)))
        assertFalse(rule.allows(clean.copy(podcastId = "adult")))
    }

    @Test
    fun anAdultOrSomeoneOutsideBrazilSeesEverything() {
        val rule = ExplicitRule(hidesExplicit = false, explicitPodcasts = setOf("adult"))

        assertTrue(rule.allows(clean.copy(explicit = true)))
        assertTrue(rule.allows(clean.copy(podcastId = "adult")))
    }
}
