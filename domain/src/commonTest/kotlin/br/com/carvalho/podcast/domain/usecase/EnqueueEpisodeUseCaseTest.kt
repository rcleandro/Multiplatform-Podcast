package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnqueueEpisodeUseCaseTest {
    private val player = FakeAudioPlayer()
    private val enqueue = EnqueueEpisodeUseCase(player)

    private val playing = episode("playing")
    private val queued = episode("queued")
    private val chosen = episode("chosen")

    private fun queue() = player.playerState.value.queue.map { it.id }

    @Test
    fun playNextPutsTheEpisodeFirstAndAddToQueueLast() = runTest {
        player.play(playing)
        player.setQueue(listOf(queued))

        assertEquals(Enqueued.NEXT, enqueue(chosen, next = true))
        assertEquals(listOf("chosen", "queued"), queue())

        assertEquals(Enqueued.LAST, enqueue(chosen, next = false))
        // Moved, not added twice.
        assertEquals(listOf("queued", "chosen"), queue())
    }

    @Test
    fun withNothingPlayingTheEpisodePlaysAtOnce() = runTest {
        assertEquals(Enqueued.PLAYING, enqueue(chosen, next = false))

        assertEquals("chosen", player.playCalledWith?.id)
    }

    @Test
    fun theEpisodePlayingIsNotQueued() = runTest {
        player.play(playing)
        player.queueSet = null

        assertEquals(Enqueued.ALREADY_PLAYING, enqueue(playing, next = true))
        assertNull(player.queueSet)
    }

    private fun episode(id: String) = Episode(
        id = id, podcastId = "p1", title = id, description = null, audioUrl = "", imageUrl = null, duration = 0,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null
    )
}
