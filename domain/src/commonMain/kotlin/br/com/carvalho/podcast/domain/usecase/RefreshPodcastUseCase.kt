package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.domain.repository.FeedSource
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.core.observability.Metrics
import br.com.carvalho.podcast.core.util.AppLogger
import kotlin.time.TimeSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val TAG = "RefreshUseCase"

// A few feeds at a time: faster than one by one, without opening dozens of connections on a phone.
private const val MAX_CONCURRENT_REFRESHES = 4

class RefreshPodcastUseCase(
    private val feedSource: FeedSource,
    private val podcastRepository: PodcastRepository
) {
    suspend operator fun invoke(podcastId: String): Result<Unit> {
        val podcast = podcastRepository.getPodcastById(podcastId)
            ?: return Result.failure(AppError.NotFound)

        AppLogger.i(TAG, "Refreshing podcast: ${podcast.title}")
        val start = TimeSource.Monotonic.markNow()
        var outcome = "unchanged"
        return feedSource.fetchIfChanged(podcast.feedUrl, podcast.feedVersion, podcast.id)
            .mapCatching { feed ->
                if (feed == null) {
                    AppLogger.d(TAG, "Podcast ${podcast.title} has not changed")
                } else {
                    // The id never changes; the address follows the feed when it moves.
                    val feedUrl = movedFeedUrl(podcast.feedUrl, feed.movedTo) ?: podcast.feedUrl
                    podcastRepository.saveFeed(feed.podcast.copy(id = podcast.id, feedUrl = feedUrl), feed.episodes)
                    outcome = "updated"
                    AppLogger.d(TAG, "Podcast ${podcast.title} updated with ${feed.episodes.size} episodes")
                }
            }.onFailure { e ->
                outcome = "failed"
                AppLogger.e(TAG, "Failed to refresh podcast: ${podcast.title}", e)
            }.also {
                Metrics.record(Metrics.FEED_REFRESH, start.elapsedNow(), "outcome" to outcome)
            }
    }

    /** Refreshes every podcast, even after one fails, and says how many failed and why. */
    suspend fun refreshAll(): RefreshSummary {
        AppLogger.i(TAG, "Starting refresh for all podcasts")
        val start = TimeSource.Monotonic.markNow()
        val podcasts = podcastRepository.getPodcasts().first()
        val permits = Semaphore(MAX_CONCURRENT_REFRESHES)
        val results = coroutineScope {
            podcasts.map { async { permits.withPermit { invoke(it.id) } } }.awaitAll()
        }
        return RefreshSummary(total = results.size, failures = results.mapNotNull { it.exceptionOrNull() }).also {
            Metrics.record(
                Metrics.REFRESH_ALL, start.elapsedNow(), "podcasts" to it.total, "failed" to it.failures.size,
            )
        }
    }
}

data class RefreshSummary(val total: Int, val failures: List<Throwable>) {
    val allFailed: Boolean get() = total > 0 && failures.size == total
}
