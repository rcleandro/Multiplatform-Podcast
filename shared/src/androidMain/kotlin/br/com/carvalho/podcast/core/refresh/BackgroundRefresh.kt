package br.com.carvalho.podcast.core.refresh

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.new_episodes_channel
import br.com.carvalho.podcast.core.ui.generated.resources.new_episodes_count
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.RefreshInBackgroundUseCase
import br.com.carvalho.podcast.shared.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.mp.KoinPlatform.getKoin
import java.util.concurrent.TimeUnit

private const val TAG = "BackgroundRefresh"
private const val WORK_NAME = "refresh-podcasts"

/**
 * Looks for new episodes while the app is closed (18.18), every few hours as Settings say, only with a network.
 * Android may run it later than asked to save battery; that is the price of not keeping the app awake.
 */
object BackgroundRefresh {
    /** Schedules the refresh now and again whenever its interval changes; 0 cancels it. */
    fun keepScheduled(context: Context, scope: CoroutineScope) {
        val workManager = WorkManager.getInstance(context)
        getKoin().get<PreferencesRepository>().refreshIntervalHours
            .onEach { hours ->
                if (hours <= 0) {
                    workManager.cancelUniqueWork(WORK_NAME)
                } else {
                    val request = PeriodicWorkRequestBuilder<RefreshWorker>(hours.toLong(), TimeUnit.HOURS)
                        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .build()
                    // UPDATE keeps the next run's timing when only the interval changes.
                    workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
                }
            }
            .launchIn(scope)
    }
}

/**
 * One look at every feed, the same as pulling the library down; new episodes land in "New" (18.7) and, unless
 * Settings or their podcast say otherwise, in a notification.
 */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {
    private val refresh: RefreshInBackgroundUseCase by inject()

    override suspend fun doWork(): Result {
        val result = refresh()
        AppLogger.i(TAG, "Background refresh found ${result.toNotify.size} episodes to tell about")
        if (result.toNotify.isNotEmpty()) notify(result.toNotify)
        // Every feed failing is most likely the network: try again later instead of waiting a whole interval.
        return if (result.allFailed) Result.retry() else Result.success()
    }

    /** One notification for what this look found: the episode itself, or how many and from which podcasts. */
    @SuppressLint("MissingPermission") // checked first; without it Android 13+ shows nothing
    private suspend fun notify(episodes: List<Episode>) {
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(getString(Res.string.new_episodes_channel))
                .build()
        )
        val single = episodes.singleOrNull()
        val count = episodes.size
        val title = single?.podcastTitle ?: getPluralString(Res.plurals.new_episodes_count, count, count)
        val text = single?.title ?: episodes.mapNotNull { it.podcastTitle }.distinct().joinToString(", ")
        val open = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?.let { PendingIntent.getActivity(applicationContext, 0, it, PendingIntent.FLAG_IMMUTABLE) }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_new_episodes)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "new_episodes"

        /** One at a time: a newer look replaces what the last one said. */
        const val NOTIFICATION_ID = 1818
    }
}
