package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

class AddPodcastFromUrlUseCaseTest {
    private val feedSource = FakeFeedSource()
    private val podcastRepo = FakePodcastRepository()
    private val useCase = AddPodcastFromUrlUseCase(feedSource, podcastRepo)

    @Test
    fun `returns AlreadyExists if podcast is already in database`() = runTest {
        val existingPodcast = Podcast(
            id = FEED_URL,
            title = "Existing",
            description = "",
            imageUrl = null,
            author = null,
            language = null,
            categories = emptyList(),
            feedUrl = FEED_URL,
            siteUrl = null,
            lastUpdated = 0,
            isSubscribed = true,
            episodeCount = 0
        )

        podcastRepo.podcasts.value = listOf(existingPodcast)

        val result = useCase(FEED_URL)

        assertTrue(result.isFailure)
        assertIs<AppError.AlreadyExists>(result.exceptionOrNull())
    }

    @Test
    fun `the same feed written another way is already in the library`() = runTest {
        podcastRepo.podcasts.value = listOf(podcast("https://www.hipsters.tech/feed/podcast/"))

        val result = useCase("hipsters.tech/feed/podcast")

        assertEquals(AppError.AlreadyExists, result.exceptionOrNull())
        assertEquals(null, feedSource.fetchCalledWith)
    }

    @Test
    fun `a feed that declares the address of a saved one is already in the library`() = runTest {
        podcastRepo.podcasts.value = listOf(podcast("https://feeds.simplecast.com/54nAGcIl"))
        feedSource.result = Result.success(
            FetchedFeed(
                podcast("https://old-host.example.com/daily"), emptyList(),
                declaredUrls = listOf("https://feeds.simplecast.com/54nAGcIl"),
            )
        )

        val result = useCase("https://old-host.example.com/daily")

        assertEquals(AppError.AlreadyExists, result.exceptionOrNull())
        assertEquals(0, podcastRepo.saveFeedCalledCount)
    }

    @Test
    fun `a feed that moved is saved at its new address`() = runTest {
        feedSource.result = Result.success(
            FetchedFeed(podcast(FEED_URL), emptyList(), movedTo = "https://new.example.com/rss")
        )

        val saved = useCase(FEED_URL).getOrThrow()

        assertEquals("https://new.example.com/rss", saved.feedUrl)
        assertEquals("https://new.example.com/rss", podcastRepo.podcasts.value.single().feedUrl)
    }

    @Test
    fun `an invalid address fails without fetching`() = runTest {
        val result = useCase("not a feed")

        assertEquals(AppError.InvalidUrl, result.exceptionOrNull())
        assertEquals(null, feedSource.fetchCalledWith)
    }

    @Test
    fun `fetches feed and saves podcast and episodes on success`() = runTest {
        val url = "https://test.com/rss"
        val podcast = Podcast(
            id = url, title = "New Podcast", description = "Desc", imageUrl = "img", author = "Author",
            language = "en", categories = listOf("Tech"), feedUrl = url, siteUrl = "link", lastUpdated = 0,
            isSubscribed = true
        )
        feedSource.result = Result.success(FetchedFeed(podcast, emptyList()))

        val result = useCase(url)

        assertTrue(result.isSuccess)
        assertEquals("New Podcast", result.getOrNull()?.title)
        assertEquals(1, podcastRepo.saveFeedCalledCount)
        assertEquals("New Podcast", podcastRepo.podcasts.value.find { it.feedUrl == url }?.title)
    }

    private fun podcast(feedUrl: String) = Podcast(
        id = feedUrl, title = "P", description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = feedUrl, siteUrl = null, lastUpdated = 0, isSubscribed = true
    )

    private companion object {
        const val FEED_URL = "https://feeds.example.com/rss"
    }
}
