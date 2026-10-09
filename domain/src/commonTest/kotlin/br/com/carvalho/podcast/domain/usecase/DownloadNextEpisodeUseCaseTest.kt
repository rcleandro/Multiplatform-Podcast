package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadNextEpisodeUseCaseTest {
    private val repository = FakePodcastRepository()
    private val downloader = FakeEpisodeDownloader()
    private val preferences = FakePreferencesRepository().apply { setDownloadNextOnFinish(true) }
    private val downloadNext = DownloadNextEpisodeUseCase(repository, downloader, preferences)

    // The queue a podcast's list builds: the chosen episode and the newer ones, oldest first.
    private val finished = episode("ep1", date = 1, played = true)
    private val queue = listOf(finished, episode("ep2", date = 2), episode("ep3", date = 3))

    @Test
    fun nothingIsDownloadedWithTheSettingOff() = runTest {
        preferences.setDownloadNextOnFinish(false)
        repository.episodes.value = queue + episode("new", date = 9)

        downloadNext(finished, queue)

        assertNull(downloader.downloadCalledWith)
    }

    @Test
    fun thePodcastsNewestEpisodeComesFirst() = runTest {
        repository.episodes.value = queue + episode("new", date = 9)

        downloadNext(finished, queue)

        assertEquals("new", downloader.downloadCalledWith?.id)
    }

    @Test
    fun aNewestEpisodeAlreadyHeardOrDownloadedGivesWayToTheNextInTheQueue() = runTest {
        repository.episodes.value = queue + episode("new", date = 9, played = true)

        downloadNext(finished, queue)
        assertEquals("ep2", downloader.downloadCalledWith?.id)

        repository.episodes.value = listOf(finished, episode("ep2", date = 2, downloaded = true), queue[2]) +
            episode("new", date = 9, downloaded = true)
        downloadNext(finished, queue)
        assertEquals("ep3", downloader.downloadCalledWith?.id)
    }

    @Test
    fun anEpisodeAlreadyDownloadingIsNotAskedForAgain() = runTest {
        repository.episodes.value = queue
        downloader.activeDownloads.value = mapOf("ep3" to DownloadStatus.Downloading(0.5f, 1, 2))
        // The newest is ep3, which is on its way; ep2 is next in the queue.
        downloadNext(finished, queue)

        assertEquals("ep2", downloader.downloadCalledWith?.id)
    }

    @Test
    fun withNothingLeftToHearNothingIsDownloaded() = runTest {
        val heard = queue.map { it.copy(isPlayed = true) }
        repository.episodes.value = heard

        downloadNext(finished, heard)

        assertNull(downloader.downloadCalledWith)
    }

    private fun episode(id: String, date: Long, played: Boolean = false, downloaded: Boolean = false) = Episode(
        id = id, podcastId = "p", title = id, description = null, audioUrl = "https://cdn.example.com/$id.mp3",
        imageUrl = null, duration = 60, publishDate = date, isPlayed = played, playbackPosition = 0,
        isDownloaded = downloaded, fileSize = null,
    )
}
