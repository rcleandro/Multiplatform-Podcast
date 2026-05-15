package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.data.remote.RssFeedDataSource
import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastError
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

class AddPodcastFromUrlUseCaseTest {
    private val rssDataSource = mockk<RssFeedDataSource>()
    private val podcastRepo = mockk<PodcastRepository>()
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

        coEvery { podcastRepo.getPodcastById("test-url") } returns existingPodcast

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

        coEvery { podcastRepo.getPodcastById(url) } returns null
        coEvery { rssDataSource.fetchFeed(url) } returns Result.success(rssFeed)
        coEvery { podcastRepo.savePodcast(any()) } returns Unit
        coEvery { podcastRepo.saveEpisodes(any()) } returns Unit

        val result = useCase(url)

        assertTrue(result.isSuccess)
        assertEquals("New Podcast", result.getOrNull()?.title)
        io.mockk.coVerify {
            podcastRepo.savePodcast(match { it.title == "New Podcast" })
            podcastRepo.saveEpisodes(any())
        }
    }
}
