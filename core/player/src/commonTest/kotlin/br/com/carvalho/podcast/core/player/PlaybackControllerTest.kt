package br.com.carvalho.podcast.core.player

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

    private fun TestScope.controller() = PlaybackController(
        engine, playerRepository, podcastRepository, downloader,
        CoroutineDispatchers(main = dispatcher, io = dispatcher, default = dispatcher),
        scope = backgroundScope,
    ).also { runCurrent() }

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
        playerRepository.savedPlaybackState = PlaybackState("second", 42_000, 1.25f, listOf(first, second))

        val controller = controller()

        assertEquals("/downloads/second.mp3", engine.loaded?.localPath)
        assertEquals(42_000L, engine.loadedAt)
        assertEquals(false, engine.playWhenReady)
        assertEquals(1.25f, engine.currentSpeed)
        assertEquals(listOf("first", "second"), controller.playerState.value.queue.map { it.id })
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
        const val TICK_MS = 500L
        const val MINUTE_MS = 60_000L
        const val TICKS_PER_MINUTE = (MINUTE_MS / TICK_MS).toInt()
        const val MIN_SAVES_PER_MINUTE = 10
        const val SAVE_LAG_MS = 5_000L
    }
}
