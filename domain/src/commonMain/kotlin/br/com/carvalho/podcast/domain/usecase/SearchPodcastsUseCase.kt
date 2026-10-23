package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.DirectoryPodcast
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.model.topCategories
import br.com.carvalho.podcast.domain.repository.PodcastDirectory
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first

private const val MIN_TERM_LENGTH = 2
private const val CATEGORIES_FOR_YOU = 2
private const val RECOMMENDATIONS = 10

/**
 * The directory of "Add podcast": search (18.1) and charts per category (18.2). A term shorter than two letters does
 * not reach the network, and each result says whether the library already has it, by the same feed key that adding by
 * address uses; the same feed listed twice comes once. With "Show explicit content" off, explicit shows stay out
 * (18.5), and so they do under Brazil's ECA Digital (26.3).
 */
class SearchPodcastsUseCase(
    private val directory: PodcastDirectory,
    private val podcastRepository: PodcastRepository,
    private val preferences: PreferencesRepository,
    private val explicitContent: ExplicitContent = ExplicitContent(),
    private val childSafety: ChildSafety = ChildSafety(),
) {
    suspend operator fun invoke(term: String): Result<List<DirectoryResult>> {
        val trimmed = term.trim()
        if (trimmed.length < MIN_TERM_LENGTH) return Result.success(emptyList())
        return directory.search(trimmed).markingSubscribed()
    }

    /** The most popular podcasts of [category] (18.2), or of all of them, marked like a search. */
    suspend fun top(category: PodcastCategory?): Result<List<DirectoryResult>> =
        directory.top(category).markingSubscribed()

    /**
     * Podcasts like the library's [podcastId] (19.6), with no backend to say who listens to what: its author's other
     * shows first, then the chart of its first category. Neither it nor what the library has; null when the library
     * does not have it. Fails only when both lookups fail.
     */
    suspend fun similarTo(podcastId: String): Similar? {
        val podcast = podcastRepository.getPodcastById(podcastId) ?: return null
        val author = podcast.author?.trim().orEmpty()
        val byAuthor = if (author.length < MIN_TERM_LENGTH) {
            Result.success(emptyList())
        } else {
            directory.search(author).map { found -> found.filter { it.author.equals(author, ignoreCase = true) } }
        }
        val byCategory = podcast.topCategories.firstOrNull()?.let { directory.top(it) } ?: Result.success(emptyList())
        return Similar(podcast.title, recommending(listOf(byAuthor, byCategory)) { it + feedUrlKey(podcast.feedUrl) })
    }

    /**
     * For the library (19.6): the charts of its two most common categories, taking turns. Empty for an empty one, and
     * for a minor in Brazil, since it follows what they subscribe to (26.4).
     */
    suspend fun forLibrary(): Result<List<DirectoryResult>> {
        val library = podcastRepository.getPodcasts().first().takeUnless { childSafety.state.value.isMinor }.orEmpty()
        val categories = library
            .flatMap { it.topCategories }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(CATEGORIES_FOR_YOU)
            .map { it.key }
        if (categories.isEmpty()) return Result.success(emptyList())
        val charts = categories.map { directory.top(it) }
        val turns = charts.map { it.getOrDefault(emptyList()) }
        val longest = turns.maxOfOrNull { it.size } ?: 0
        val alternated = (0 until longest).flatMap { i -> turns.mapNotNull { it.getOrNull(i) } }
        return recommending(listOf(if (charts.all { it.isFailure }) charts.first() else Result.success(alternated)))
    }

    /** One list from [sources], in order, each feed once, without what the library has or [excluded] keys. */
    private suspend fun recommending(
        sources: List<Result<List<DirectoryPodcast>>>,
        excluded: (Set<String>) -> Set<String> = { it },
    ): Result<List<DirectoryResult>> {
        if (sources.all { it.isFailure }) return Result.failure(sources.firstNotNullOf { it.exceptionOrNull() })
        val merged = Result.success(sources.flatMap { it.getOrDefault(emptyList()) })
        val skip = excluded(podcastRepository.getPodcasts().first().map { feedUrlKey(it.feedUrl) }.toSet())
        return merged.markingSubscribed().map { results ->
            results.filter { feedUrlKey(it.podcast.feedUrl) !in skip }.take(RECOMMENDATIONS)
        }
    }

    private fun showsExplicit() = preferences.showExplicit.value && !explicitContent.rule.value.hidesExplicit

    private suspend fun Result<List<DirectoryPodcast>>.markingSubscribed(): Result<List<DirectoryResult>> {
        val saved = podcastRepository.getPodcasts().first().map { feedUrlKey(it.feedUrl) }.toSet()
        // Apple sometimes lists one feed under two entries: one row each, or the list would show it twice.
        return map { found ->
            found.distinctBy { feedUrlKey(it.feedUrl) }
                .filter { (showsExplicit() || !it.explicit) && !explicitContent.rule.value.hides(it.feedUrl) }
                .map { DirectoryResult(it, isSubscribed = feedUrlKey(it.feedUrl) in saved) }
        }
    }
}

data class DirectoryResult(val podcast: DirectoryPodcast, val isSubscribed: Boolean)

/** Podcasts like the one titled [title] (19.6). */
data class Similar(val title: String, val results: Result<List<DirectoryResult>>)
