package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.AppError
import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FeedSource
import br.com.carvalho.podcast.domain.repository.PodcastDirectory
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.first

/** The podcast a link leads to (18.4), to show with "Subscribe"; [libraryId] when the library already has it. */
data class LinkedPodcast(val podcast: DirectoryPodcast, val libraryId: String?)

/**
 * Opens a podcast link: an Apple Podcasts show comes from the directory, a feed is read to show what it is. A feed the
 * library has, by address or by one the feed declares, is not subscribed to twice. An unknown link is
 * [AppError.NotFound], and so is an explicit podcast under Brazil's ECA Digital (26.3).
 */
class OpenPodcastLinkUseCase(
    private val directory: PodcastDirectory,
    private val feedSource: FeedSource,
    private val podcastRepository: PodcastRepository,
    private val explicitContent: ExplicitContent = ExplicitContent(),
) {
    suspend operator fun invoke(link: String): Result<LinkedPodcast> = open(link).mapCatching { linked ->
        if (linked.podcast.explicit && explicitContent.rule.value.hidesExplicit) throw AppError.NotFound
        linked
    }

    private suspend fun open(link: String): Result<LinkedPodcast> {
        val library = podcastRepository.getPodcasts().first().associateBy { feedUrlKey(it.feedUrl) }
        fun saved(url: String) = library[feedUrlKey(url)]
        return when (val target = podcastLink(link)) {
            null -> Result.failure(AppError.NotFound)
            is PodcastLink.Apple -> directory.lookup(target.id).mapCatching { found ->
                found ?: throw AppError.NotFound
                LinkedPodcast(found, saved(found.feedUrl)?.id)
            }
            is PodcastLink.Feed -> saved(target.url)
                ?.let { Result.success(LinkedPodcast(it.toDirectoryPodcast(it.episodeCount), it.id)) }
                ?: feedSource.fetch(target.url).map { feed ->
                    val podcast = feed.podcast.copy(feedUrl = target.url)
                    LinkedPodcast(
                        podcast.toDirectoryPodcast(feed.episodes.size),
                        feed.declaredUrls.firstNotNullOfOrNull(::saved)?.id,
                    )
                }
        }
    }

    // The feed's categories are in English: no genre rather than one in another language.
    private fun Podcast.toDirectoryPodcast(episodes: Int) = DirectoryPodcast(
        title = title,
        author = author,
        artworkUrl = imageUrl,
        feedUrl = feedUrl,
        genre = null,
        episodeCount = episodes,
        explicit = explicit,
    )
}
