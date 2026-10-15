package br.com.carvalho.podcast.feature.podcast.presentation

import app.cash.turbine.test
import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.observability.FakeAnalytics
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.bulk_played
import br.com.carvalho.podcast.core.ui.generated.resources.error_no_connection
import br.com.carvalho.podcast.core.ui.generated.resources.queued_next
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastPlayback
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import br.com.carvalho.podcast.domain.usecase.ActOnEpisodesUseCase
import br.com.carvalho.podcast.domain.usecase.EnqueueEpisodeUseCase
import br.com.carvalho.podcast.domain.usecase.EpisodeAction
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import br.com.carvalho.podcast.presentation.UiMessage
import br.com.carvalho.podcast.presentation.component.OlderMark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastDetailViewModelTest {
    private val audioPlayer = FakeAudioPlayer()
    private val repository = FakePodcastRepository()
    private val feedSource = FakeFeedSource()
    private val refreshUseCase = RefreshPodcastUseCase(feedSource, repository)
    private val episodeDownloader = FakeEpisodeDownloader()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(main = testDispatcher, io = testDispatcher)

    private val podcastId = "p1"
    private val samplePodcast = Podcast(id = podcastId, title = "P1", description = "", imageUrl = null, author = null, language = null, categories = emptyList(), feedUrl = podcastId, siteUrl = null, lastUpdated = 0, isSubscribed = true)
    private val sampleEpisode = Episode(id = "e1", podcastId = podcastId, title = "E1", description = null, audioUrl = "", imageUrl = null, duration = 100, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PodcastDetailViewModel(
        podcastId, audioPlayer, PlayEpisodeUseCase(audioPlayer), EnqueueEpisodeUseCase(audioPlayer),
        ActOnEpisodesUseCase(repository, episodeDownloader, EnqueueEpisodeUseCase(audioPlayer)), refreshUseCase,
        episodeDownloader, repository, dispatchers, FakeAnalytics()
    )

    @Test
    fun `the header counts the episodes and finds the newest unplayed one`() = runTest(testDispatcher) {
        repository.episodes.value = listOf(
            sampleEpisode.copy(id = "played", publishDate = 300, isPlayed = true),
            sampleEpisode.copy(id = "latest", publishDate = 200),
            sampleEpisode.copy(id = "older", publishDate = 100),
        )
        val viewModel = createViewModel()

        assertEquals(3, viewModel.uiState.value.episodeCount)
        assertEquals("latest", viewModel.uiState.value.latestUnplayed?.id)
        viewModel.onIntent(PodcastDetailIntent.Play(viewModel.uiState.value.latestUnplayed!!))

        assertEquals("latest", audioPlayer.playCalledWith?.id)
    }

    @Test
    fun `initial state loads the podcast`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(samplePodcast.id, state.podcast?.id)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `refresh calls use case`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)
        feedSource.result = Result.success(FetchedFeed(samplePodcast, emptyList()))
        feedSource.delayMs = 10

        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(PodcastDetailIntent.Refresh)

            val refreshingState = awaitItem()
            assertTrue(refreshingState.isRefreshing)
            assertFalse(refreshingState.isLoading)

            val finalState = awaitItem()
            assertFalse(finalState.isRefreshing)
            
            assertEquals(podcastId, feedSource.fetchCalledWith)
        }
    }

    @Test
    fun `confirming marks the episode and the older ones unplayed leaving the newer`() = runTest(testDispatcher) {
        val older = sampleEpisode.copy(id = "old", publishDate = 1, isPlayed = true)
        val chosen = sampleEpisode.copy(id = "chosen", publishDate = 2, isPlayed = true)
        val newer = sampleEpisode.copy(id = "new", publishDate = 3, isPlayed = true)
        repository.episodes.value = listOf(newer, chosen, older)
        val viewModel = createViewModel()
        val mark = OlderMark(chosen, played = false)

        viewModel.onIntent(PodcastDetailIntent.RequestMarkOlder(mark))
        assertEquals(mark, viewModel.uiState.value.olderMark)
        viewModel.onIntent(PodcastDetailIntent.ConfirmMarkOlder(mark))

        assertEquals(null, viewModel.uiState.value.olderMark)
        assertEquals(
            mapOf("new" to true, "chosen" to false, "old" to false),
            repository.episodes.value.associate { it.id to it.isPlayed },
        )
    }

    @Test
    fun `playEpisode prepares and plays via audioPlayer`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)
        repository.episodes.value = listOf(sampleEpisode)
        
        val viewModel = createViewModel()
        
        viewModel.onIntent(PodcastDetailIntent.Play(sampleEpisode))
        
        assertEquals(sampleEpisode.id, audioPlayer.playCalledWith?.id)
    }

    @Test
    fun `marking unplayed reaches the repository`() = runTest(testDispatcher) {
        repository.episodes.value = listOf(sampleEpisode.copy(isPlayed = true, playbackPosition = 50))
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.MarkUnplayed(sampleEpisode))

        assertEquals(false, repository.episodes.value.single().isPlayed)
        assertEquals(0L, repository.episodes.value.single().playbackPosition)
    }

    @Test
    fun `saving an episode and unsaving it reach the repository`() = runTest(testDispatcher) {
        repository.episodes.value = listOf(sampleEpisode)
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.SetSaved(sampleEpisode, saved = true))
        assertTrue(repository.episodes.value.single().isSaved)

        viewModel.onIntent(PodcastDetailIntent.SetSaved(sampleEpisode, saved = false))
        assertFalse(repository.episodes.value.single().isSaved)
    }

    @Test
    fun `downloading an episode hands it to the downloader`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.Download(sampleEpisode))

        assertEquals(sampleEpisode, episodeDownloader.downloadCalledWith)
    }

    @Test
    fun `deleting a download asks first and only deletes on confirm`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.RequestDeleteDownload(sampleEpisode))
        assertEquals(sampleEpisode, viewModel.uiState.value.deleteEpisodeConfirmation)
        viewModel.onIntent(PodcastDetailIntent.DismissDeleteDownload)
        assertEquals(null, viewModel.uiState.value.deleteEpisodeConfirmation)
        assertEquals(null, episodeDownloader.deleteCalledWith)

        viewModel.onIntent(PodcastDetailIntent.RequestDeleteDownload(sampleEpisode))
        viewModel.onIntent(PodcastDetailIntent.ConfirmDeleteDownload(sampleEpisode))

        assertEquals(null, viewModel.uiState.value.deleteEpisodeConfirmation)
        assertEquals(sampleEpisode.id, episodeDownloader.deleteCalledWith)
    }

    @Test
    fun `marking played and dismissing the older mark`() = runTest(testDispatcher) {
        repository.episodes.value = listOf(sampleEpisode)
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.MarkPlayed(sampleEpisode))
        assertTrue(repository.episodes.value.single().isPlayed)

        viewModel.onIntent(PodcastDetailIntent.RequestMarkOlder(OlderMark(sampleEpisode, played = true)))
        viewModel.onIntent(PodcastDetailIntent.DismissMarkOlder)
        assertEquals(null, viewModel.uiState.value.olderMark)
    }

    @Test
    fun `confirming marks the episode and the older ones played leaving the newer`() = runTest(testDispatcher) {
        val older = sampleEpisode.copy(id = "old", publishDate = 1)
        val chosen = sampleEpisode.copy(id = "chosen", publishDate = 2)
        val newer = sampleEpisode.copy(id = "new", publishDate = 3)
        repository.episodes.value = listOf(newer, chosen, older)
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.ConfirmMarkOlder(OlderMark(chosen, played = true)))

        assertEquals(
            mapOf("new" to false, "chosen" to true, "old" to true),
            repository.episodes.value.associate { it.id to it.isPlayed },
        )
    }

    @Test
    fun `cancelDownload cancels the episode download`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.CancelDownload(sampleEpisode.copy(id = "episode-1")))

        assertEquals("episode-1", episodeDownloader.cancelCalledWith)
    }

    @Test
    fun `a serial podcast lists oldest first until the user chooses otherwise`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast.copy(serial = true))
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.oldestFirst)

        viewModel.onIntent(PodcastDetailIntent.SetOldestFirst(false))

        assertEquals(false, repository.podcasts.value.single().oldestFirst)
        assertFalse(viewModel.uiState.value.oldestFirst)
    }

    @Test
    fun `the selected episodes are marked played together and the selection closes`() = runTest(testDispatcher) {
        val one = sampleEpisode.copy(id = "one")
        val two = sampleEpisode.copy(id = "two")
        val other = sampleEpisode.copy(id = "other")
        repository.episodes.value = listOf(one, two, other)
        val viewModel = createViewModel()

        listOf(one, other, two, other).forEach { viewModel.onIntent(PodcastDetailIntent.ToggleSelection(it)) }
        assertEquals(listOf("one", "two"), viewModel.uiState.value.selection.map { it.id })

        viewModel.messages.test {
            viewModel.onIntent(PodcastDetailIntent.ActOnSelection(EpisodeAction.MARK_PLAYED))
            assertEquals(UiMessage(Res.string.bulk_played, listOf(2)), awaitItem())
        }
        assertEquals(listOf("one", "two"), repository.episodes.value.filter { it.isPlayed }.map { it.id })
        assertTrue(viewModel.uiState.value.selection.isEmpty())
    }

    @Test
    fun `the podcast's playback settings reach the repository`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.SetPlayback(PodcastPlayback(speed = 1.5f, skipIntroSeconds = 30)))

        assertEquals(PodcastPlayback(speed = 1.5f, skipIntroSeconds = 30), repository.podcasts.value.single().playback)
        assertEquals(PodcastPlayback(speed = 1.5f, skipIntroSeconds = 30), viewModel.uiState.value.podcast?.playback)
    }

    @Test
    fun `turning the podcast's notifications off reaches the repository`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.SetNotifications(false))

        assertFalse(repository.podcasts.value.single().notifyNewEpisodes)
    }

    @Test
    fun `choosing how many to download saves it and reads the feed again`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.SetAutoDownload(3))

        assertEquals(3, repository.podcasts.value.single().autoDownloadCount)
        assertEquals(podcastId, feedSource.podcastIdAskedFor)
    }

    @Test
    fun `filter survives a podcast update`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.SetFilter(EpisodeFilter.UNPLAYED))
        repository.podcasts.value = listOf(samplePodcast.copy(title = "Renamed"))

        assertEquals(EpisodeFilter.UNPLAYED, viewModel.uiState.value.filter)
    }

    @Test
    fun `a failed refresh tells the user why`() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(samplePodcast)
        feedSource.result = Result.failure(AppError.NoConnection)
        val viewModel = createViewModel()

        viewModel.onIntent(PodcastDetailIntent.Refresh)

        viewModel.messages.test {
            assertEquals(UiMessage(Res.string.error_no_connection), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `play next puts the episode first in the queue and says so`() = runTest(testDispatcher) {
        val playing = sampleEpisode.copy(id = "playing")
        audioPlayer.play(playing)
        val viewModel = createViewModel()

        viewModel.messages.test {
            viewModel.onIntent(PodcastDetailIntent.Enqueue(sampleEpisode, next = true))
            assertEquals(UiMessage(Res.string.queued_next), awaitItem())
        }
        assertEquals(listOf("e1"), audioPlayer.playerState.value.queue.map { it.id })
    }
}
