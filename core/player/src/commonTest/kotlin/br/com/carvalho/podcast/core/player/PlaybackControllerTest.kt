package br.com.carvalho.podcast.core.player

import br.com.carvalho.podcast.core.observability.CrashReporter
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Chapter
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastPlayback
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.player.SleepTimer
import br.com.carvalho.podcast.domain.repository.FakePlayerRepository
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.PlaybackState
import br.com.carvalho.podcast.domain.usecase.ExplicitRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
    private val chapters = mutableMapOf<String, List<Chapter>>()
    private val explicitRule = MutableStateFlow(ExplicitRule())
    private val continuous = MutableStateFlow(true)

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
        chapterSource = { chapters[it.id].orEmpty() },
        scope = backgroundScope,
        timeSource = testScheduler.timeSource,
        explicitRule = explicitRule,
        continuousPlayback = continuous,
    ).also { runCurrent() }

    @Test
    fun withoutContinuousPlaybackTheQueueWaitsAtTheEndOfAnEpisode() = runTest(dispatcher) {
        continuous.value = false
        val controller = controller()
        controller.setQueue(listOf(second))
        controller.play(first)
        runCurrent()

        engine.raise(PlatformEvent.Ended)
        runCurrent()

        assertEquals("first", engine.loaded?.id)
        assertFalse(controller.playerState.value.isPlaying)
        assertEquals(listOf("second"), controller.playerState.value.queue.map { it.id })
    }

    @Test
    fun underTheEcaDigitalAnExplicitEpisodeDoesNotPlayAndNextSkipsIt() = runTest(dispatcher) {
        val explicit = first.copy(explicit = true)
        val ofExplicitPodcast = episode("third").copy(podcastId = "adult")
        explicitRule.value = ExplicitRule(hidesExplicit = true, explicitPodcasts = setOf("adult"))
        val controller = controller()

        controller.play(explicit)
        controller.play(ofExplicitPodcast)
        runCurrent()
        assertNull(engine.loaded)

        controller.setQueue(listOf(explicit, ofExplicitPodcast, second))
        controller.playNext()
        runCurrent()
        assertEquals("second", engine.loaded?.id)
    }

    @Test
    fun aMinorsSignalArrivingWhileSomethingExplicitPlaysStopsIt() = runTest(dispatcher) {
        val controller = controller()
        controller.play(first.copy(explicit = true))
        engine.raise(PlatformEvent.PlayingChanged(true))
        runCurrent()
        assertTrue(controller.playerState.value.isPlaying)

        explicitRule.value = ExplicitRule(hidesExplicit = true)
        runCurrent()
        assertFalse(controller.playerState.value.isPlaying)

        controller.resume()
        runCurrent()
        assertFalse(controller.playerState.value.isPlaying)
    }

    @Test
    fun theChapterPlayingFollowsThePositionAndThePlatformHearsOnlyWhenItChanges() = runTest(dispatcher) {
        chapters["first"] = listOf(Chapter(0, "Intro"), Chapter(60_000, ""), Chapter(120_000, "News"))
        val controller = controller()

        controller.play(first)
        runCurrent()
        assertEquals(chapters["first"], controller.playerState.value.chapters)
        assertEquals("Intro", controller.playerState.value.currentChapter?.title)

        engine.positionMs = 10_000
        advanceTimeBy(TICK_MS + 1)
        engine.positionMs = 130_000
        advanceTimeBy(TICK_MS)
        assertEquals("News", controller.playerState.value.currentChapter?.title)

        // An untitled chapter shows the episode alone; the next episode starts with none until its own load.
        controller.seekTo(70_000)
        controller.play(second)
        runCurrent()
        assertEquals(emptyList(), controller.playerState.value.chapters)
        assertEquals(listOf("Intro", "News", null), engine.shownChapters)
    }

    @Test
    fun timeHeardIsRealTimeAndTheSpeedSavesTheRestButASeekCountsForNothing() = runTest(dispatcher) {
        val controller = controller()
        controller.play(first)
        runCurrent()

        // Ten ticks at 1.5x: 5 s of real time moved the episode 7.5 s.
        repeat(10) {
            engine.positionMs += TICK_MS * 3 / 2
            advanceTimeBy(TICK_MS)
            runCurrent()
        }
        engine.positionMs += 10 * MINUTE_MS
        advanceTimeBy(TICK_MS)
        runCurrent()
        controller.pause()
        runCurrent()

        val heard = playerRepository.listened
        assertEquals(setOf(first.podcastId), heard.map { it.first }.toSet())
        assertEquals(10 * TICK_MS to 5 * TICK_MS, heard.sumOf { it.second } to heard.sumOf { it.third })
    }

    @Test
    fun anEpisodeNotBegunSkipsItsPodcastsJingleAndAStartedOneResumes() = runTest(dispatcher) {
        podcastRepository.podcasts.value = listOf(podcast(PodcastPlayback(skipIntroSeconds = 30)))
        podcastRepository.episodes.value = listOf(first, second.copy(playbackPosition = START_MS))
        val controller = controller()

        controller.play(first)
        runCurrent()
        assertEquals(30_000L, engine.loadedAt)

        controller.play(second)
        runCurrent()
        assertEquals(START_MS, engine.loadedAt)
    }

    @Test
    fun theClosingCreditsEndTheEpisodeAndTheQueueMovesOn() = runTest(dispatcher) {
        podcastRepository.podcasts.value = listOf(podcast(PodcastPlayback(skipOutroSeconds = 20)))
        podcastRepository.episodes.value = listOf(first, second)
        val controller = controller()
        controller.setQueue(listOf(second))
        controller.play(first)
        engine.durationMs = 600_000L
        engine.positionMs = 579_000L
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals("first", controller.playerState.value.currentEpisode?.id)

        engine.positionMs = 581_000L
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(listOf("first" to listOf("second")), finishedWith)
        assertTrue(podcastRepository.episodes.value.first { it.id == "first" }.isPlayed)
        assertEquals("second", engine.loaded?.id)
    }

    @Test
    fun aPodcastsOwnSpeedPlaysItsEpisodesAndLeavesTheGeneralOneAlone() = runTest(dispatcher) {
        podcastRepository.podcasts.value = listOf(podcast(PodcastPlayback(speed = 1.5f)))
        val other = episode("other").copy(podcastId = "elsewhere")
        val controller = controller()
        controller.setSpeed(1.25f)
        runCurrent()

        controller.play(first)
        runCurrent()
        assertEquals(1.5f, engine.currentSpeed)
        // Changed during this podcast's episode: that episode only.
        controller.setSpeed(2f)
        runCurrent()
        controller.play(other)
        runCurrent()

        assertEquals(1.25f, engine.currentSpeed)
        assertEquals(1.25f, controller.playerState.value.speed)
    }

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
    fun theVolumeGoesDownOverTheLastMinuteAndBackUpWhenExtendedOrDone() = runTest(dispatcher) {
        val controller = controller()
        controller.play(first)
        controller.setSleepTimer(SleepTimer.Minutes(2))
        runCurrent()

        advanceTimeBy(MINUTE_MS / 2)
        assertEquals(1f, engine.volume)
        advanceTimeBy(MINUTE_MS)
        // Half a minute left: half the volume.
        assertEquals(0.5f, engine.volume, absoluteTolerance = 0.02f)

        controller.extendSleepTimer(5)
        runCurrent()
        assertEquals(1f, engine.volume)
        assertEquals(5 * MINUTE_MS + MINUTE_MS / 2, controller.playerState.value.sleepTimerMillis)

        advanceTimeBy(6 * MINUTE_MS)
        assertFalse(engine.isPlaying)
        assertEquals(1f, engine.volume)
    }

    @Test
    fun cancellingTheTimerInItsLastMinuteBringsTheVolumeBack() = runTest(dispatcher) {
        val controller = controller()
        controller.play(first)
        controller.setSleepTimer(SleepTimer.Minutes(1))
        runCurrent()
        advanceTimeBy(MINUTE_MS / 2)

        controller.setSleepTimer(null)
        runCurrent()

        assertEquals(1f, engine.volume)
        assertTrue(engine.isPlaying)
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
    fun aStartedEpisodeLeavesNewAndEntersHistory() = runTest(dispatcher) {
        podcastRepository.episodes.value = listOf(first.copy(isNew = true))
        val controller = controller()

        controller.play(first)
        runCurrent()

        assertFalse(podcastRepository.episodes.value.single().isNew)
        assertNotNull(podcastRepository.episodes.value.single().lastPlayedAt)
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

    private fun podcast(playback: PodcastPlayback) = Podcast(
        id = "p1", title = "P1", description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = "p1", siteUrl = null, lastUpdated = 0, isSubscribed = true,
        playback = playback,
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
