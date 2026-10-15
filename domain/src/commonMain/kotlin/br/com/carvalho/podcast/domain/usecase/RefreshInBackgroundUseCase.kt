package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first

/** What a look for new episodes in the background did (18.18): whether every feed failed, and what to tell. */
data class BackgroundRefresh(val allFailed: Boolean, val toNotify: List<Episode>)

/**
 * Looks at every feed, as pulling the library down does, and returns the episodes that arrived in "New" with it, less
 * those of podcasts whose notifications are off, or all of them when Settings turn notifications off.
 */
class RefreshInBackgroundUseCase(
    private val refresh: RefreshPodcastUseCase,
    private val repository: PodcastRepository,
    private val preferences: PreferencesRepository,
) {
    suspend operator fun invoke(): BackgroundRefresh {
        val before = repository.getNewEpisodes().map { it.id }.toSet()
        val summary = refresh.refreshAll()
        val toNotify = if (preferences.notifyNewEpisodes.value) {
            val quiet = repository.getPodcasts().first().filterNot { it.notifyNewEpisodes }.map { it.id }.toSet()
            repository.getNewEpisodes().filter { it.id !in before && it.podcastId !in quiet }
        } else {
            emptyList()
        }
        return BackgroundRefresh(summary.allFailed, toNotify)
    }
}
