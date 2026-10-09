package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayEpisodeUseCaseTest {
    private val player = FakeAudioPlayer()
    private val playEpisode = PlayEpisodeUseCase(player)

    private val chosen = episode("chosen")
    private val queued = episode("queued")

    @Test
    fun playingAnEpisodeKeepsTheQueueTheUserArranged() = runTest {
        player.setQueue(listOf(queued))
        player.queueSet = null

        playEpisode(chosen)

        assertEquals("chosen", player.playCalledWith?.id)
        // The podcast's newer episodes no longer fill the queue on their own (18.6).
        assertNull(player.queueSet)
        assertEquals(listOf("queued"), player.playerState.value.queue.map { it.id })
    }

    @Test
    fun theCurrentEpisodePausesInsteadOfStartingOver() = runTest {
        playEpisode(chosen)
        player.playCalledWith = null

        playEpisode(chosen)

        assertTrue(player.pauseCalled)
        assertNull(player.playCalledWith)
    }

    private fun episode(id: String) = Episode(
        id = id, podcastId = "p1", title = id, description = null, audioUrl = "", imageUrl = null, duration = 0,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null
    )
}
