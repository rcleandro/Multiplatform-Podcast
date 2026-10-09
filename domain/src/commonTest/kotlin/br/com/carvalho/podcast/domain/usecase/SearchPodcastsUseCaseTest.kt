package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.repository.FakePodcastDirectory
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchPodcastsUseCaseTest {
    private val directory = FakePodcastDirectory()
    private val repository = FakePodcastRepository()
    private val search = SearchPodcastsUseCase(directory, repository)

    @Test
    fun aShortTermDoesNotReachTheDirectory() = runTest {
        assertEquals(emptyList(), search(" a ").getOrThrow())
        assertEquals(emptyList(), directory.searchedTerms)
    }

    @Test
    fun theTermIsTrimmedAndResultsInTheLibraryAreMarked() = runTest {
        // Saved over http with www.: still the same feed as the directory's https address.
        repository.podcasts.value = listOf(saved("http://www.hipsters.tech/feed/podcast"))
        directory.result = Result.success(listOf(found("https://hipsters.tech/feed/podcast/"), found("https://other.fm/rss")))

        val results = search("  hipsters ").getOrThrow()

        assertEquals(listOf("hipsters"), directory.searchedTerms)
        assertEquals(listOf(true, false), results.map { it.isSubscribed })
    }

    @Test
    fun theChartOfACategoryIsMarkedLikeASearch() = runTest {
        repository.podcasts.value = listOf(saved("https://hipsters.tech/feed/podcast/"))
        directory.topResult = Result.success(listOf(found("https://other.fm/rss"), found("https://hipsters.tech/feed/podcast/")))

        val chart: List<DirectoryResult> = search.top(PodcastCategory.TECHNOLOGY).getOrThrow()

        assertEquals(listOf<PodcastCategory?>(PodcastCategory.TECHNOLOGY), directory.chartsAsked)
        assertEquals(listOf(false, true), chart.map { it.isSubscribed })
    }

    @Test
    fun aDirectoryFailureIsPassedOn() = runTest {
        directory.result = Result.failure(AppError.NoConnection)

        assertEquals(AppError.NoConnection, search("news").exceptionOrNull())
    }

    private fun found(feedUrl: String) = DirectoryPodcast(
        title = feedUrl, author = null, artworkUrl = null, feedUrl = feedUrl, genre = null, episodeCount = null,
    )

    private fun saved(feedUrl: String) = Podcast(
        id = feedUrl, title = "Saved", description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = feedUrl, siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )
}
