package br.com.carvalho.podcast.core.download

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.downloads
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.data.download.KtorEpisodeDownloader
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val TAG = "DownloadWorker"
private const val KEY_EPISODE_ID = "episodeId"
private const val KEY_AUDIO_URL = "audioUrl"
private const val KEY_TITLE = "title"
private const val CHANNEL_ID = "downloads"

private fun workName(episodeId: String) = "download-$episodeId"

/**
 * Hands each download to WorkManager, so it goes on with the app in the background or closed, and waits for a
 * network. The transfer itself, its state and the files stay with [KtorEpisodeDownloader].
 */
class WorkManagerEpisodeDownloader(
    private val downloader: KtorEpisodeDownloader,
    private val workManager: WorkManager,
) : EpisodeDownloader by downloader {

    override suspend fun download(episode: Episode) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            // ponytail: any network; the Wi-Fi only preference comes with the Settings screen (18.5).
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setInputData(
                workDataOf(KEY_EPISODE_ID to episode.id, KEY_AUDIO_URL to episode.audioUrl, KEY_TITLE to episode.title)
            )
            .build()
        downloader.markQueued(episode.id)
        // KEEP: tapping again while the episode is waiting or downloading changes nothing.
        workManager.enqueueUniqueWork(workName(episode.id), ExistingWorkPolicy.KEEP, request)
    }

    override suspend fun cancel(episodeId: String) {
        workManager.cancelUniqueWork(workName(episodeId))
        downloader.cancel(episodeId)
    }
}

/** Runs one download; a foreground notification lets it take longer than WorkManager's ten minutes. */
class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {
    private val downloader: KtorEpisodeDownloader by inject()

    override suspend fun doWork(): Result {
        val episodeId = inputData.getString(KEY_EPISODE_ID)
        val audioUrl = inputData.getString(KEY_AUDIO_URL)
        if (episodeId == null || audioUrl == null) return Result.failure()
        try {
            setForeground(foregroundInfo(episodeId))
        } catch (e: IllegalStateException) {
            // Android 12+ refuses a foreground service started from the background; the download runs without it.
            AppLogger.e(TAG, "Downloading $episodeId without a foreground notification", e)
        }
        downloader.transfer(episodeId, audioUrl)
        return Result.success()
    }

    private suspend fun foregroundInfo(episodeId: String): ForegroundInfo {
        val channelName = getString(Res.string.downloads)
        NotificationManagerCompat.from(applicationContext).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(channelName)
                .build()
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(channelName)
            .setContentText(inputData.getString(KEY_TITLE))
            .setProgress(0, 0, true)
            .setOngoing(true)
            .build()
        return ForegroundInfo(episodeId.hashCode(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }
}
