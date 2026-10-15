package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeleteFinishedDownloadUseCaseTest {
    private val repository = FakePodcastRepository()
    private val downloader = FakeEpisodeDownloader()
    private val preferences = FakePreferencesRepository()
    private val deleteFinished = DeleteFinishedDownloadUseCase(repository, downloader, preferences)

    @Test
    fun aHeardDownloadIsDeletedWithTheSettingOn() = runTest {
        preferences.setDeleteFinishedDownloads(true)
        repository.episodes.value = listOf(episode(downloaded = true))

        // The player still holds it as not downloaded: the database is what counts.
        deleteFinished(episode(downloaded = false))

        assertEquals("e1", downloader.deleteCalledWith)
    }

    @Test
    fun nothingIsDeletedWithTheSettingOffOrWithoutAFile() = runTest {
        repository.episodes.value = listOf(episode(downloaded = true))
        deleteFinished(episode(downloaded = true))
        assertNull(downloader.deleteCalledWith)

        preferences.setDeleteFinishedDownloads(true)
        repository.episodes.value = listOf(episode(downloaded = false))
        deleteFinished(episode(downloaded = false))
        assertNull(downloader.deleteCalledWith)
    }

    private fun episode(downloaded: Boolean) = Episode(
        id = "e1", podcastId = "p", title = "E1", description = null, audioUrl = "a", imageUrl = null, duration = 60,
        publishDate = 0, isPlayed = true, playbackPosition = 0, isDownloaded = downloaded, fileSize = null,
    )
}
