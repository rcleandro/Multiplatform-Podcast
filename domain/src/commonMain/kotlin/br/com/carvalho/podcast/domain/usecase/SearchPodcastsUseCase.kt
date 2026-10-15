package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.repository.PodcastDirectory
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first

private const val MIN_TERM_LENGTH = 2

/**
 * The directory of "Add podcast": search (18.1) and charts per category (18.2). A term shorter than two letters does
 * not reach the network, and each result says whether the library already has it, by the same feed key that adding by
 * address uses; the same feed listed twice comes once. With "Show explicit content" off, explicit shows stay out
 * (18.5).
 */
class SearchPodcastsUseCase(
    private val directory: PodcastDirectory,
    private val podcastRepository: PodcastRepository,
    private val preferences: PreferencesRepository,
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
        // Apple sometimes lists one feed under two entries: one row each, or the list would show it twice.
        return map { found ->
            found.distinctBy { feedUrlKey(it.feedUrl) }
                .filter { preferences.showExplicit.value || !it.explicit }
                .map { DirectoryResult(it, isSubscribed = feedUrlKey(it.feedUrl) in saved) }
        }
    }
}

data class DirectoryResult(val podcast: DirectoryPodcast, val isSubscribed: Boolean)
