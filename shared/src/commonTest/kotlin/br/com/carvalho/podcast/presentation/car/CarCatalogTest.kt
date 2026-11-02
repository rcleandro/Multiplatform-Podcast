package br.com.carvalho.podcast.presentation.car

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.usecase.ExplicitContent
import br.com.carvalho.podcast.domain.usecase.ExplicitRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CarCatalogTest {
    private val repository = FakePodcastRepository()
    private val player = FakeAudioPlayer()

    @Test
    fun theCarListsTheLibraryAndPlaysAnEpisode() = runTest {
        repository.podcasts.value = listOf(podcast("show"))
        repository.episodes.value = listOf(episode("one", "show"), episode("other", "elsewhere"))
        val catalog = CarCatalog(repository, player)

        assertEquals(listOf("show"), catalog.podcasts().map { it.id })
        assertEquals(listOf("one"), catalog.episodes("show").map { it.id })
        catalog.play("one")
        assertEquals("one", player.playCalledWith?.id)
    }

    @Test
    fun underTheEcaDigitalTheCarShowsNothingExplicit() = runTest {
        repository.podcasts.value = listOf(podcast("show"), podcast("adult").copy(explicit = true))
        repository.episodes.value = listOf(episode("clean", "show"), episode("swearing", "show").copy(explicit = true))
        val rule = ExplicitRule(hidesExplicit = true, explicitPodcasts = setOf("adult"))
        val catalog = CarCatalog(repository, player, ExplicitContent(MutableStateFlow(rule)))

        assertEquals(listOf("show"), catalog.podcasts().map { it.id })
        assertEquals(listOf("clean"), catalog.episodes("show").map { it.id })
    }

    private fun podcast(id: String) = Podcast(
        id = id, title = id, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = id, siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )

    private fun episode(id: String, podcastId: String) = Episode(
        id = id, podcastId = podcastId, title = id, description = null, audioUrl = "a", imageUrl = null, duration = 60,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
    )
}
