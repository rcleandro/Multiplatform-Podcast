package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayEpisodeUseCaseTest {
    private val player = FakeAudioPlayer()
    private val downloader = FakeEpisodeDownloader()
    private val repository = FakePodcastRepository()
    private val playEpisode = PlayEpisodeUseCase(player, downloader, repository)

    private val older = episode("old", publishDate = 1)
    private val chosen = episode("chosen", publishDate = 2)
    private val newer = episode("new", publishDate = 3)

    @Test
    fun aDownloadedEpisodePlaysFromItsFile() = runTest {
        downloader.localPaths["chosen"] = "/downloads/chosen.mp3"

        playEpisode(chosen)

        assertEquals("/downloads/chosen.mp3", player.playCalledWith?.localPath)
    }

    @Test
    fun withoutAQueueTheNewerEpisodesOfThePodcastFollow() = runTest {
        repository.episodes.value = listOf(newer, chosen, older)

        playEpisode(chosen)

        assertEquals(listOf("chosen", "new"), player.queueSet?.map { it.id })
    }

    @Test
    fun aGivenQueueIsKept() = runTest {
        playEpisode(chosen, queue = listOf(newer, chosen))

        assertEquals(listOf("new", "chosen"), player.queueSet?.map { it.id })
    }

    @Test
    fun theCurrentEpisodePausesInsteadOfStartingOver() = runTest {
        playEpisode(chosen)
        player.playCalledWith = null

        playEpisode(chosen)

        assertTrue(player.pauseCalled)
        assertNull(player.playCalledWith)
    }

    private fun episode(id: String, publishDate: Long) = Episode(
        id = id, podcastId = "p1", title = id, description = null, audioUrl = "", imageUrl = null, duration = 0,
        publishDate = publishDate, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null
    )
}
