package br.com.carvalho.podcast.data.download

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.data.local.dao.FakeEpisodeDao
import br.com.carvalho.podcast.data.mapper.toEntity
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.model.Episode
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** The audio URL comes from the feed (22.5, T5 of ADR 0014): what it answers must not fill the disk or pass as audio. */
class HostileDownloadTest {
    private val episodeDao = FakeEpisodeDao()
    private val fileSystem = FakeFileSystem()
    private val baseDir = "/test".toPath()
    private val episode = Episode(
        id = "e1", podcastId = "p1", title = "Title", description = null, audioUrl = "https://test.com/audio.mp3",
        imageUrl = null, duration = 100, publishDate = 0, isPlayed = false, playbackPosition = 0,
        isDownloaded = false, fileSize = null,
    )

    @Test
    fun `a size that does not fit under the limit fails before anything is written`() = runTest {
        val status = download(
            MockEngine { respond(ByteArray(ROOM * 2), HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, "${ROOM * 2}")) },
            limit = ROOM.toLong(),
        )

        assertEquals(DownloadStatus.Failed(AppError.StorageLimit), status)
        assertEquals(0, bytesOnDisk())
    }

    @Test
    fun `a body without a size stops where the room ends`() = runTest {
        // An endless radio stream as the "episode": no length, it would write until the disk is full.
        val endless = ByteChannel()
        backgroundScope.launch(Dispatchers.Default) { repeat(ROOM) { endless.writeFully(ByteArray(CHUNK)) } }

        val status = download(MockEngine { respond(endless, HttpStatusCode.OK) }, limit = ROOM.toLong())

        assertEquals(DownloadStatus.Failed(AppError.StorageLimit), status)
        assertEquals(0, bytesOnDisk())
    }

    @Test
    fun `a download that would leave the disk without room says the disk is full`() = runTest {
        val status = download(
            MockEngine { respond(ByteArray(CHUNK), HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, "$CHUNK")) },
            freeDisk = RESERVED_DISK_BYTES + CHUNK / 2,
        )

        assertEquals(DownloadStatus.Failed(AppError.StorageFull), status)
        assertEquals(0, bytesOnDisk())
    }

    @Test
    fun `a web page in place of the audio is not kept as an episode`() = runTest {
        val status = download(
            MockEngine { respond("<html>Log in</html>", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html")) },
        )

        assertIs<DownloadStatus.Failed>(status)
        assertNull(episodeDao.getById(episode.id)?.downloadFile)
        assertEquals(0, bytesOnDisk())
    }

    private suspend fun download(engine: MockEngine, limit: Long? = null, freeDisk: Long? = null): DownloadStatus? {
        episodeDao.insertAll(listOf(episode.toEntity()))
        val downloader = KtorEpisodeDownloader(
            HttpClient(engine), episodeDao, AppDirectories(fileSystem, baseDir), Dispatchers.Default,
            storageLimitBytes = { limit }, freeDiskBytes = { freeDisk },
        )
        downloader.download(episode)
        return withContext(Dispatchers.Default) {
            withTimeout(TIMEOUT_MS) {
                downloader.activeDownloads.first { downloads ->
                    downloads[episode.id].let { it is DownloadStatus.Failed || it is DownloadStatus.Completed }
                }
            }
        }[episode.id]
    }

    private fun bytesOnDisk(): Long = fileSystem.listOrNull(baseDir / "downloads").orEmpty()
        .sumOf { fileSystem.metadata(it).size ?: 0L }

    private companion object {
        const val CHUNK = 1_024
        const val ROOM = 64 * CHUNK
        const val TIMEOUT_MS = 10_000L
    }
}
