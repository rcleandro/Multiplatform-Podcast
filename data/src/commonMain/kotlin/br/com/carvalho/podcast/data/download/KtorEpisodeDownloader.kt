package br.com.carvalho.podcast.data.download

import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.data.local.dao.EpisodeDao
import br.com.carvalho.podcast.data.remote.toAppError
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CancellationException
import okio.IOException
import okio.Path
import okio.buffer
import okio.use

private const val TAG = "KtorEpisodeDownloader"
private const val PART_SUFFIX = ".part"

/**
 * Implementação base do [EpisodeDownloader] utilizando Ktor e Okio.
 */
open class KtorEpisodeDownloader(
    private val httpClient: HttpClient,
    private val episodeDao: EpisodeDao,
    private val directories: AppDirectories,
    ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : EpisodeDownloader {
    private val fileSystem = directories.fileSystem

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _activeDownloads = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    override val activeDownloads: StateFlow<Map<String, DownloadStatus>> = _activeDownloads.asStateFlow()

    // Updated from several coroutines at once; update {} makes check-and-add atomic on every platform.
    private val downloadJobs = MutableStateFlow<Map<String, Job>>(emptyMap())

    override suspend fun download(episode: Episode) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            // The episode only appears under its final name once it is whole; a failure leaves just the .part.
            val partPath = directories.downloadPath("${episode.id}$PART_SUFFIX")
            try {
                updateStatus(episode.id, DownloadStatus.Queued())

                // prepareGet + execute streams the body to disk; get() would hold the whole episode in memory first.
                val fileName = httpClient.prepareGet(episode.audioUrl) {
                    // An episode takes minutes; only connecting and silence are limited, not the whole transfer.
                    timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS }
                    onDownload { bytesSentTotal, contentLength ->
                        if (contentLength != null && contentLength > 0) {
                            val progress = bytesSentTotal.toFloat() / contentLength.toFloat()
                            updateStatus(episode.id, DownloadStatus.Downloading(progress, bytesSentTotal, contentLength))
                        }
                    }
                }.execute { response ->
                    if (!response.status.isSuccess()) {
                        updateStatus(episode.id, DownloadStatus.Failed(AppError.Http(response.status.value)))
                        return@execute null
                    }
                    fileSystem.createDirectories(directories.downloadsDir)
                    writeBody(response.bodyAsChannel(), partPath, response.contentLength())
                    // The extension tells the platform player the format (AVPlayer reads nothing else).
                    "${episode.id}.${audioExtension(response.contentType(), episode.audioUrl)}".also {
                        fileSystem.atomicMove(partPath, directories.downloadPath(it))
                    }
                } ?: return@launch

                episodeDao.updateDownloadFile(episode.id, fileName)
                val destPath = directories.downloadPath(fileName)

                updateStatus(episode.id, DownloadStatus.Completed(destPath.toString()))
                AppLogger.d(TAG, "Download completed for ${episode.id}: $destPath")

            } catch (e: CancellationException) {
                AppLogger.d(TAG, "Download canceled for ${episode.id}")
                updateStatus(episode.id, DownloadStatus.Idle)
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "Download failed for ${episode.id}", e)
                updateStatus(episode.id, DownloadStatus.Failed(e.toAppError()))
            } finally {
                fileSystem.delete(partPath, mustExist = false)
                val self = coroutineContext.job
                downloadJobs.update { jobs -> if (jobs[episode.id] === self) jobs - episode.id else jobs }
            }
        }

        var added = false
        downloadJobs.update { jobs ->
            added = episode.id !in jobs
            if (added) jobs + (episode.id to job) else jobs
        }
        if (added) {
            job.start()
        } else {
            AppLogger.d(TAG, "Download already in progress for ${episode.id}")
            job.cancel()
        }
    }

    override suspend fun pause(episodeId: String) {
        cancel(episodeId)
    }

    override suspend fun resume(episodeId: String) {
    }

    override suspend fun cancel(episodeId: String) {
        // Wait for the job: it may be moving the finished file into place, which delete() must see.
        downloadJobs.value[episodeId]?.cancelAndJoin()
        delete(episodeId)
    }

    override suspend fun delete(episodeId: String) {
        withContext(Dispatchers.Default) {
            try {
                episodeDao.getById(episodeId)?.downloadFile?.let {
                    fileSystem.delete(directories.downloadPath(it), mustExist = false)
                }
                episodeDao.updateDownloadFile(episodeId, null)
                updateStatus(episodeId, DownloadStatus.Idle)
                AppLogger.d(TAG, "Deleted local file for episode: $episodeId")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to delete episode: $episodeId", e)
            }
        }
    }

    override fun getDownloadStatus(episodeId: String): StateFlow<DownloadStatus> {
        return activeDownloads.map { it[episodeId] ?: DownloadStatus.Idle }
            .stateIn(scope, SharingStarted.WhileSubscribed(), DownloadStatus.Idle)
    }

    override suspend fun getLocalPath(episodeId: String): String? {
        val fileName = episodeDao.getById(episodeId)?.downloadFile ?: return null
        return directories.downloadPath(fileName).takeIf { fileSystem.exists(it) }?.toString()
    }

    private suspend fun writeBody(channel: ByteReadChannel, destPath: Path, contentLength: Long?) {
        var written = 0L
        fileSystem.sink(destPath).buffer().use { sink ->
            val buffer = ByteArray(AppConfig.DOWNLOAD_BUFFER_SIZE)
            while (!channel.isClosedForRead) {
                val read = channel.readAvailable(buffer)
                if (read > 0) {
                    sink.write(buffer, 0, read)
                    written += read
                }
            }
        }
        // A connection that drops mid-body can still end the channel normally.
        if (contentLength != null && written != contentLength) {
            throw IOException("Body ended at $written of $contentLength bytes")
        }
    }

    private fun updateStatus(episodeId: String, status: DownloadStatus) {
        _activeDownloads.value += (episodeId to status)
    }
}

private const val DEFAULT_EXTENSION = "mp3"

private val EXTENSIONS_BY_TYPE = mapOf(
    "audio/mpeg" to "mp3",
    "audio/mp3" to "mp3",
    "audio/mp4" to "m4a",
    "audio/x-m4a" to "m4a",
    "audio/m4a" to "m4a",
    "audio/aac" to "aac",
    "audio/ogg" to "ogg",
    "audio/opus" to "opus",
    "audio/wav" to "wav",
    "video/mp4" to "mp4",
)

/**
 * The downloaded file's extension: from the response type, else from the URL (many hosts answer with
 * `application/octet-stream`), else mp3.
 */
internal fun audioExtension(contentType: ContentType?, audioUrl: String): String =
    EXTENSIONS_BY_TYPE[contentType?.withoutParameters()?.toString()?.lowercase()]
        ?: Url(audioUrl).encodedPath.substringAfterLast('/').substringAfterLast('.', "").lowercase()
            .takeIf { it in EXTENSIONS_BY_TYPE.values }
        ?: DEFAULT_EXTENSION
