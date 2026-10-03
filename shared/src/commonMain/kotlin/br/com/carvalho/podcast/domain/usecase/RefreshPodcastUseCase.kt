package br.com.carvalho.podcast.domain.usecase

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
            ?: return Result.failure(Exception("Podcast not found"))

        AppLogger.i(TAG, "Refreshing podcast: ${podcast.title}")
        return feedSource.fetch(podcast.feedUrl)
            .mapCatching { feed ->
                podcastRepository.savePodcast(feed.podcast)
                podcastRepository.saveEpisodes(feed.episodes)
                AppLogger.d(TAG, "Podcast ${podcast.title} updated with ${feed.episodes.size} episodes")
            }.onFailure { e ->
                AppLogger.e(TAG, "Failed to refresh podcast: ${podcast.title}", e)
            }
    }

    suspend fun refreshAll(): Result<Unit> {
        AppLogger.i(TAG, "Starting refresh for all podcasts")
        return try {
            val podcasts = podcastRepository.getPodcasts().first()
            podcasts.forEach { podcast ->
                invoke(podcast.id)
            }
            AppLogger.i(TAG, "All podcasts refreshed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error during bulk refresh", e)
            Result.failure(e)
        }
    }
}
