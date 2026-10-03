package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.domain.repository.FeedSource
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.core.util.AppLogger
import kotlinx.coroutines.flow.first

private const val TAG = "RefreshUseCase"

class RefreshPodcastUseCase(
    private val feedSource: FeedSource,
    private val podcastRepository: PodcastRepository
) {
    suspend operator fun invoke(podcastId: String): Result<Unit> {
        val podcast = podcastRepository.getPodcastById(podcastId)
            ?: return Result.failure(AppError.NotFound)

        AppLogger.i(TAG, "Refreshing podcast: ${podcast.title}")
        return feedSource.fetch(podcast.feedUrl)
            .mapCatching { feed ->
                podcastRepository.saveFeed(feed.podcast, feed.episodes)
                AppLogger.d(TAG, "Podcast ${podcast.title} updated with ${feed.episodes.size} episodes")
            }.onFailure { e ->
                AppLogger.e(TAG, "Failed to refresh podcast: ${podcast.title}", e)
            }
    }

    /** Refreshes every podcast, even after one fails, and says how many failed and why. */
    suspend fun refreshAll(): RefreshSummary {
        AppLogger.i(TAG, "Starting refresh for all podcasts")
        val results = podcastRepository.getPodcasts().first().map { invoke(it.id) }
        return RefreshSummary(total = results.size, failures = results.mapNotNull { it.exceptionOrNull() })
    }
}

data class RefreshSummary(val total: Int, val failures: List<Throwable>) {
    val allFailed: Boolean get() = total > 0 && failures.size == total
}
