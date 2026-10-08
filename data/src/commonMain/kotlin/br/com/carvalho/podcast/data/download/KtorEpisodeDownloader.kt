package br.com.carvalho.podcast.data.download

import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.observability.Metrics
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.data.local.dao.EpisodeDao
import br.com.carvalho.podcast.data.remote.toAppError
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException
import okio.Path
import okio.buffer
import okio.use
import kotlin.time.TimeSource

private const val TAG = "KtorEpisodeDownloader"
private const val PART_SUFFIX = ".part"

/**
 * Downloads episodes with Ktor and Okio and keeps their state. Android and iOS wrap it to keep downloads going in
 * the background; Desktop and Web use it as is, in the app's process.
 */
@Suppress("TooManyFunctions") // the EpisodeDownloader interface plus the hooks Android and iOS use
class KtorEpisodeDownloader(
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
        // Undispatched: the job is registered before download() returns, so an immediate cancel() finds it.
        scope.launch(start = CoroutineStart.UNDISPATCHED) { transfer(episode.id, episode.audioUrl) }
    }

    /**
     * Downloads in the caller's coroutine, so the caller decides how long it lives: this process ([download]) or an
     * Android worker that outlives the app. Cancelling the caller stops it and removes the partial file.
     */
    @Suppress("TooGenericExceptionCaught") // the boundary where any failure becomes a Failed status
    suspend fun transfer(episodeId: String, audioUrl: String) {
        val self = currentCoroutineContext().job
        var added = false
        downloadJobs.update { jobs ->
            added = episodeId !in jobs
            if (added) jobs + (episodeId to self) else jobs
        }
        if (!added) {
            AppLogger.d(TAG, "Download already in progress for $episodeId")
            return
        }

        // The episode only appears under its final name once it is whole; a failure leaves just the .part.
        val partPath = directories.downloadPath("$episodeId$PART_SUFFIX")
        val start = TimeSource.Monotonic.markNow()
        try {
            updateStatus(episodeId, DownloadStatus.Queued())

            // prepareGet + execute streams the body to disk; get() would hold the whole episode in memory first.
            val fileName = httpClient.prepareGet(audioUrl) {
                // An episode takes minutes; only connecting and silence are limited, not the whole transfer.
                timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS }
                onDownload { bytesSentTotal, contentLength ->
                    if (contentLength != null && contentLength > 0) {
                        val progress = bytesSentTotal.toFloat() / contentLength.toFloat()
                        updateStatus(episodeId, DownloadStatus.Downloading(progress, bytesSentTotal, contentLength))
                    }
                }
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    updateStatus(episodeId, DownloadStatus.Failed(AppError.Http(response.status.value)))
                    return@execute null
                }
                fileSystem.createDirectories(directories.downloadsDir)
                writeBody(response.bodyAsChannel(), partPath, response.contentLength())
                fileName(episodeId, response.contentType(), audioUrl).also {
                    fileSystem.atomicMove(partPath, directories.downloadPath(it))
                }
            } ?: return

            markDownloaded(episodeId, fileName)
            val bytes = fileSystem.metadataOrNull(directories.downloadPath(fileName))?.size ?: 0L
            Metrics.record(Metrics.DOWNLOAD, start.elapsedNow(), "bytes" to bytes)
        } catch (e: CancellationException) {
            AppLogger.d(TAG, "Download canceled for $episodeId")
            updateStatus(episodeId, DownloadStatus.Idle)
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "Download failed for $episodeId", e)
            updateStatus(episodeId, DownloadStatus.Failed(e.toAppError()))
        } finally {
            fileSystem.delete(partPath, mustExist = false)
            downloadJobs.update { jobs -> if (jobs[episodeId] === self) jobs - episodeId else jobs }
        }
    }

    /** Shows the episode as waiting while a platform scheduler holds it (no network yet, for instance). */
    fun markQueued(episodeId: String) = updateStatus(episodeId, DownloadStatus.Queued())

    /** Records a file already in the downloads folder as the episode's download. */
    internal suspend fun markDownloaded(episodeId: String, fileName: String) {
        episodeDao.updateDownloadFile(episodeId, fileName)
        val destPath = directories.downloadPath(fileName)
        updateStatus(episodeId, DownloadStatus.Completed(destPath.toString()))
        AppLogger.d(TAG, "Download completed for $episodeId: $destPath")
    }

    override suspend fun cancel(episodeId: String) {
        // Wait for the job: it may be moving the finished file into place, which delete() must see.
        downloadJobs.value[episodeId]?.cancelAndJoin()
        delete(episodeId)
    }

    @Suppress("TooGenericExceptionCaught") // a file or database failure is only logged; the row stays as it was
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

    override suspend fun usedBytes(): Long = withContext(Dispatchers.Default) {
        fileSystem.listOrNull(directories.downloadsDir).orEmpty().sumOf { fileSystem.metadataOrNull(it)?.size ?: 0L }
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

    internal fun updateStatus(episodeId: String, status: DownloadStatus) {
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

/** The downloaded file's name; the extension tells the platform player the format (AVPlayer reads nothing else). */
internal fun fileName(episodeId: String, contentType: ContentType?, audioUrl: String): String =
    "$episodeId.${audioExtension(contentType, audioUrl)}"

/**
 * The downloaded file's extension: from the response type, else from the URL (many hosts answer with
 * `application/octet-stream`), else mp3.
 */
internal fun audioExtension(contentType: ContentType?, audioUrl: String): String =
    EXTENSIONS_BY_TYPE[contentType?.withoutParameters()?.toString()?.lowercase()]
        ?: Url(audioUrl).encodedPath.substringAfterLast('/').substringAfterLast('.', "").lowercase()
            .takeIf { it in EXTENSIONS_BY_TYPE.values }
        ?: DEFAULT_EXTENSION
