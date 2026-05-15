package br.com.carvalho.podcast.data.download

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.FileUtils
import br.com.carvalho.podcast.data.local.dao.EpisodeDao
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
import okio.buffer

private const val TAG = "KtorEpisodeDownloader"

/**
 * Implementação base do [EpisodeDownloader] utilizando Ktor e Okio.
 */
open class KtorEpisodeDownloader(
    private val httpClient: HttpClient,
    private val episodeDao: EpisodeDao,
    private val fileSystem: okio.FileSystem = FileUtils.fileSystem,
    private val baseDir: okio.Path = FileUtils.baseDir,
    ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : EpisodeDownloader {

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
            try {
                updateStatus(episode.id, DownloadStatus.Queued())

                val response = httpClient.get(episode.audioUrl) {
                    onDownload { bytesSentTotal, contentLength ->
                        if (contentLength != null && contentLength > 0) {
                            val progress = bytesSentTotal.toFloat() / contentLength.toFloat()
                            updateStatus(episode.id, DownloadStatus.Downloading(progress, bytesSentTotal, contentLength))
                        }
                    }
                }

                if (!response.status.isSuccess()) {
                    updateStatus(episode.id, DownloadStatus.Failed("HTTP Error: ${response.status}"))
                    return@launch
                }

                val fileName = "${episode.id}.mp3"
                val destPath = baseDir / "downloads" / fileName

                if (!fileSystem.exists(baseDir / "downloads")) {
                    fileSystem.createDirectories(baseDir / "downloads")
                }

                val channel = response.bodyAsChannel()
                val sink = fileSystem.sink(destPath).buffer()
                try {
                    val buffer = ByteArray(8192)
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer)
                        if (read > 0) {
                            sink.write(buffer, 0, read)
                        }
                    }
                } finally {
                    sink.close()
                }

                // Atualiza no banco de dados
                episodeDao.updateDownloadStatus(episode.id, true)

                updateStatus(episode.id, DownloadStatus.Completed(destPath.toString()))
                AppLogger.d(TAG, "Download completed for ${episode.id}: $destPath")

            } catch (e: kotlinx.coroutines.CancellationException) {
                AppLogger.e(TAG, "Download canceled for ${episode.id}", e)
                updateStatus(episode.id, DownloadStatus.Idle)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Download failed for ${episode.id}", e)
                updateStatus(episode.id, DownloadStatus.Failed(e.message ?: "Unknown error"))
            } finally {
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

        val fileName = "${episodeId}.mp3"
        val destPath = baseDir / "downloads" / fileName
        if (fileSystem.exists(destPath)) {
            fileSystem.delete(destPath)
        }

        episodeDao.updateDownloadStatus(episodeId, false)
        updateStatus(episodeId, DownloadStatus.Idle)
    }

    override suspend fun delete(episodeId: String) {
        val fileName = "${episodeId}.mp3"
        val destPath = baseDir / "downloads" / fileName

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
        val fileName = "${episodeId}.mp3"
        val destPath = baseDir / "downloads" / fileName
        return if (fileSystem.exists(destPath)) destPath.toString() else null
    }

    private fun updateStatus(episodeId: String, status: DownloadStatus) {
        _activeDownloads.value += (episodeId to status)
    }
}
