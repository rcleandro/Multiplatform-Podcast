package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakeOpmlFormat
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OpmlUseCasesTest {
    private val repository = FakePodcastRepository()
    private val feedSource = FakeFeedSource()


    @Test
    fun importAddsTheNewFeedsCountsTheKnownAndTheFailedAndReportsProgress() = runTest {
        repository.podcasts.value = listOf(podcast("https://known.example.com/feed"))
        feedSource.result = Result.success(FetchedFeed(podcast("https://new.example.com/feed"), emptyList()))
        val progress = mutableListOf<Pair<Int, Int>>()
        val import = ImportOpmlUseCase(FakeOpmlFormat, AddPodcastFromUrlUseCase(feedSource, repository))

        val result = import(
            "https://new.example.com/feed\nhttp://known.example.com/feed\nnot a url\n",
        ) { done, total -> progress += done to total }

        assertEquals(OpmlImport(added = 1, known = 1, failed = 1), result)
        assertEquals(0 to 3, progress.first())
        assertEquals(3 to 3, progress.last())
    }

    @Test
    fun exportWritesEveryPodcastInTheLibrary() = runTest {
        repository.podcasts.value = listOf(podcast("https://b.example.com/feed"), podcast("https://a.example.com/feed"))

        val export = ExportOpmlUseCase(repository, FakeOpmlFormat)()

        assertEquals(setOf("https://a.example.com/feed", "https://b.example.com/feed"), export.opml.lines().toSet())
        assertEquals(2, export.podcasts)
    }

    private fun podcast(feed: String) = Podcast(
        id = feed, title = feed, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = feed, siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )
}
