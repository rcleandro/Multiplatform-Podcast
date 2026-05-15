package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.data.remote.RssFeedDataSource
import br.com.carvalho.podcast.data.remote.model.RssEpisode
import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class RefreshPodcastUseCaseTest {
    private val rssDataSource = mockk<RssFeedDataSource>()
    private val podcastRepo = mockk<PodcastRepository>(relaxed = true)
    private val useCase = RefreshPodcastUseCase(rssDataSource, podcastRepo)

    private val samplePodcast = Podcast(
        id = "url",
        title = "Title",
        description = "",
        imageUrl = null,
        author = null,
        language = null,
        categories = emptyList(),
        feedUrl = "url",
        siteUrl = null,
        lastUpdated = 0,
        isSubscribed = true
    )

    private val sampleFeed = RssFeed(
        title = "Title",
        description = "",
        imageUrl = null,
        author = null,
        language = null,
        categories = emptyList(),
        link = null,
        ttl = null,
        episodes = listOf(
            RssEpisode(
                guid = "guid",
                title = "Ep",
                description = null,
                enclosureUrl = "audio",
                enclosureType = null,
                duration = null,
                publishDate = "Fri, 15 May 2026 10:00:00 GMT",
                imageUrl = null,
                explicit = false,
                season = null,
                episode = null
            )
        )
    )

    @Test
    fun `invoking refresh updates podcast and episodes`() = runTest {
        coEvery { podcastRepo.getPodcastById("url") } returns samplePodcast
        coEvery { rssDataSource.fetchFeed("url") } returns Result.success(sampleFeed)

        val result = useCase("url")

        assertTrue(result.isSuccess)
        coVerify { podcastRepo.savePodcast(any()) }
        coVerify { podcastRepo.saveEpisodes(any()) }
    }

    @Test
    fun `refreshAll updates all subscribed podcasts`() = runTest {
        coEvery { podcastRepo.getPodcasts() } returns flowOf(listOf(samplePodcast))
        coEvery { podcastRepo.getPodcastById("url") } returns samplePodcast
        coEvery { rssDataSource.fetchFeed("url") } returns Result.success(sampleFeed)

        val result = useCase.refreshAll()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { rssDataSource.fetchFeed("url") }
    }
}
