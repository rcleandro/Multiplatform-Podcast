package br.com.carvalho.podcast.data.download

import br.com.carvalho.podcast.core.util.AppDirectories
import app.cash.turbine.test
import br.com.carvalho.podcast.data.local.dao.FakeEpisodeDao
import br.com.carvalho.podcast.data.mapper.toEntity
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
        return KtorEpisodeDownloader(client, episodeDao, AppDirectories(fileSystem, baseDir), testDispatcher)
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
            assertEquals(emptyMap(), awaitItem())

            downloader.download(sampleEpisode)

            testDispatcher.scheduler.runCurrent()
            val queuedStatus = awaitItem()[sampleEpisode.id]
            assertTrue(queuedStatus is DownloadStatus.Queued, "Should be Queued but was $queuedStatus")

            testDispatcher.scheduler.advanceUntilIdle()

            val finalMap = awaitItem()
            val finalStatus = finalMap[sampleEpisode.id]
            assertTrue(finalStatus is DownloadStatus.Completed, "Should be Completed but was $finalStatus")

            assertTrue(fileSystem.exists(baseDir / "downloads" / "e1.mp3"))
        }
    }

    @Test
    fun `the file is written while the episode is still downloading`() = runTest {
        val body = ByteChannel()
        val engine = MockEngine {
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, "${CHUNK * 2}"))
        }
        val downloader = KtorEpisodeDownloader(
            HttpClient(engine), episodeDao, AppDirectories(fileSystem, baseDir), Dispatchers.Default
        )

        downloader.download(sampleEpisode)
        body.writeFully(ByteArray(CHUNK))
        body.flush()

        // Only half of the body has arrived: a streamed download has already written it, a buffered one has not.
        withContext(Dispatchers.Default) {
            withTimeout(STREAM_TIMEOUT_MS) { while (bytesOnDisk() < CHUNK) delay(POLL_MS) }
        }
        body.close()
    }

    @Test
    fun `a body cut short does not leave a downloaded episode`() = runTest {
        val body = ByteChannel()
        body.writeFully(ByteArray(CHUNK))
        body.close()
        val engine = MockEngine {
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, "${CHUNK * 2}"))
        }
        val downloader = KtorEpisodeDownloader(
            HttpClient(engine), episodeDao, AppDirectories(fileSystem, baseDir), Dispatchers.Default
        )

        downloader.download(sampleEpisode)
        val status = withContext(Dispatchers.Default) {
            withTimeout(STREAM_TIMEOUT_MS) {
                downloader.activeDownloads.first { downloads ->
                    downloads[sampleEpisode.id].let { it is DownloadStatus.Failed || it is DownloadStatus.Completed }
                }
            }
        }[sampleEpisode.id]
        assertTrue(status is DownloadStatus.Failed, "Should be Failed but was $status")
        kotlin.test.assertNull(downloader.getLocalPath(sampleEpisode.id))
        assertEquals(0, bytesOnDisk(), "No partial file should stay behind")
    }

    @Test
    fun `the file is named after the audio type and recorded in the database`() = runTest {
        episodeDao.insertAll(listOf(sampleEpisode.toEntity()))
        val engine = MockEngine {
            respond("aac audio", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "audio/mp4"))
        }
        val downloader = KtorEpisodeDownloader(
            HttpClient(engine), episodeDao, AppDirectories(fileSystem, baseDir), Dispatchers.Default
        )

        downloader.download(sampleEpisode)
        withContext(Dispatchers.Default) {
            withTimeout(STREAM_TIMEOUT_MS) {
                downloader.activeDownloads.first { it[sampleEpisode.id] is DownloadStatus.Completed }
            }
        }

        assertEquals("e1.m4a", episodeDao.getById("e1")?.downloadFile)
        assertEquals((baseDir / "downloads" / "e1.m4a").toString(), downloader.getLocalPath("e1"))
    }

    @Test
    fun `the extension falls back to the url and then to mp3`() {
        assertEquals("m4a", audioExtension(ContentType.Application.OctetStream, "https://host/ep.M4A?token=1"))
        assertEquals("mp3", audioExtension(null, "https://host/download?id=1"))
        assertEquals("ogg", audioExtension(ContentType.parse("audio/ogg; codecs=opus"), "https://host/ep.mp3"))
    }

    private fun bytesOnDisk(): Long = fileSystem.listOrNull(baseDir / "downloads").orEmpty()
        .sumOf { fileSystem.metadata(it).size ?: 0 }

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
            awaitItem()
            downloader.download(sampleEpisode)
            testDispatcher.scheduler.runCurrent()
            awaitItem()

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
        episodeDao.insertAll(listOf(sampleEpisode.toEntity().copy(downloadFile = "e1.mp3")))

        downloader.delete("e1")
        testDispatcher.scheduler.advanceUntilIdle()

        kotlin.test.assertNull(downloader.getLocalPath("e1"))
        assertTrue(!fileSystem.exists(path))
    }

    private companion object {
        const val CHUNK = 64 * 1024
        const val STREAM_TIMEOUT_MS = 5_000L
        const val POLL_MS = 10L
    }
}
