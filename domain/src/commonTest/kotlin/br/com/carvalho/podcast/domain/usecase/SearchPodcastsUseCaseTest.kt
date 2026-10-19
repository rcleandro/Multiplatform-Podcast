package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.repository.FakePodcastDirectory
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchPodcastsUseCaseTest {
    private val directory = FakePodcastDirectory()
    private val repository = FakePodcastRepository()
    private val preferences = FakePreferencesRepository()
    private val search = SearchPodcastsUseCase(directory, repository, preferences)

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
    fun explicitShowsStayOutWhenTheUserTurnsThemOff() = runTest {
        directory.result = Result.success(
            listOf(found("https://clean.fm/rss"), found("https://explicit.fm/rss").copy(explicit = true))
        )
        assertEquals(2, search("show").getOrThrow().size)

        preferences.setShowExplicit(false)

        assertEquals(listOf("https://clean.fm/rss"), search("show").getOrThrow().map { it.podcast.feedUrl })
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
    fun theSameFeedListedTwiceShowsOnce() = runTest {
        // Apple sometimes lists one feed under two entries; the screen keys its rows by feed.
        val twice = listOf(
            found("https://anchor.fm/s/5f4dd230/podcast/rss"),
            found("https://other.fm/rss"),
            found("http://anchor.fm/s/5f4dd230/podcast/rss/"),
        )
        directory.result = Result.success(twice)
        directory.topResult = Result.success(twice)

        assertEquals(2, search("anchor").getOrThrow().size)
        assertEquals(
            listOf("https://anchor.fm/s/5f4dd230/podcast/rss", "https://other.fm/rss"),
            search.top(null).getOrThrow().map { it.podcast.feedUrl },
        )
    }

    @Test
    fun aDirectoryFailureIsPassedOn() = runTest {
        directory.result = Result.failure(AppError.NoConnection)

        assertEquals(AppError.NoConnection, search("news").exceptionOrNull())
    }

    @Test
    fun similarPodcastsAreTheAuthorsOtherShowsThenItsCategorysChartWithoutWhatTheLibraryHas() = runTest {
        val podcast = saved("https://a.fm/rss").copy(id = "a", author = "Alura", categories = listOf("Technology"))
        repository.podcasts.value = listOf(podcast, saved("https://have.fm/rss"))
        directory.result = Result.success(
            listOf(
                found("https://a.fm/rss").copy(author = "Alura"),
                found("https://other-by-alura.fm/rss").copy(author = "alura"),
                found("https://someone-else.fm/rss").copy(author = "Someone"),
            )
        )
        directory.topByCategory[PodcastCategory.TECHNOLOGY] =
            Result.success(listOf(found("https://have.fm/rss"), found("https://tech.fm/rss")))

        val similar = search.similarTo("a")?.results?.getOrThrow()

        assertEquals(listOf("Alura"), directory.searchedTerms)
        assertEquals(listOf("https://other-by-alura.fm/rss", "https://tech.fm/rss"), similar?.map { it.podcast.feedUrl })
        assertEquals(null, search.similarTo("not in the library"))
    }

    @Test
    fun similarPodcastsFailOnlyWhenNothingCouldBeAsked() = runTest {
        repository.podcasts.value = listOf(saved("https://a.fm/rss").copy(id = "a", author = "Alura", categories = listOf("News")))
        directory.result = Result.failure(AppError.NoConnection)
        directory.topResult = Result.success(listOf(found("https://news.fm/rss")))
        assertEquals(1, search.similarTo("a")?.results?.getOrThrow()?.size)

        directory.topResult = Result.failure(AppError.NoConnection)
        assertTrue(search.similarTo("a")?.results?.isFailure == true)
    }

    @Test
    fun forTheLibraryTheChartsOfItsTwoMostCommonCategoriesTakeTurns() = runTest {
        repository.podcasts.value = listOf(
            saved("https://t1.fm/rss").copy(categories = listOf("Technology")),
            saved("https://t2.fm/rss").copy(categories = listOf("Technology")),
            saved("https://n1.fm/rss").copy(categories = listOf("News")),
            saved("https://c1.fm/rss").copy(categories = listOf("Comedy")),
        )
        directory.topByCategory[PodcastCategory.TECHNOLOGY] =
            Result.success(listOf(found("https://tech-a.fm/rss"), found("https://t1.fm/rss"), found("https://tech-b.fm/rss")))
        directory.topByCategory[PodcastCategory.NEWS] = Result.success(listOf(found("https://news-a.fm/rss")))

        val forYou = search.forLibrary().getOrThrow().map { it.podcast.feedUrl }

        assertEquals(listOf("https://tech-a.fm/rss", "https://news-a.fm/rss", "https://tech-b.fm/rss"), forYou)
        assertEquals(setOf(PodcastCategory.TECHNOLOGY, PodcastCategory.NEWS), directory.chartsAsked.toSet())
    }

    private fun found(feedUrl: String) = DirectoryPodcast(
        title = feedUrl, author = null, artworkUrl = null, feedUrl = feedUrl, genre = null, episodeCount = null,
    )

    private fun saved(feedUrl: String) = Podcast(
        id = feedUrl, title = "Saved", description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = feedUrl, siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )
}
