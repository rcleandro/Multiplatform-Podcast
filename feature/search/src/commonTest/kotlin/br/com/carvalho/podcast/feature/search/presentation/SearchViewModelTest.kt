package br.com.carvalho.podcast.feature.search.presentation

import br.com.carvalho.podcast.presentation.component.OlderMark
import br.com.carvalho.podcast.core.observability.FakeAnalytics
import app.cash.turbine.test
import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.error_storage_full
import br.com.carvalho.podcast.core.ui.generated.resources.download_deleted
import br.com.carvalho.podcast.domain.model.EpisodeListFilter
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.presentation.UiMessage
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.core.util.FakeNetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val repository = FakePodcastRepository()
    private val episodeDownloader = FakeEpisodeDownloader()
    private val audioPlayer = FakeAudioPlayer()
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

    private val networkMonitor = FakeNetworkMonitor()

    private fun createViewModel() = SearchViewModel(
        repository, episodeDownloader, audioPlayer, PlayEpisodeUseCase(audioPlayer, repository),
        networkMonitor, dispatchers, FakeAnalytics()
    )

    @Test
    fun `without network the episodes start on the downloaded filter`() = runTest(testDispatcher) {
        networkMonitor.online = false

        val viewModel = createViewModel()

        assertEquals(EpisodeListFilter.DOWNLOADED, viewModel.uiState.value.filter)
    }

    @Test
    fun `with network the episodes start on all of them`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        assertEquals(EpisodeListFilter.ALL, viewModel.uiState.value.filter)
    }

    @Test
    fun `episodes are marked played and unplayed from the tab`() = runTest(testDispatcher) {
        val episode = episode("e1", publishDate = 1)
        repository.episodes.value = listOf(episode)
        val viewModel = createViewModel()

        viewModel.onIntent(SearchIntent.SetPlayed(episode, played = true))
        assertEquals(true, repository.episodes.value.single().isPlayed)
        viewModel.onIntent(SearchIntent.SetPlayed(episode, played = false))
        assertEquals(false, repository.episodes.value.single().isPlayed)
    }

    @Test
    fun `confirming marks the episode and the older ones of its podcast unplayed`() = runTest(testDispatcher) {
        val chosen = episode("chosen", publishDate = 2)
        repository.episodes.value = listOf(
            episode("new", publishDate = 3), chosen, episode("old", publishDate = 1),
            episode("other", publishDate = 1, podcastId = "p2"),
        )
        val viewModel = createViewModel()
        val mark = OlderMark(chosen, played = false)

        viewModel.onIntent(SearchIntent.RequestMarkOlder(mark))
        assertEquals(mark, viewModel.uiState.value.olderMark)
        viewModel.onIntent(SearchIntent.ConfirmMarkOlder(mark))

        assertEquals(null, viewModel.uiState.value.olderMark)
        assertEquals(
            mapOf("new" to true, "chosen" to false, "old" to false, "other" to true),
            repository.episodes.value.associate { it.id to it.isPlayed },
        )
    }

    @Test
    fun `dismissing keeps everything and confirming played marks the older ones of its podcast`() =
        runTest(testDispatcher) {
            val chosen = episode("chosen", publishDate = 2).copy(isPlayed = false)
            repository.episodes.value = listOf(
                episode("new", publishDate = 3).copy(isPlayed = false), chosen,
                episode("old", publishDate = 1).copy(isPlayed = false),
            )
            val viewModel = createViewModel()
            val mark = OlderMark(chosen, played = true)

            viewModel.onIntent(SearchIntent.RequestMarkOlder(mark))
            viewModel.onIntent(SearchIntent.DismissMarkOlder)
            assertEquals(null, viewModel.uiState.value.olderMark)
            viewModel.onIntent(SearchIntent.RequestDeleteDownload(chosen))
            viewModel.onIntent(SearchIntent.DismissDeleteDownload)
            assertEquals(null, viewModel.uiState.value.deleteEpisodeConfirmation)

            viewModel.onIntent(SearchIntent.ConfirmMarkOlder(mark))

            assertEquals(
                mapOf("new" to false, "chosen" to true, "old" to true),
                repository.episodes.value.associate { it.id to it.isPlayed },
            )
        }

    private fun episode(id: String, publishDate: Long, podcastId: String = "p1") = Episode(
        id = id, podcastId = podcastId, title = id, description = null, audioUrl = "a", imageUrl = null,
        duration = 0, publishDate = publishDate, isPlayed = true, playbackPosition = 0, isDownloaded = false,
        fileSize = null,
    )

    @Test
    fun `initial state is correct`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.searchQuery)
        }
    }

    @Test
    fun `the downloaded filter shows the space the downloads take`() = runTest(testDispatcher) {
        episodeDownloader.usedBytes = USED_BYTES
        val viewModel = createViewModel()

        viewModel.onIntent(SearchIntent.ChangeFilter(EpisodeListFilter.DOWNLOADED))

        assertEquals(EpisodeListFilter.DOWNLOADED, viewModel.uiState.value.filter)
        assertEquals(USED_BYTES, viewModel.uiState.value.usedBytes)
    }

    @Test
    fun `removing a download says so`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val episode = Episode(
            id = "e1", podcastId = "p", title = "E", description = null, audioUrl = "a", imageUrl = null,
            duration = 0, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = true, fileSize = null
        )

        viewModel.messages.test {
            viewModel.onIntent(SearchIntent.ConfirmDeleteDownload(episode))
            assertEquals(UiMessage(Res.string.download_deleted), awaitItem())
        }
    }

    @Test
    fun `onQueryChange updates state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(SearchIntent.ChangeQuery("Title"))
            
            val state = awaitItem()
            assertEquals("Title", state.searchQuery)
        }
    }

    @Test
    fun `a download that fails tells why, once, and not for failures from before`() = runTest(testDispatcher) {
        val old = DownloadStatus.Failed(AppError.NoConnection)
        episodeDownloader.activeDownloads.value = mapOf("old" to old)
        val viewModel = createViewModel()

        viewModel.messages.test {
            episodeDownloader.activeDownloads.value = mapOf("old" to old, "e1" to DownloadStatus.Downloading(0.5f, 1, 2))
            episodeDownloader.activeDownloads.value =
                mapOf("old" to old, "e1" to DownloadStatus.Failed(AppError.StorageFull))
            assertEquals(UiMessage(Res.string.error_storage_full), awaitItem())
            episodeDownloader.activeDownloads.value =
                mapOf("old" to old, "e1" to DownloadStatus.Failed(AppError.StorageFull), "e2" to DownloadStatus.Idle)
            expectNoEvents()
        }
    }

    @Test
    fun `playEpisode calls audioPlayer`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val episode = createEpisode("1", "Title 1")

        viewModel.onIntent(SearchIntent.Play(episode))

        assertEquals("1", audioPlayer.playCalledWith?.id)
    }

    @Test
    fun `downloadEpisode calls downloader`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val episode = createEpisode("1", "Title 1")

        viewModel.onIntent(SearchIntent.Download(episode))

        assertEquals("1", episodeDownloader.downloadCalledWith?.id)
    }

    @Test
    fun `deleteDownload calls downloader`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        
        viewModel.onIntent(SearchIntent.ConfirmDeleteDownload(createEpisode("1", "Title 1")))

        assertEquals("1", episodeDownloader.deleteCalledWith)
    }

    private fun createEpisode(id: String, title: String) = Episode(
        id = id,
        podcastId = "p1",
        title = title,
        description = null,
        audioUrl = "url",
        imageUrl = null,
        duration = 0L,
        publishDate = 0L,
        isPlayed = false,
        playbackPosition = 0L,
        isDownloaded = false,
        fileSize = null
    )

    @Test
    fun `cancelDownload cancels the episode download`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(SearchIntent.CancelDownload(createEpisode("episode-1", "Title")))

        assertEquals("episode-1", episodeDownloader.cancelCalledWith)
    }

    private companion object {
        const val USED_BYTES = 52_428_800L
    }
}
