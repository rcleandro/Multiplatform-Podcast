package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * What Brazil's ECA Digital lets show and play (26.3, ADR 0012): with a minor's signal or none, nothing explicit,
 * whether the episode or its whole podcast is marked. Elsewhere, and for an adult, everything but what was hidden
 * after a report (26.7), for anyone.
 */
data class ExplicitRule(
    val hidesExplicit: Boolean = false,
    val explicitPodcasts: Set<String> = emptySet(),
    val hiddenPodcasts: Set<String> = emptySet(),
    val hiddenEpisodes: Set<String> = emptySet(),
) {
    fun allows(podcast: Podcast): Boolean = podcast.id !in hiddenPodcasts && (!hidesExplicit || !podcast.explicit)

    fun allows(episode: Episode): Boolean =
        episode.id !in hiddenEpisodes && episode.podcastId !in hiddenPodcasts &&
            (!hidesExplicit || (!episode.explicit && episode.podcastId !in explicitPodcasts))

    /** A directory entry, by its feed: the library's podcast id is its feed address. */
    fun hides(feedUrl: String): Boolean = feedUrl in hiddenPodcasts
}

/**
 * The rule as it stands, for lists and the player alike. Only the screens and the player read it: backup, sync, OPML
 * and downloads keep seeing everything, so nothing hidden is lost.
 */
class ExplicitContent(val rule: StateFlow<ExplicitRule> = MutableStateFlow(ExplicitRule())) {
    companion object {
        /** Follows the age signal, the library's explicit podcasts and what was hidden, while [scope] lives. */
        fun following(
            childSafety: ChildSafety,
            podcastRepository: PodcastRepository,
            preferences: PreferencesRepository,
            scope: CoroutineScope,
        ) = ExplicitContent(
            combine(
                childSafety.state,
                podcastRepository.getPodcasts(),
                preferences.hiddenPodcasts,
                preferences.hiddenEpisodes,
            ) { safety, podcasts, hiddenPodcasts, hiddenEpisodes ->
                ExplicitRule(
                    safety.hidesExplicit,
                    podcasts.filter { it.explicit }.map { it.id }.toSet(),
                    hiddenPodcasts,
                    hiddenEpisodes,
                )
            }.stateIn(
                scope,
                SharingStarted.Eagerly,
                ExplicitRule(
                    childSafety.state.value.hidesExplicit,
                    hiddenPodcasts = preferences.hiddenPodcasts.value,
                    hiddenEpisodes = preferences.hiddenEpisodes.value,
                ),
            )
        )
    }
}
