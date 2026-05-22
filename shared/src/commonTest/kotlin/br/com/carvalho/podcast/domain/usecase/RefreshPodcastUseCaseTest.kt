package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.data.remote.FakeRssFeedDataSource
import br.com.carvalho.podcast.data.remote.model.RssEpisode
import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefreshPodcastUseCaseTest {
    private val rssDataSource = FakeRssFeedDataSource()
    private val podcastRepo = FakePodcastRepository()
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
        podcastRepo.podcasts.value = listOf(samplePodcast)
        rssDataSource.feedResult = Result.success(sampleFeed)

        val result = useCase("url")

        assertTrue(result.isSuccess)
        assertEquals(1, podcastRepo.savePodcastCalledCount)
        assertEquals(1, podcastRepo.saveEpisodesCalledCount)
    }

    @Test
    fun `refreshAll updates all subscribed podcasts`() = runTest {
        podcastRepo.podcasts.value = listOf(samplePodcast)
        rssDataSource.feedResult = Result.success(sampleFeed)

        val result = useCase.refreshAll()

        assertTrue(result.isSuccess)
        assertEquals("url", rssDataSource.fetchFeedCalledWith)
    }
}
