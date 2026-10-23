package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastDirectory
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenPodcastLinkUseCaseTest {
    private val directory = FakePodcastDirectory()
    private val feedSource = FakeFeedSource()
    private val podcastRepo = FakePodcastRepository()
    private val useCase = OpenPodcastLinkUseCase(directory, feedSource, podcastRepo)

    @Test
    fun anApplePodcastsLinkComesFromTheDirectory() = runTest {
        directory.lookupResult = Result.success(FOUND)

        val linked = useCase("https://podcasts.apple.com/br/podcast/hipsters/id1133325943").getOrThrow()

        assertEquals(LinkedPodcast(FOUND, libraryId = null), linked)
        assertEquals(listOf(1_133_325_943L), directory.lookedUp)
    }

    @Test
    fun anApplePodcastsShowInTheLibraryGivesItsId() = runTest {
        podcastRepo.podcasts.value = listOf(podcast("https://www.hipsters.tech/feed/podcast"))
        directory.lookupResult = Result.success(FOUND)

        val linked = useCase("podcasts://podcasts.apple.com/us/podcast/id1133325943").getOrThrow()

        assertEquals("https://www.hipsters.tech/feed/podcast", linked.libraryId)
    }

    @Test
    fun anApplePodcastsShowWithoutAFeedIsNotFound() = runTest {
        val result = useCase("https://podcasts.apple.com/us/podcast/id42")

        assertEquals(AppError.NotFound, result.exceptionOrNull())
    }

    @Test
    fun underTheEcaDigitalAnExplicitPodcastsLinkIsNotFound() = runTest {
        feedSource.result = Result.success(
            FetchedFeed(podcast("https://example.com/rss").copy(explicit = true), episodes = emptyList())
        )
        val hidden = OpenPodcastLinkUseCase(
            directory, feedSource, podcastRepo, ExplicitContent(MutableStateFlow(ExplicitRule(hidesExplicit = true)))
        )

        assertEquals(AppError.NotFound, hidden("pcast://example.com/rss").exceptionOrNull())
        assertEquals(true, useCase("pcast://example.com/rss").getOrThrow().podcast.explicit)
    }

    @Test
    fun aFeedLinkIsReadToShowWhatItIs() = runTest {
        feedSource.result = Result.success(
            FetchedFeed(podcast("https://example.com/rss").copy(title = "Daily"), episodes = emptyList())
        )

        val linked = useCase("pcast://example.com/rss").getOrThrow()

        assertEquals("https://example.com/rss", feedSource.fetchCalledWith)
        assertEquals("Daily", linked.podcast.title)
        assertEquals("https://example.com/rss", linked.podcast.feedUrl)
        assertEquals(null, linked.libraryId)
    }

    @Test
    fun aFeedInTheLibraryIsNotRead() = runTest {
        podcastRepo.podcasts.value = listOf(podcast("https://example.com/rss/"))

        val linked = useCase("feed:http://example.com/rss").getOrThrow()

        assertEquals("https://example.com/rss/", linked.libraryId)
        assertEquals(null, feedSource.fetchCalledWith)
    }

    @Test
    fun aFeedThatDeclaresOneInTheLibraryIsThatOne() = runTest {
        podcastRepo.podcasts.value = listOf(podcast("https://new.example.com/rss"))
        feedSource.result = Result.success(
            FetchedFeed(
                podcast("https://old.example.com/rss"),
                emptyList(),
                declaredUrls = listOf("https://new.example.com/rss"),
            )
        )

        val linked = useCase("https://old.example.com/rss").getOrThrow()

        assertEquals("https://new.example.com/rss", linked.libraryId)
    }

    @Test
    fun anythingElseIsNotFoundWithoutTheNetwork() = runTest {
        val result = useCase("https://podcasts.apple.com/br/browse")

        assertEquals(AppError.NotFound, result.exceptionOrNull())
        assertEquals(emptyList(), directory.lookedUp)
        assertEquals(null, feedSource.fetchCalledWith)
    }

    private fun podcast(feedUrl: String) = Podcast(
        id = feedUrl, title = "P", description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = feedUrl, siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )

    private companion object {
        val FOUND = DirectoryPodcast(
            title = "Hipsters Ponto Tech",
            author = "Alura",
            artworkUrl = null,
            feedUrl = "https://hipsters.tech/feed/podcast/",
            genre = "Tecnologia",
            episodeCount = 629,
        )
    }
}
