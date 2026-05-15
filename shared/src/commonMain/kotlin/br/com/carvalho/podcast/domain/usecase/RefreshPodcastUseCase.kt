package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.data.mapper.toEpisode
import br.com.carvalho.podcast.data.mapper.toPodcast
import br.com.carvalho.podcast.data.remote.RssFeedDataSource
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.core.util.AppLogger
import kotlinx.coroutines.flow.first

private const val TAG = "RefreshUseCase"

class RefreshPodcastUseCase(
    private val rssDataSource: RssFeedDataSource,
    private val podcastRepository: PodcastRepository
) {
    suspend operator fun invoke(podcastId: String): Result<Unit> {
        val podcast = podcastRepository.getPodcastById(podcastId)
            ?: return Result.failure(Exception("Podcast not found"))

        AppLogger.i(TAG, "Refreshing podcast: ${podcast.title}")
        return rssDataSource.fetchFeed(podcast.feedUrl)
            .mapCatching { feed ->
                val updatedPodcast = feed.toPodcast(feedUrl = podcast.feedUrl)
                val updatedEpisodes = feed.episodes.map { 
                    it.toEpisode(
                        podcastId = updatedPodcast.id,
                        podcastTitle = updatedPodcast.title
                    ) 
                }

                podcastRepository.savePodcast(updatedPodcast)
                podcastRepository.saveEpisodes(updatedEpisodes)
                AppLogger.d(TAG, "Podcast ${podcast.title} updated with ${updatedEpisodes.size} episodes")
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
