package br.com.carvalho.podcast.data.download

import app.cash.turbine.test
import br.com.carvalho.podcast.data.local.dao.FakeEpisodeDao
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class KtorEpisodeDownloaderTest {

    private val episodeDao = FakeEpisodeDao()
    private val testDispatcher = StandardTestDispatcher()
    private val fileSystem = FakeFileSystem()
    private val baseDir = "/test".toPath()

    private fun createDownloader(engine: MockEngine): KtorEpisodeDownloader {
        val client = HttpClient(engine)
        return KtorEpisodeDownloader(client, episodeDao, fileSystem, baseDir, testDispatcher)
    }

    private val sampleEpisode = Episode(
        id = "e1",
        podcastId = "p1",
        title = "Title",
        description = null,
        audioUrl = "https://test.com/audio.mp3",
        imageUrl = null,
        duration = 100,
        publishDate = 0,
        isPlayed = false,
        playbackPosition = 0,
        isDownloaded = false,
        fileSize = null
    )

    @Test
    fun `download flow updates status correctly to completed`() = runTest(testDispatcher) {
        val mockEngine = MockEngine {
            respond(
                content = "dummy audio content",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "audio/mpeg")
            )
        }

        val downloader = createDownloader(mockEngine)

        downloader.activeDownloads.test {
            assertEquals(emptyMap(), awaitItem()) // Initial

            downloader.download(sampleEpisode)

            // Advance to Queued
            testDispatcher.scheduler.runCurrent()
            val queuedStatus = awaitItem()[sampleEpisode.id]
            assertTrue(queuedStatus is DownloadStatus.Queued, "Should be Queued but was $queuedStatus")

            // Advance to completion
            testDispatcher.scheduler.advanceUntilIdle()

            val finalMap = awaitItem()
            val finalStatus = finalMap[sampleEpisode.id]
            assertTrue(finalStatus is DownloadStatus.Completed, "Should be Completed but was $finalStatus")

            assertTrue(fileSystem.exists(baseDir / "downloads" / "e1.mp3"))
        }
    }

    @Test
    fun `download flow handles http error`() = runTest(testDispatcher) {
        val mockEngine = MockEngine {
            respond(
                content = "Not Found",
                status = HttpStatusCode.NotFound
            )
        }

        val downloader = createDownloader(mockEngine)

        downloader.activeDownloads.test {
            awaitItem() // initial
            downloader.download(sampleEpisode)
            testDispatcher.scheduler.runCurrent()
            awaitItem() // queued

            testDispatcher.scheduler.advanceUntilIdle()

            val finalStatus = awaitItem()[sampleEpisode.id]
            assertTrue(finalStatus is DownloadStatus.Failed)
        }
    }

    @Test
    fun `delete removes file`() = runTest(testDispatcher) {
        val downloader = createDownloader(MockEngine { respondOk() })
        val path = baseDir / "downloads" / "e1.mp3"
        fileSystem.createDirectories(baseDir / "downloads")
        fileSystem.write(path) { writeUtf8("content") }

        downloader.delete("e1")
        testDispatcher.scheduler.advanceUntilIdle()

        kotlin.test.assertNull(downloader.getLocalPath("e1"))
        assertTrue(!fileSystem.exists(path))
    }
}
