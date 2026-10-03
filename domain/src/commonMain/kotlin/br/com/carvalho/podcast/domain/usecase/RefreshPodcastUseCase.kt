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

    /** Refreshes every podcast, even after one fails, and reports the first failure. */
    suspend fun refreshAll(): Result<Unit> {
        AppLogger.i(TAG, "Starting refresh for all podcasts")
        val failure = podcastRepository.getPodcasts().first()
            .map { invoke(it.id) }
            .firstNotNullOfOrNull { it.exceptionOrNull() }
        return if (failure == null) Result.success(Unit) else Result.failure(failure)
    }
}
