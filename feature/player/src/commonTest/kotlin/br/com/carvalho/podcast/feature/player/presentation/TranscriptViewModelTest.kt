package br.com.carvalho.podcast.feature.player.presentation

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.observability.FakeAnalytics
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.TranscriptLine
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TranscriptViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val player = FakeAudioPlayer()
    private val analytics = FakeAnalytics()
    private val lines = listOf(
        TranscriptLine(0, 5_000, "Welcome to the show", "Alice"),
        TranscriptLine(5_000, 9_000, "Today we talk about Kotlin"),
        TranscriptLine(9_000, 12_000, "kotlin multiplatform, mostly"),
    )
    private val transcripts = mutableMapOf<String, Result<List<TranscriptLine>>>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = TranscriptViewModel(
        player,
        { transcripts.getValue(it.id) },
        CoroutineDispatchers(main = dispatcher, io = dispatcher),
        analytics,
    )

    @Test
    fun theLineBeingSaidFollowsThePlayerAndATapPlaysFromALine() = runTest(dispatcher) {
        transcripts["e1"] = Result.success(lines)
        player.play(episode("e1"))
        player.pause()
        val viewModel = viewModel()

        player.advanceTo(6_000)
        assertEquals(1, viewModel.uiState.value.current)

        viewModel.onIntent(TranscriptIntent.PlayFrom(lines[2]))
        assertEquals(9_000L, player.seekToCalledWith)
        assertTrue(player.resumeCalled)
        assertEquals(listOf("play_from_transcript"), analytics.events)
    }

    @Test
    fun aSearchKeepsOnlyTheLinesWithItsWordsAndANewEpisodeClearsIt() = runTest(dispatcher) {
        transcripts["e1"] = Result.success(lines)
        transcripts["e2"] = Result.failure(AppError.NoConnection)
        player.play(episode("e1"))
        val viewModel = viewModel()

        viewModel.onIntent(TranscriptIntent.ChangeQuery(" KOTLIN "))
        assertEquals(listOf(1, 2), viewModel.uiState.value.shown)

        player.play(episode("e2"))
        assertEquals("", viewModel.uiState.value.query)
        assertTrue(viewModel.uiState.value.failed)

        transcripts["e2"] = Result.success(lines.take(1))
        viewModel.onIntent(TranscriptIntent.Retry)
        assertEquals(listOf(0), viewModel.uiState.value.shown)
    }

    private fun episode(id: String) = Episode(
        id = id, podcastId = "p1", title = id, description = null, audioUrl = "", imageUrl = null, duration = 0,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
        transcriptUrl = "https://example.com/$id.vtt", transcriptType = "text/vtt",
    )
}
