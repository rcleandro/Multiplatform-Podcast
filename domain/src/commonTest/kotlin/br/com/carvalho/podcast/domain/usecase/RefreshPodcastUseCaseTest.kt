package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.FeedVersion
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

    @Test
    fun `a feed that did not change is not saved again`() = runTest {
        val version = FeedVersion(etag = "\"v1\"", lastModified = "Mon, 05 Oct 2026 10:00:00 GMT")
        podcastRepo.podcasts.value = listOf(samplePodcast.copy(feedVersion = version))
        feedSource.notModified = true

        val result = useCase(samplePodcast.id)

        assertTrue(result.isSuccess)
        assertEquals(version, feedSource.versionAskedFor)
        assertEquals(0, podcastRepo.saveFeedCalledCount)
    }

    @Test
    fun `refreshAll fetches at most four feeds at a time`() = runTest {
        podcastRepo.podcasts.value = (1..10).map { samplePodcast.copy(id = "url$it", feedUrl = "url$it") }
        feedSource.result = Result.success(sampleFeed)
        feedSource.delayMs = 100

        val summary = useCase.refreshAll()

        assertEquals(10, summary.total)
        assertEquals(4, feedSource.maxConcurrentFetches)
    }

    @Test
    fun `a feed that moved keeps its id and episodes and gets the new address`() = runTest {
        val original = samplePodcast.copy(id = "https://old.example.com/rss", feedUrl = "https://old.example.com/rss")
        podcastRepo.podcasts.value = listOf(original)
        feedSource.result = Result.success(sampleFeed.copy(movedTo = "https://new.example.com/rss"))

        useCase(original.id)

        assertEquals(original.id, feedSource.podcastIdAskedFor)
        val saved = podcastRepo.podcasts.value.single()
        assertEquals(original.id, saved.id)
        assertEquals("https://new.example.com/rss", saved.feedUrl)
    }

    @Test
    fun `a podcast whose address moved before is fetched at the new one under its old id`() = runTest {
        val moved = samplePodcast.copy(id = "https://old.example.com/rss", feedUrl = "https://new.example.com/rss")
        podcastRepo.podcasts.value = listOf(moved)
        feedSource.result = Result.success(sampleFeed)

        useCase(moved.id)

        assertEquals("https://new.example.com/rss", feedSource.fetchCalledWith)
        assertEquals(moved.id, feedSource.podcastIdAskedFor)
        assertEquals(moved, podcastRepo.podcasts.value.single().copy(lastUpdated = moved.lastUpdated))
    }
}
