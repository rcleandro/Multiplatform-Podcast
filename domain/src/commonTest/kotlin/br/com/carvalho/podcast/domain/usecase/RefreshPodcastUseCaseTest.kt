package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefreshPodcastUseCaseTest {
    private val feedSource = FakeFeedSource()
    private val podcastRepo = FakePodcastRepository()
    private val useCase = RefreshPodcastUseCase(feedSource, podcastRepo)

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

    private val sampleFeed = FetchedFeed(
        podcast = samplePodcast,
        episodes = listOf(
            Episode(
                id = "guid", podcastId = "url", title = "Ep", description = null, audioUrl = "audio", imageUrl = null,
                duration = 0, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false,
                fileSize = null
            )
        )
    )

    @Test
    fun `invoking refresh updates podcast and episodes`() = runTest {
        podcastRepo.podcasts.value = listOf(samplePodcast)
        feedSource.result = Result.success(sampleFeed)

        val result = useCase("url")

        assertTrue(result.isSuccess)
        assertEquals(1, podcastRepo.saveFeedCalledCount)
    }

    @Test
    fun `refreshAll updates all subscribed podcasts`() = runTest {
        podcastRepo.podcasts.value = listOf(samplePodcast)
        feedSource.result = Result.success(sampleFeed)

        val summary = useCase.refreshAll()

        assertEquals(RefreshSummary(total = 1, failures = emptyList()), summary)
        assertEquals("url", feedSource.fetchCalledWith)
    }

    @Test
    fun `refreshAll counts the feeds that failed and keeps refreshing the others`() = runTest {
        val other = samplePodcast.copy(id = "other", feedUrl = "other")
        podcastRepo.podcasts.value = listOf(samplePodcast, other)
        feedSource.result = Result.success(sampleFeed)
        feedSource.resultsByUrl["other"] = Result.failure(IllegalStateException("offline"))

        val summary = useCase.refreshAll()

        assertEquals(2, summary.total)
        assertEquals(1, summary.failures.size)
        assertEquals(false, summary.allFailed)
        assertEquals(1, podcastRepo.saveFeedCalledCount)
    }
}
