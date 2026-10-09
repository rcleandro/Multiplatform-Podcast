package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.repository.PodcastDirectory
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.first

private const val MIN_TERM_LENGTH = 2

/**
 * The directory search of "Add podcast" (18.1). A term shorter than two letters does not reach the network, and each
 * result says whether the library already has it, by the same feed key that adding by address uses.
 */
class SearchPodcastsUseCase(
    private val directory: PodcastDirectory,
    private val podcastRepository: PodcastRepository,
) {
    suspend operator fun invoke(term: String): Result<List<DirectoryResult>> {
        val trimmed = term.trim()
        if (trimmed.length < MIN_TERM_LENGTH) return Result.success(emptyList())
        return directory.search(trimmed).map { found ->
            val saved = podcastRepository.getPodcasts().first().map { feedUrlKey(it.feedUrl) }.toSet()
            found.map { DirectoryResult(it, isSubscribed = feedUrlKey(it.feedUrl) in saved) }
        }
    }
}

data class DirectoryResult(val podcast: DirectoryPodcast, val isSubscribed: Boolean)
