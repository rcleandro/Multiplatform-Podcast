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

    private val downloadJobs = mutableMapOf<String, Job>()

    override suspend fun download(episode: Episode) {
        if (downloadJobs.containsKey(episode.id)) {
            AppLogger.d(TAG, "Download already in progress for ${episode.id}")
            return
        }

        val job = scope.launch {
            val destPath = directories.downloadPath(episode.id)
            // The episode only appears under its final name once it is whole; a failure leaves just the .part.
            val partPath = destPath.parent!! / "${destPath.name}$PART_SUFFIX"
            try {
                updateStatus(episode.id, DownloadStatus.Queued())

                // prepareGet + execute streams the body to disk; get() would hold the whole episode in memory first.
                val completed = httpClient.prepareGet(episode.audioUrl) {
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
                        return@execute false
                    }
                    fileSystem.createDirectories(directories.downloadsDir)
                    writeBody(response.bodyAsChannel(), partPath, response.contentLength())
                    fileSystem.atomicMove(partPath, destPath)
                    true
                }
                if (!completed) return@launch

                episodeDao.updateDownloadStatus(episode.id, true)

                updateStatus(episode.id, DownloadStatus.Completed(destPath.toString()))
                AppLogger.d(TAG, "Download completed for ${episode.id}: $destPath")

            } catch (e: kotlinx.coroutines.CancellationException) {
                AppLogger.e(TAG, "Download canceled for ${episode.id}", e)
                updateStatus(episode.id, DownloadStatus.Idle)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Download failed for ${episode.id}", e)
                updateStatus(episode.id, DownloadStatus.Failed(e.toAppError()))
            } finally {
                fileSystem.delete(partPath, mustExist = false)
                downloadJobs.remove(episode.id)
            }
        }

        downloadJobs[episode.id] = job
    }

    override suspend fun pause(episodeId: String) {
        cancel(episodeId)
    }

    override suspend fun resume(episodeId: String) {
    }

    override suspend fun cancel(episodeId: String) {
        downloadJobs[episodeId]?.cancel()
        downloadJobs.remove(episodeId)

        val destPath = directories.downloadPath(episodeId)
        if (fileSystem.exists(destPath)) {
            fileSystem.delete(destPath)
        }

        episodeDao.updateDownloadStatus(episodeId, false)
        updateStatus(episodeId, DownloadStatus.Idle)
    }

    override suspend fun delete(episodeId: String) {
        val destPath = directories.downloadPath(episodeId)

        withContext(Dispatchers.Default) {
            try {
                if (fileSystem.exists(destPath)) {
                    fileSystem.delete(destPath)
                }
                episodeDao.updateDownloadStatus(episodeId, false)
                updateStatus(episodeId, DownloadStatus.Idle)
                AppLogger.d(TAG, "Deleted local file for episode: $episodeId")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to delete episode: $episodeId", e)
            }
        }
    }

    override fun getDownloadStatus(episodeId: String): StateFlow<DownloadStatus> {
        return activeDownloads.map { it[episodeId] ?: DownloadStatus.Idle }
            .stateIn(scope, SharingStarted.WhileSubscribed(), DownloadStatus.Idle)
    }

    override fun getLocalPath(episodeId: String): String? {
        val destPath = directories.downloadPath(episodeId)
        return if (fileSystem.exists(destPath)) destPath.toString() else null
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
