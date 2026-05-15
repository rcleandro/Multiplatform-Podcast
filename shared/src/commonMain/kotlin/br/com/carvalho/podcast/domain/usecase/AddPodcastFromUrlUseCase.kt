package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.data.mapper.toEpisode
import br.com.carvalho.podcast.data.mapper.toPodcast
import br.com.carvalho.podcast.data.remote.RssFeedDataSource
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastError
import br.com.carvalho.podcast.domain.repository.PodcastRepository

class AddPodcastFromUrlUseCase(
    private val rssDataSource: RssFeedDataSource,
    private val podcastRepository: PodcastRepository
) {
    suspend operator fun invoke(url: String): Result<Podcast> {
        if (podcastRepository.getPodcastById(url) != null) {
            return Result.failure(PodcastError.AlreadyExists)
        }

        return rssDataSource.fetchFeed(url)
            .mapCatching { feed ->
                val podcast = feed.toPodcast(feedUrl = url)
                val episodes = feed.episodes.map {
                    it.toEpisode(
                        podcastId = podcast.id,
                        podcastTitle = podcast.title
                    )
                }

                podcastRepository.savePodcast(podcast)
                podcastRepository.saveEpisodes(episodes)

                podcast
            }
    }
}
