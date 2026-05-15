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
            lastUpdated = 0
        )

        coEvery { podcastRepo.getPodcastById("test-url") } returns existingPodcast

        val result = useCase("test-url")

        assertTrue(result.isFailure)
        assertIs<PodcastError.AlreadyExists>(result.exceptionOrNull())
    }
}
