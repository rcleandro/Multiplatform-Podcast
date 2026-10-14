package br.com.carvalho.podcast.core.refresh

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.domain.usecase.RefreshPodcastUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

/** One look at every feed, the same as pulling the library down; new episodes land in "New" (18.7). */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {
    private val refresh: RefreshPodcastUseCase by inject()

    override suspend fun doWork(): Result {
        val summary = refresh.refreshAll()
        AppLogger.i(TAG, "Refreshed ${summary.total} podcasts in the background, ${summary.failures.size} failed")
        // Every feed failing is most likely the network: try again later instead of waiting a whole interval.
        return if (summary.allFailed) Result.retry() else Result.success()
    }
}
