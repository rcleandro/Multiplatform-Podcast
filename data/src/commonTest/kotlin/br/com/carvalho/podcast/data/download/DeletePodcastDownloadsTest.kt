package br.com.carvalho.podcast.data.download

import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.data.local.dao.FakeEpisodeDao
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.usecase.DeletePodcastUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeletePodcastDownloadsTest {
    private val fileSystem = FakeFileSystem()
    private val directories = AppDirectories(fileSystem, "/app".toPath())
    private val repository = FakePodcastRepository()
    private val downloader = KtorEpisodeDownloader(
        HttpClient(MockEngine { respondOk() }), FakeEpisodeDao(), directories, StandardTestDispatcher()
    )

    @Test
    fun deletingAPodcastDeletesItsDownloadedFilesAndNoOtherPodcastFiles() = runTest {
        repository.episodes.value = listOf(
            episode("mine", podcastId = "p1", downloaded = true),
            episode("other", podcastId = "p2", downloaded = true),
        )
        fileSystem.createDirectories(directories.downloadsDir)
        listOf("mine", "other").forEach { id -> fileSystem.write(directories.downloadPath(id)) { writeUtf8(id) } }

        DeletePodcastUseCase(repository, downloader)("p1")

        assertFalse(fileSystem.exists(directories.downloadPath("mine")))
        assertTrue(fileSystem.exists(directories.downloadPath("other")))
        assertEquals("p1", repository.deletePodcastCalledWith)
    }

    private fun episode(id: String, podcastId: String, downloaded: Boolean) = Episode(
        id = id, podcastId = podcastId, title = id, description = null, audioUrl = "", imageUrl = null, duration = 0,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = downloaded, fileSize = null
    )
}
