package br.com.carvalho.podcast.feature.library.presentation

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.observability.FakeAnalytics
import br.com.carvalho.podcast.core.ui.generated.resources.error_refresh_some_podcasts
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.error_no_connection
import app.cash.turbine.test
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.LibraryLayout
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import br.com.carvalho.podcast.domain.usecase.ExplicitContent
import br.com.carvalho.podcast.domain.usecase.ExplicitRule
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import br.com.carvalho.podcast.presentation.UiMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val repository = FakePodcastRepository()
    private val audioPlayer = FakeAudioPlayer()
    private val feedSource = FakeFeedSource()
    private val refreshPodcastUseCase = RefreshPodcastUseCase(feedSource, repository)
    private val deletePodcastUseCase = DeletePodcastUseCase(repository, FakeEpisodeDownloader())
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(main = testDispatcher, io = testDispatcher)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val analytics = FakeAnalytics()

    private val preferences = FakePreferencesRepository()

    private val explicitRule = MutableStateFlow(ExplicitRule())

    private fun createViewModel(): LibraryViewModel {
        return LibraryViewModel(
            repository, refreshPodcastUseCase, deletePodcastUseCase, preferences,
            PlayEpisodeUseCase(audioPlayer), dispatchers, analytics, ExplicitContent(explicitRule)
        )
    }

    @Test
    fun `under the ECA Digital explicit podcasts and episodes leave the library and come back for an adult`() =
        runTest(testDispatcher) {
            fun podcast(id: String) = Podcast(
                id = id, title = id, description = "", imageUrl = null, author = null, language = null,
                categories = emptyList(), feedUrl = id, siteUrl = null, lastUpdated = 0, isSubscribed = true,
            )
            val started = Episode(
                id = "e1", podcastId = "clean", title = "Started", description = null, audioUrl = "a",
                imageUrl = null, duration = 600, publishDate = 0, isPlayed = false, playbackPosition = 120_000,
                isDownloaded = false, fileSize = null,
            )
            repository.podcasts.value = listOf(podcast("clean"), podcast("adult").copy(explicit = true))
            repository.episodes.value = listOf(started, started.copy(id = "e2", explicit = true))
            explicitRule.value = ExplicitRule(hidesExplicit = true, explicitPodcasts = setOf("adult"))
            val viewModel = createViewModel()

            assertEquals(listOf("clean"), viewModel.uiState.value.podcasts.map { it.podcast.id })
            assertEquals(listOf("e1"), viewModel.uiState.value.inProgress.map { it.id })

            explicitRule.value = ExplicitRule()
            assertEquals(listOf("adult", "clean"), viewModel.uiState.value.podcasts.map { it.podcast.id })
            assertEquals(listOf("e1", "e2"), viewModel.uiState.value.inProgress.map { it.id })
        }

    @Test
    fun `continue listening shows the started episodes and plays the tapped one`() = runTest(testDispatcher) {
        val started = Episode(
            id = "e1", podcastId = "p1", title = "Started", description = null, audioUrl = "a", imageUrl = null,
            duration = 600, publishDate = 0, isPlayed = false, playbackPosition = 120_000, isDownloaded = false,
            fileSize = null,
        )
        repository.episodes.value = listOf(started, started.copy(id = "e2", playbackPosition = 0))
        val viewModel = createViewModel()

        assertEquals(listOf("e1"), viewModel.uiState.value.inProgress.map { it.id })
        viewModel.onIntent(LibraryIntent.Play(started))

        assertEquals("e1", audioPlayer.playCalledWith?.id)
    }

    @Test
    fun `the sort choice is saved and orders the library`() = runTest(testDispatcher) {
        fun podcast(title: String, subscribedAt: Long) = Podcast(
            id = title, title = title, description = "", imageUrl = null, author = null, language = null,
            categories = emptyList(), feedUrl = title, siteUrl = null, lastUpdated = 0, isSubscribed = true,
            subscribedAt = subscribedAt,
        )
        repository.podcasts.value = listOf(podcast("A", subscribedAt = 1), podcast("B", subscribedAt = 2))
        val viewModel = createViewModel()

        viewModel.onIntent(LibraryIntent.ChangeSort(LibrarySort.RECENTLY_ADDED))

        assertEquals(LibrarySort.RECENTLY_ADDED, preferences.librarySort.value)
        assertEquals(LibrarySort.RECENTLY_ADDED, viewModel.uiState.value.sort)
        assertEquals(listOf("B", "A"), viewModel.uiState.value.podcasts.map { it.podcast.title })
    }

    @Test
    fun `the layout choice is saved and shown`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(LibraryIntent.ToggleLayout)

        assertEquals(LibraryLayout.LIST, preferences.libraryLayout.value)
        assertEquals(LibraryLayout.LIST, viewModel.uiState.value.layout)

        viewModel.onIntent(LibraryIntent.ToggleLayout)

        assertEquals(LibraryLayout.GRID, viewModel.uiState.value.layout)
    }

    @Test
    fun `the library opens in the saved layout with the unplayed counts`() = runTest(testDispatcher) {
        preferences.setLibraryLayout(LibraryLayout.LIST)
        val podcast = Podcast(
            id = "p", title = "P", description = "", imageUrl = null, author = null, language = null,
            categories = emptyList(), feedUrl = "p", siteUrl = null, lastUpdated = 0, isSubscribed = true
        )
        repository.podcasts.value = listOf(podcast)
        repository.episodes.value = listOf(
            Episode(
                id = "e1", podcastId = "p", title = "E1", description = null, audioUrl = "a", imageUrl = null,
                duration = 0, publishDate = 5, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null
            )
        )

        val viewModel = createViewModel()

        assertEquals(LibraryLayout.LIST, viewModel.uiState.value.layout)
        assertEquals(listOf(LibraryEntry(podcast, unplayedCount = 1, latestEpisodeDate = 5)), viewModel.uiState.value.podcasts)
    }

    @Test
    fun `initial state is correct`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.podcasts.isEmpty())
        }
    }

    @Test
    fun `confirmDelete calls delete use case`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val podcast = Podcast(id = "1", title = "P1", description = "", imageUrl = null, author = null, language = null, categories = emptyList(), feedUrl = "", siteUrl = null, lastUpdated = 0, isSubscribed = true)
        
        viewModel.uiState.test {
            awaitItem()
            
            viewModel.onIntent(LibraryIntent.RequestDelete(podcast))
            awaitItem()

            viewModel.onIntent(LibraryIntent.ConfirmDelete)
            
            val state = awaitItem()
            assertEquals(null, state.podcastToDelete)
            
            assertEquals("1", repository.deletePodcastCalledWith)
            assertEquals(listOf("delete_podcast"), analytics.events)
        }
    }

    @Test
    fun `events never carry the feed URL or titles`() = runTest(testDispatcher) {
        val feedUrl = "https://feeds.example.com/private/rss?token=s3cr3t"
        val podcast = Podcast(
            id = feedUrl, title = "Paid show", description = "", imageUrl = null, author = null, language = null,
            categories = emptyList(), feedUrl = feedUrl, siteUrl = null, lastUpdated = 0, isSubscribed = true
        )
        repository.podcasts.value = listOf(podcast)
        val viewModel = createViewModel()

        viewModel.onIntent(LibraryIntent.RequestDelete(podcast))
        viewModel.onIntent(LibraryIntent.ConfirmDelete)

        assertEquals(listOf("delete_podcast"), analytics.events)
        analytics.params.forEach { params ->
            assertEquals(mapOf<String, Any?>("host" to "feeds.example.com"), params)
        }
    }

    @Test
    fun `refreshing all when some feeds fail says how many`() = runTest(testDispatcher) {
        val podcasts = listOf("a", "b", "c").map {
            Podcast(
                id = it, title = it, description = "", imageUrl = null, author = null, language = null,
                categories = emptyList(), feedUrl = it, siteUrl = null, lastUpdated = 0, isSubscribed = true
            )
        }
        repository.podcasts.value = podcasts
        feedSource.result = Result.success(FetchedFeed(podcasts.first(), emptyList()))
        feedSource.resultsByUrl["b"] = Result.failure(AppError.NoConnection)
        val viewModel = createViewModel()

        viewModel.onIntent(LibraryIntent.RefreshAll)

        viewModel.messages.test {
            assertEquals(UiMessage(Res.string.error_refresh_some_podcasts, listOf(1, 3)), awaitItem())
        }
    }

    @Test
    fun `refreshing all when every feed fails says why instead of how many`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf("a", "b").map {
            Podcast(
                id = it, title = it, description = "", imageUrl = null, author = null, language = null,
                categories = emptyList(), feedUrl = it, siteUrl = null, lastUpdated = 0, isSubscribed = true
            )
        }
        feedSource.result = Result.failure(AppError.NoConnection)
        val viewModel = createViewModel()

        viewModel.onIntent(LibraryIntent.RefreshAll)

        viewModel.messages.test {
            assertEquals(UiMessage(Res.string.error_no_connection), awaitItem())
        }
    }

    @Test
    fun `categories come from the library the most common first and filter the podcasts`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(
            categorized("a", "Technology"), categorized("b", "Technology", "News"), categorized("c", "Comedy"),
        )
        val viewModel = createViewModel()
        // Technology has two podcasts; the others tie and keep the order they appear in.
        assertEquals(
            listOf(PodcastCategory.TECHNOLOGY, PodcastCategory.NEWS, PodcastCategory.COMEDY),
            viewModel.uiState.value.categories,
        )

        viewModel.onIntent(LibraryIntent.SelectCategory(PodcastCategory.TECHNOLOGY))
        assertEquals(listOf("a", "b"), viewModel.uiState.value.podcasts.map { it.podcast.id })

        viewModel.onIntent(LibraryIntent.SelectCategory(null))
        assertEquals(3, viewModel.uiState.value.podcasts.size)
    }

    @Test
    fun `a category whose last podcast leaves shows the whole library again`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(categorized("a", "Technology"), categorized("c", "Comedy"))
        val viewModel = createViewModel()
        viewModel.onIntent(LibraryIntent.SelectCategory(PodcastCategory.COMEDY))

        repository.podcasts.value = listOf(categorized("a", "Technology"))

        assertEquals(null, viewModel.uiState.value.category)
        assertEquals(listOf("a"), viewModel.uiState.value.podcasts.map { it.podcast.id })
    }

    private fun categorized(id: String, vararg categories: String) = Podcast(
        id = id, title = id, description = "", imageUrl = null, author = null, language = null,
        categories = categories.toList(), feedUrl = id, siteUrl = null, lastUpdated = 0, isSubscribed = true
    )
}
