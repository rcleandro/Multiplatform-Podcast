package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.data.remote.FakeRssFeedDataSource
import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastError
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

class AddPodcastFromUrlUseCaseTest {
    private val rssDataSource = FakeRssFeedDataSource()
    private val podcastRepo = FakePodcastRepository()
    private val useCase = AddPodcastFromUrlUseCase(rssDataSource, podcastRepo)

    @Test
    fun `returns AlreadyExists if podcast is already in database`() = runTest {
        val existingPodcast = Podcast(
            id = "test-url",
            title = "Existing",
            description = "",
            imageUrl = null,
            author = null,
            language = null,
            categories = emptyList(),
            feedUrl = "test-url",
            siteUrl = null,
            lastUpdated = 0,
            isSubscribed = true,
            episodeCount = 0
        )

        podcastRepo.podcasts.value = listOf(existingPodcast)

        val result = useCase("test-url")

        assertTrue(result.isFailure)
        assertIs<PodcastError.AlreadyExists>(result.exceptionOrNull())
    }

    @Test
    fun `fetches feed and saves podcast and episodes on success`() = runTest {
        val url = "https://test.com/rss"
        val rssFeed = RssFeed(
            title = "New Podcast",
            description = "Desc",
            imageUrl = "img",
            author = "Author",
            language = "en",
            categories = listOf("Tech"),
            link = "link",
            ttl = null,
            episodes = emptyList()
        )

        rssDataSource.feedResult = Result.success(rssFeed)

        val result = useCase(url)

        assertTrue(result.isSuccess)
        assertEquals("New Podcast", result.getOrNull()?.title)
        assertEquals(1, podcastRepo.savePodcastCalledCount)
        assertEquals(1, podcastRepo.saveEpisodesCalledCount)
        assertEquals("New Podcast", podcastRepo.podcasts.value.find { it.feedUrl == url }?.title)
    }
}
