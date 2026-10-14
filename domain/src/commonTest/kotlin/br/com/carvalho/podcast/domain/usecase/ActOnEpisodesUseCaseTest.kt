package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ActOnEpisodesUseCaseTest {
    private val player = FakeAudioPlayer()
    private val repository = FakePodcastRepository()
    private val downloader = FakeEpisodeDownloader()
    private val act = ActOnEpisodesUseCase(repository, downloader, EnqueueEpisodeUseCase(player))

    private val first = episode("first")
    private val second = episode("second")
    private val third = episode("third")

    @Test
    fun queuedWithNothingPlayingTheFirstPlaysAndTheRestFollowInOrder() = runTest {
        act(listOf(first, second, third), EpisodeAction.ADD_TO_QUEUE)

        assertEquals("first", player.playCalledWith?.id)
        assertEquals(listOf("second", "third"), player.playerState.value.queue.map { it.id })
    }

    @Test
    fun downloadsOnlyWhatIsNotDownloadedYet() = runTest {
        act(listOf(first, second.copy(isDownloaded = true)), EpisodeAction.DOWNLOAD)

        assertEquals(listOf("first"), downloader.downloaded.map { it.id })
    }

    @Test
    fun marksEachOnePlayedAndUnplayed() = runTest {
        repository.episodes.value = listOf(first, second, third)

        act(listOf(first, third), EpisodeAction.MARK_PLAYED)
        assertEquals(listOf("first", "third"), repository.episodes.value.filter { it.isPlayed }.map { it.id })

        act(listOf(first), EpisodeAction.MARK_UNPLAYED)
        assertEquals(listOf("third"), repository.episodes.value.filter { it.isPlayed }.map { it.id })
    }

    private fun episode(id: String) = Episode(
        id = id, podcastId = "p", title = id, description = null, audioUrl = "a", imageUrl = null, duration = 60,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
    )
}
