package br.com.carvalho.podcast.watch

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WatchPlayerTest {
    private val episode = Episode(
        id = "e", podcastId = "p", title = "e", description = null, audioUrl = "a", imageUrl = null, duration = 100,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
    )

    @Test
    fun playPauseTogglesThePlayer() = runTest {
        val player = FakeAudioPlayer()
        val watch = WatchPlayer(player)
        watch.play(episode)

        watch.playPause()
        assertTrue(player.pauseCalled)
        watch.playPause()
        assertTrue(player.resumeCalled)
    }

    @Test
    fun theRingFollowsThePositionOverTheFeedsLengthUntilTheAudioKnowsIt() {
        assertEquals(0.25f, PlayerState(currentEpisode = episode, position = 25_000).progress())
        assertEquals(0.5f, PlayerState(currentEpisode = episode, position = 25_000, duration = 50_000).progress())
        assertEquals(0f, PlayerState().progress())
    }
}
