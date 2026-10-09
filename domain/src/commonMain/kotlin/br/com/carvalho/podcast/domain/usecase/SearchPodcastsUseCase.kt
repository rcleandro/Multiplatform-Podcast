package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.repository.PodcastDirectory
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.first

private const val MIN_TERM_LENGTH = 2

/**
 * The directory of "Add podcast": search (18.1) and charts per category (18.2). A term shorter than two letters does
 * not reach the network, and each result says whether the library already has it, by the same feed key that adding by
 * address uses.
 */
class SearchPodcastsUseCase(
    private val directory: PodcastDirectory,
    private val podcastRepository: PodcastRepository,
) {
    suspend operator fun invoke(term: String): Result<List<DirectoryResult>> {
        val trimmed = term.trim()
        if (trimmed.length < MIN_TERM_LENGTH) return Result.success(emptyList())
        return directory.search(trimmed).markingSubscribed()
    }

    /** The most popular podcasts of [category] (18.2), or of all of them, marked like a search. */
    suspend fun top(category: PodcastCategory?): Result<List<DirectoryResult>> =
        directory.top(category).markingSubscribed()

    private suspend fun Result<List<DirectoryPodcast>>.markingSubscribed(): Result<List<DirectoryResult>> {
        val saved = podcastRepository.getPodcasts().first().map { feedUrlKey(it.feedUrl) }.toSet()
        return map { found -> found.map { DirectoryResult(it, isSubscribed = feedUrlKey(it.feedUrl) in saved) } }
    }
}

data class DirectoryResult(val podcast: DirectoryPodcast, val isSubscribed: Boolean)
