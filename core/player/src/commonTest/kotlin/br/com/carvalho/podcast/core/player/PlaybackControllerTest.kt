package br.com.carvalho.podcast.core.player

import kotlinx.coroutines.flow.MutableStateFlow
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.core.observability.CrashReporter
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.SleepTimer
import br.com.carvalho.podcast.domain.repository.FakePlayerRepository
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.PlaybackState
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaybackControllerTest {
    private val dispatcher = StandardTestDispatcher()
    private val engine = FakePlatformPlayer()
    private val playerRepository = FakePlayerRepository()
    private val podcastRepository = FakePodcastRepository()
    private val downloader = FakeEpisodeDownloader()

    private val first = episode("first")
    private val second = episode("second")

    private val finishedWith = mutableListOf<Pair<String, List<String>>>()
    private val skips = MutableStateFlow(SkipIntervals())

    @Test
    fun theJumpsChosenInSettingsReachTheStateThePlatformAndTheRemoteButtons() = runTest(dispatcher) {
        val controller = controller()
        controller.play(first)
        engine.positionMs = START_MS
        runCurrent()

        skips.value = SkipIntervals(backwardSeconds = 5, forwardSeconds = 15)
        runCurrent()
        engine.raise(PlatformEvent.Remote(RemoteCommand.SKIP_FORWARD))
        runCurrent()

        assertEquals(SkipIntervals(5, 15), controller.playerState.value.skipIntervals)
        assertEquals(SkipIntervals(5, 15), engine.shownSkipIntervals)
        assertEquals(START_MS + 15_000, engine.positionMs)
    }

    private fun TestScope.controller() = PlaybackController(
        engine, playerRepository, podcastRepository, downloader,
        CoroutineDispatchers(main = dispatcher, io = dispatcher, default = dispatcher),
        onFinished = { finished, queue -> finishedWith += finished.id to queue.map { it.id } },
        skipIntervals = skips,
        scope = backgroundScope,
    ).also { runCurrent() }

    @Test
    fun anEpisodeThatEndsIsHandedOnWithItsQueue() = runTest(dispatcher) {
        val controller = controller()
        controller.setQueue(listOf(first, second))
        controller.play(first)
        runCurrent()

        engine.raise(PlatformEvent.Ended)
        runCurrent()

        assertEquals(listOf("first" to listOf("second")), finishedWith)
    }

    @Test
    fun theTimeToAudioIsMeasuredOncePerEpisodeUpToTheFirstSound() = runTest(dispatcher) {
        val lines = mutableListOf<String>()
        AppLogger.crashReporter = object : CrashReporter {
            override fun log(message: String) { lines += message }
            override fun recordException(throwable: Throwable) = Unit
        }
        try {
            val controller = controller()
            controller.play(first)
            runCurrent()
            controller.pause()
            controller.resume()
            runCurrent()

            val measured = lines.filter { "metric=time_to_audio" in it }
            assertEquals(1, measured.size, lines.toString())
            assertTrue(measured.single().endsWith("source=stream"), measured.single())
        } finally {
            AppLogger.crashReporter = null
        }
    }

    @Test
    fun theEndOfAnEpisodeMarksItPlayedAndMovesToTheNextOne() = runTest(dispatcher) {
        podcastRepository.episodes.value = listOf(first, second)
        val controller = controller()
        controller.setQueue(listOf(first, second))
        controller.play(first)
        runCurrent()

        engine.raise(PlatformEvent.Ended)
        runCurrent()

        assertTrue(podcastRepository.episodes.value.first { it.id == "first" }.isPlayed)
        assertEquals("second", engine.loaded?.id)
        // The finished episode is gone and the one playing is no longer next (18.6).
        assertEquals(emptyList(), controller.playerState.value.queue)
    }

    @Test
    fun theLastEpisodeEndingStopsPlayback() = runTest(dispatcher) {
        val controller = controller()
        controller.setQueue(listOf(first))
        controller.play(first)
        runCurrent()

        engine.raise(PlatformEvent.Ended)
        runCurrent()

        assertFalse(controller.playerState.value.isPlaying)
        assertEquals("first", engine.loaded?.id)
    }

    @Test
    fun theEndOfEpisodeTimerStopsInsteadOfMovingOn() = runTest(dispatcher) {
        val controller = controller()
        controller.setQueue(listOf(first, second))
        controller.play(first)
        controller.setSleepTimer(SleepTimer.EndOfEpisode)
        runCurrent()

        engine.raise(PlatformEvent.Ended)
        runCurrent()

        assertEquals("first", engine.loaded?.id)
        assertFalse(controller.playerState.value.isPlaying)
        assertNull(controller.playerState.value.sleepTimer)
    }

    @Test
    fun theMinutesTimerPausesWhenItRunsOut() = runTest(dispatcher) {
        val controller = controller()
        controller.play(first)
        controller.setSleepTimer(SleepTimer.Minutes(1))
        runCurrent()

        advanceTimeBy(MINUTE_MS + 1)

        assertFalse(engine.isPlaying)
        assertNull(controller.playerState.value.sleepTimer)
    }

    @Test
    fun theSpeedSurvivesAPause() = runTest(dispatcher) {
        val controller = controller()
        controller.play(first)
        controller.setSpeed(1.5f)
        controller.pause()
        controller.resume()
        runCurrent()

        assertEquals(1.5f, engine.currentSpeed)
        assertEquals(1.5f, controller.playerState.value.speed)
    }

    @Test
    fun progressIsSavedWhilePlaying() = runTest(dispatcher) {
        val controller = controller()
        controller.play(first)
        runCurrent()
        val savesBefore = playerRepository.saveCount

        repeat(TICKS_PER_MINUTE) {
            engine.positionMs += TICK_MS
            advanceTimeBy(TICK_MS)
        }

        assertTrue(playerRepository.saveCount - savesBefore >= MIN_SAVES_PER_MINUTE)
        assertTrue(playerRepository.savedPlaybackState!!.position >= MINUTE_MS - SAVE_LAG_MS)
    }

    @Test
    fun theLastSessionIsRestoredPausedWhereItStopped() = runTest(dispatcher) {
        podcastRepository.episodes.value = listOf(first, second)
        downloader.localPaths["second"] = "/downloads/second.mp3"
        playerRepository.savedPlaybackState = PlaybackState("second", 42_000, 1.25f, listOf(first))

        val controller = controller()

        assertEquals("/downloads/second.mp3", engine.loaded?.localPath)
        assertEquals(42_000L, engine.loadedAt)
        assertEquals(false, engine.playWhenReady)
        assertEquals(1.25f, engine.currentSpeed)
        assertEquals(listOf("first"), controller.playerState.value.queue.map { it.id })
    }

    @Test
    fun aQueueSavedBefore186KeepsOnlyWhatFollowsTheCurrentEpisode() = runTest(dispatcher) {
        val third = episode("third")
        podcastRepository.episodes.value = listOf(first, second, third)
        // The old queue held the episodes already heard and the current one too.
        playerRepository.savedPlaybackState = PlaybackState("second", 42_000, 1f, listOf(first, second, third))

        val controller = controller()

        assertEquals(listOf("third"), controller.playerState.value.queue.map { it.id })
    }

    @Test
    fun playingAnEpisodeTakesItOutOfTheQueueAndKeepsTheRest() = runTest(dispatcher) {
        val controller = controller()
        controller.setQueue(listOf(first, second))

        controller.play(second)
        runCurrent()

        assertEquals(listOf("first"), controller.playerState.value.queue.map { it.id })
    }

    @Test
    fun aQueueArrangedWhilePausedIsSavedAtOnce() = runTest(dispatcher) {
        val controller = controller()
        controller.play(first)
        runCurrent()
        controller.pause()
        runCurrent()

        controller.setQueue(listOf(second))
        runCurrent()

        assertEquals(listOf("second"), playerRepository.savedPlaybackState?.queue?.map { it.id })
    }

    @Test
    fun aDownloadedEpisodePlaysFromItsFile() = runTest(dispatcher) {
        downloader.localPaths["first"] = "/downloads/first.mp3"
        val controller = controller()

        controller.play(first)
        runCurrent()

        assertEquals("/downloads/first.mp3", engine.loaded?.localPath)
    }

    @Test
    fun theNextEpisodeInTheQueuePlaysFromItsDownloadedFile() = runTest(dispatcher) {
        downloader.localPaths["second"] = "/downloads/second.mp3"
        val controller = controller()
        controller.setQueue(listOf(first, second))
        controller.play(first)
        runCurrent()

        engine.raise(PlatformEvent.Remote(RemoteCommand.NEXT))
        runCurrent()

        assertEquals("/downloads/second.mp3", engine.loaded?.localPath)
    }

    @Test
    fun aStartedEpisodePlaysFromWhereItStopped() = runTest(dispatcher) {
        // The queue still holds the episode as it was before it was heard; the database knows where it stopped.
        podcastRepository.episodes.value = listOf(first.copy(playbackPosition = 754_000))
        val controller = controller()

        controller.play(first)
        runCurrent()

        assertEquals(754_000L, engine.loadedAt)
        assertEquals(754_000L, controller.playerState.value.position)
    }

    @Test
    fun aStartedEpisodeLeavesNew() = runTest(dispatcher) {
        podcastRepository.episodes.value = listOf(first.copy(isNew = true))
        val controller = controller()

        controller.play(first)
        runCurrent()

        assertFalse(podcastRepository.episodes.value.single().isNew)
    }

    @Test
    fun aHeardEpisodeStartsOver() = runTest(dispatcher) {
        podcastRepository.episodes.value = listOf(first.copy(isPlayed = true, playbackPosition = 754_000))
        val controller = controller()

        controller.play(first)
        runCurrent()

        assertEquals(0L, engine.loadedAt)
    }

    @Test
    fun previousOnTheFirstEpisodeGoesBackToItsStart() = runTest(dispatcher) {
        val controller = controller()
        controller.setQueue(listOf(first, second))
        controller.play(first)
        runCurrent()
        engine.positionMs = 90_000

        controller.playPrevious()
        runCurrent()

        assertEquals("first", engine.loaded?.id)
        assertEquals(0L, engine.positionMs)
    }

    private fun episode(id: String) = Episode(
        id = id, podcastId = "p1", title = id, description = null, audioUrl = "https://audio/$id", imageUrl = null,
        duration = 0, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null
    )

    private companion object {
        const val START_MS = 60_000L
        const val TICK_MS = 500L
        const val MINUTE_MS = 60_000L
        const val TICKS_PER_MINUTE = (MINUTE_MS / TICK_MS).toInt()
        const val MIN_SAVES_PER_MINUTE = 10
        const val SAVE_LAG_MS = 5_000L
    }
}
