package br.com.carvalho.podcast.feature.discover.presentation

import app.cash.turbine.test
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.observability.CrashReporter
import br.com.carvalho.podcast.core.observability.FakeAnalytics
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.error_invalid_feed
import br.com.carvalho.podcast.core.ui.generated.resources.error_invalid_url
import br.com.carvalho.podcast.core.ui.generated.resources.error_open_link
import br.com.carvalho.podcast.core.ui.generated.resources.error_podcast_exists
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastDirectory
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.DirectoryResult
import br.com.carvalho.podcast.domain.usecase.OpenPodcastLinkUseCase
import br.com.carvalho.podcast.domain.usecase.SearchPodcastsUseCase
import br.com.carvalho.podcast.presentation.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val directory = FakePodcastDirectory()
    private val repository = FakePodcastRepository()
    private val feedSource = FakeFeedSource()
    private val analytics = FakeAnalytics()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        AppLogger.crashReporter = null
    }

    private fun viewModel() = DiscoverViewModel(
        SearchPodcastsUseCase(directory, repository),
        AddPodcastFromUrlUseCase(feedSource, repository),
        OpenPodcastLinkUseCase(directory, feedSource, repository),
        CoroutineDispatchers(main = dispatcher, io = dispatcher, default = dispatcher),
        analytics,
    )

    private fun TestScope.waitForDebounce() = advanceTimeBy(AppConfig.SEARCH_DEBOUNCE_MS + 1)

    @Test
    fun `typing searches the directory once the user pauses`() = runTest(dispatcher) {
        directory.result = Result.success(listOf(found(HIPSTERS)))
        val viewModel = viewModel()

        viewModel.onIntent(DiscoverIntent.ChangeQuery("hip"))
        viewModel.onIntent(DiscoverIntent.ChangeQuery("hipsters"))
        waitForDebounce()

        assertEquals(listOf("hipsters"), directory.searchedTerms)
        assertEquals(listOf(HIPSTERS), viewModel.uiState.value.results.map { it.podcast.feedUrl })
        // The term never reaches analytics, only how many results came back.
        assertEquals(listOf("search_directory"), analytics.events)
        assertEquals(mapOf<String, Any?>("results" to 1), analytics.params.single())
    }

    @Test
    fun `clearing the search shows the popular podcasts again`() = runTest(dispatcher) {
        directory.result = Result.success(listOf(found(HIPSTERS)))
        directory.topResult = Result.success(listOf(found(OTHER)))
        val viewModel = viewModel()
        viewModel.onIntent(DiscoverIntent.ChangeQuery("hipsters"))
        waitForDebounce()

        viewModel.onIntent(DiscoverIntent.ChangeQuery(""))
        waitForDebounce()

        assertEquals(listOf(OTHER), viewModel.uiState.value.results.map { it.podcast.feedUrl })
        assertEquals(1, directory.searchedTerms.size)
    }

    @Test
    fun `a failed search can be tried again with the same term`() = runTest(dispatcher) {
        directory.result = Result.failure(AppError.NoConnection)
        val viewModel = viewModel()
        viewModel.onIntent(DiscoverIntent.ChangeQuery("news"))
        waitForDebounce()
        assertTrue(viewModel.uiState.value.searchFailed)

        directory.result = Result.success(listOf(found(HIPSTERS)))
        viewModel.onIntent(DiscoverIntent.Retry)
        waitForDebounce()

        assertEquals(listOf("news", "news"), directory.searchedTerms)
        assertFalse(viewModel.uiState.value.searchFailed)
        assertEquals(1, viewModel.uiState.value.results.size)
    }

    @Test
    fun `the screen opens on the popular podcasts`() = runTest(dispatcher) {
        directory.topResult = Result.success(listOf(found(HIPSTERS)))

        val viewModel = viewModel()

        assertEquals(listOf<PodcastCategory?>(null), directory.chartsAsked)
        assertEquals(listOf(HIPSTERS), viewModel.uiState.value.results.map { it.podcast.feedUrl })
    }

    @Test
    fun `a category shows its chart at once and a second tap goes back to the popular ones`() = runTest(dispatcher) {
        directory.topResult = Result.success(listOf(found(HIPSTERS), found(OTHER)))
        val viewModel = viewModel()

        viewModel.onIntent(DiscoverIntent.SelectCategory(PodcastCategory.TECHNOLOGY))

        assertEquals(listOf(null, PodcastCategory.TECHNOLOGY), directory.chartsAsked)
        assertEquals(2, viewModel.uiState.value.results.size)
        assertEquals(listOf("browse_category"), analytics.events)
        assertEquals(mapOf<String, Any?>("category" to "technology"), analytics.params.single())

        viewModel.onIntent(DiscoverIntent.SelectCategory(null))

        assertNull(viewModel.uiState.value.category)
        assertEquals(listOf(null, PodcastCategory.TECHNOLOGY, null), directory.chartsAsked)
    }

    @Test
    fun `typing searches even with a category open`() = runTest(dispatcher) {
        directory.result = Result.success(listOf(found(OTHER)))
        val viewModel = viewModel()
        viewModel.onIntent(DiscoverIntent.SelectCategory(PodcastCategory.NEWS))

        viewModel.onIntent(DiscoverIntent.ChangeQuery("other"))
        waitForDebounce()

        assertEquals(listOf("other"), directory.searchedTerms)
        assertEquals(listOf(OTHER), viewModel.uiState.value.results.map { it.podcast.feedUrl })
    }

    @Test
    fun `subscribing adds the podcast and marks the result`() = runTest(dispatcher) {
        directory.result = Result.success(listOf(found(HIPSTERS), found(OTHER)))
        feedSource.result = Result.success(FetchedFeed(podcast(HIPSTERS), emptyList()))
        val viewModel = viewModel()
        viewModel.onIntent(DiscoverIntent.ChangeQuery("hipsters"))
        waitForDebounce()

        viewModel.onIntent(DiscoverIntent.Subscribe(viewModel.uiState.value.results.first()))

        assertEquals(HIPSTERS, feedSource.fetchCalledWith)
        assertEquals(listOf(true, false), viewModel.uiState.value.results.map { it.isSubscribed })
        assertEquals(emptySet(), viewModel.uiState.value.subscribing)
    }

    @Test
    fun `a result already in the library is shown as subscribed and not added again`() = runTest(dispatcher) {
        repository.podcasts.value = listOf(podcast(HIPSTERS))
        directory.result = Result.success(listOf(found(HIPSTERS)))
        val viewModel = viewModel()
        viewModel.onIntent(DiscoverIntent.ChangeQuery("hipsters"))
        waitForDebounce()

        viewModel.onIntent(DiscoverIntent.Subscribe(viewModel.uiState.value.results.single()))

        assertTrue(viewModel.uiState.value.results.single().isSubscribed)
        assertNull(feedSource.fetchCalledWith)
    }

    @Test
    fun `a podcast added by its address closes the screen`() = runTest(dispatcher) {
        feedSource.result = Result.success(FetchedFeed(podcast(HIPSTERS), emptyList()))
        val viewModel = viewModel()

        viewModel.addedByUrl.test {
            viewModel.onIntent(DiscoverIntent.OpenUrlDialog)
            viewModel.onIntent(DiscoverIntent.ChangeUrl("hipsters.tech/feed/podcast/"))
            viewModel.onIntent(DiscoverIntent.ConfirmUrl)

            awaitItem()
        }
        assertEquals("https://hipsters.tech/feed/podcast/", feedSource.fetchCalledWith)
        assertFalse(viewModel.uiState.value.isUrlDialogOpen)
        assertEquals("", viewModel.uiState.value.url)
    }

    @Test
    fun `adding a podcast already in the library shows a specific message`() = runTest(dispatcher) {
        repository.podcasts.value = listOf(podcast("https://feed.example/rss"))
        val viewModel = viewModel()

        addByUrl(viewModel, "https://feed.example/rss")

        viewModel.messages.test { assertEquals(UiMessage(Res.string.error_podcast_exists), awaitItem()) }
    }

    @Test
    fun `an address that is not a URL says so without fetching`() = runTest(dispatcher) {
        val viewModel = viewModel()

        addByUrl(viewModel, "https://jovemnerd.com.br/feed-nerdcast|")

        viewModel.messages.test { assertEquals(UiMessage(Res.string.error_invalid_url), awaitItem()) }
        assertNull(feedSource.fetchCalledWith)
    }

    @Test
    fun `adding a URL that is not a feed says so`() = runTest(dispatcher) {
        feedSource.result = Result.failure(AppError.InvalidFeed)
        val viewModel = viewModel()

        addByUrl(viewModel, "https://example.com")

        viewModel.messages.test { assertEquals(UiMessage(Res.string.error_invalid_feed), awaitItem()) }
    }

    @Test
    fun `events and logs never carry the feed URL`() = runTest(dispatcher) {
        val logs = mutableListOf<String>()
        AppLogger.crashReporter = object : CrashReporter {
            override fun log(message: String) { logs += message }
            override fun recordException(throwable: Throwable) { logs += throwable.stackTraceToString() }
        }
        feedSource.result = Result.failure(AppError.InvalidFeed)
        val viewModel = viewModel()

        // Typed without the scheme: still recognized as an address, and cut to its host.
        addByUrl(viewModel, "feeds.example.com/private/rss?token=s3cr3t")

        assertEquals(listOf("add_podcast_attempt", "add_podcast_failure"), analytics.events)
        assertTrue(analytics.params.all { it["host"] == "feeds.example.com" }, analytics.params.toString())
        assertTrue(logs.isNotEmpty())
        assertTrue(logs.none { "s3cr3t" in it }, logs.joinToString("\n"))
    }

    private fun addByUrl(viewModel: DiscoverViewModel, url: String) {
        viewModel.onIntent(DiscoverIntent.OpenUrlDialog)
        viewModel.onIntent(DiscoverIntent.ChangeUrl(url))
        viewModel.onIntent(DiscoverIntent.ConfirmUrl)
    }

    @Test
    fun `a link shows its podcast to subscribe to`() = runTest(dispatcher) {
        directory.lookupResult = Result.success(found(HIPSTERS))
        feedSource.result = Result.success(FetchedFeed(podcast(HIPSTERS), emptyList()))
        val viewModel = viewModel()

        viewModel.onIntent(DiscoverIntent.OpenLink("https://podcasts.apple.com/br/podcast/hipsters/id1133325943"))

        assertEquals(DirectoryResult(found(HIPSTERS), isSubscribed = false), viewModel.uiState.value.linked)
        assertFalse(viewModel.uiState.value.isOpeningLink)
        assertNull(feedSource.fetchCalledWith)
        assertEquals(listOf("open_podcast_link"), analytics.events)
        assertEquals("hipsters.tech", analytics.params.single()["host"])

        viewModel.onIntent(DiscoverIntent.Subscribe(viewModel.uiState.value.linked!!))

        assertEquals(true, viewModel.uiState.value.linked?.isSubscribed)
        assertEquals(listOf(HIPSTERS), repository.podcasts.value.map { it.feedUrl })
    }

    @Test
    fun `a link to a podcast in the library opens it`() = runTest(dispatcher) {
        repository.podcasts.value = listOf(podcast(HIPSTERS))
        val viewModel = viewModel()

        viewModel.openPodcast.test {
            viewModel.onIntent(DiscoverIntent.OpenLink("pcast://hipsters.tech/feed/podcast"))

            assertEquals(HIPSTERS, awaitItem())
        }
        assertNull(viewModel.uiState.value.linked)
    }

    @Test
    fun `a link that leads nowhere says so`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(DiscoverIntent.OpenLink("https://podcasts.apple.com/br/podcast/id1"))

        viewModel.messages.test { assertEquals(UiMessage(Res.string.error_open_link), awaitItem()) }
        assertNull(viewModel.uiState.value.linked)
        assertFalse(viewModel.uiState.value.isOpeningLink)
    }

    @Test
    fun `a link's podcast is forgotten when the screen closes`() = runTest(dispatcher) {
        directory.lookupResult = Result.success(found(HIPSTERS))
        val viewModel = viewModel()
        viewModel.onIntent(DiscoverIntent.OpenLink("https://podcasts.apple.com/br/podcast/id1"))

        viewModel.onIntent(DiscoverIntent.ForgetLink)

        assertNull(viewModel.uiState.value.linked)
    }

    private fun found(feedUrl: String) = DirectoryPodcast(
        title = feedUrl, author = null, artworkUrl = null, feedUrl = feedUrl, genre = null, episodeCount = null,
    )

    private fun podcast(feedUrl: String) = Podcast(
        id = feedUrl, title = "Podcast", description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = feedUrl, siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )

    private companion object {
        const val HIPSTERS = "https://hipsters.tech/feed/podcast/"
        const val OTHER = "https://other.fm/rss"
    }
}
