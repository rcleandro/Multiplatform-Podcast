package br.com.carvalho.podcast.data.download

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.data.remote.toAppError
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import okio.IOException
import okio.Path.Companion.toPath
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "UrlSessionEpisodeDownloader"
private const val SESSION_ID = "br.com.carvalho.podcast.downloads"

/**
 * Hands each download to a background URLSession: iOS carries on with the transfer while the app is suspended or
 * closed, and relaunches it to deliver the file. The state and the files stay with [KtorEpisodeDownloader].
 * Created when the app starts, so a relaunch reconnects to the session and receives its pending events.
 */
class UrlSessionEpisodeDownloader(
    private val downloader: KtorEpisodeDownloader,
    private val directories: AppDirectories,
) : EpisodeDownloader by downloader {
    private val session = NSURLSession.sessionWithConfiguration(
        NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(SESSION_ID), SessionDelegate(), null
    )

    // Both touched only on the main queue.
    private var eventsHandler: (() -> Unit)? = null
    private var eventsFinished = false

    override suspend fun download(episode: Episode) {
        val status = downloader.activeDownloads.value[episode.id]
        if (status is DownloadStatus.Queued || status is DownloadStatus.Downloading) return
        val url = NSURL.URLWithString(episode.audioUrl)
        if (url == null) {
            downloader.updateStatus(episode.id, DownloadStatus.Failed(AppError.Unknown(null)))
            return
        }
        downloader.markQueued(episode.id)
        session.downloadTaskWithURL(url).apply {
            taskDescription = episode.id
            resume()
        }
    }

    override suspend fun cancel(episodeId: String) {
        tasks().filter { it.taskDescription == episodeId }.forEach { it.cancel() }
        downloader.cancel(episodeId)
    }

    /**
     * iOS relaunched the app to deliver this session's events; [handler] must run once they are handled. Called on
     * the main queue by the app delegate.
     */
    fun onBackgroundEvents(handler: () -> Unit) {
        if (eventsFinished) {
            eventsFinished = false
            handler()
        } else {
            eventsHandler = handler
        }
    }

    private suspend fun tasks(): List<NSURLSessionTask> = suspendCoroutine { continuation ->
        session.getAllTasksWithCompletionHandler { tasks ->
            continuation.resume(tasks.orEmpty().filterIsInstance<NSURLSessionTask>())
        }
    }

    private inner class SessionDelegate : NSObject(), NSURLSessionDownloadDelegateProtocol {

        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didWriteData: Long,
            totalBytesWritten: Long,
            totalBytesExpectedToWrite: Long,
        ) {
            val episodeId = downloadTask.taskDescription ?: return
            if (totalBytesExpectedToWrite <= 0) return
            val progress = totalBytesWritten.toFloat() / totalBytesExpectedToWrite.toFloat()
            downloader.updateStatus(
                episodeId, DownloadStatus.Downloading(progress, totalBytesWritten, totalBytesExpectedToWrite)
            )
        }

        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didFinishDownloadingToURL: NSURL,
        ) {
            val episodeId = downloadTask.taskDescription ?: return
            val response = downloadTask.response as? NSHTTPURLResponse
            val status = response?.statusCode?.toInt() ?: 0
            if (HttpStatusCode.fromValue(status).isSuccess()) {
                keep(episodeId, downloadTask, response, didFinishDownloadingToURL)
            } else {
                downloader.updateStatus(episodeId, DownloadStatus.Failed(AppError.Http(status)))
            }
        }

        /** Moves the finished file into the downloads folder; iOS deletes [location] once the delegate returns. */
        private fun keep(
            episodeId: String,
            downloadTask: NSURLSessionDownloadTask,
            response: NSHTTPURLResponse?,
            location: NSURL,
        ) {
            val contentType = response?.MIMEType?.let { runCatching { ContentType.parse(it) }.getOrNull() }
            val audioUrl = downloadTask.originalRequest?.URL?.absoluteString.orEmpty()
            val fileName = fileName(episodeId, contentType, audioUrl)
            try {
                val fileSystem = directories.fileSystem
                val destPath = directories.downloadPath(fileName)
                fileSystem.createDirectories(directories.downloadsDir)
                fileSystem.delete(destPath, mustExist = false)
                fileSystem.atomicMove(location.path!!.toPath(), destPath)
                // Blocking the session's own queue: the database must be written before iOS suspends the app again.
                runBlocking { downloader.markDownloaded(episodeId, fileName) }
            } catch (e: IOException) {
                AppLogger.e(TAG, "Could not keep the download of $episodeId", e)
                downloader.updateStatus(episodeId, DownloadStatus.Failed(e.toAppError()))
            }
        }

        override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
            val episodeId = task.taskDescription
            val error = didCompleteWithError
            // A cancelled task was cancel()'s doing, which already reset the state.
            val cancelled = error?.domain == NSURLErrorDomain && error?.code == NSURLErrorCancelled
            if (episodeId == null || error == null || cancelled) return
            AppLogger.e(TAG, "Download failed for $episodeId: ${error.domain} ${error.code}")
            val reason = if (error.domain == NSURLErrorDomain) AppError.NoConnection else AppError.Unknown(null)
            downloader.updateStatus(episodeId, DownloadStatus.Failed(reason))
        }

        override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) {
            dispatch_async(dispatch_get_main_queue()) {
                val handler = eventsHandler
                eventsHandler = null
                if (handler != null) handler() else eventsFinished = true
            }
        }
    }
}
