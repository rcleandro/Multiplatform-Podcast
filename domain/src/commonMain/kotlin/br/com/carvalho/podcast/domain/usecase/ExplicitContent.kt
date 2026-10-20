package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * What Brazil's ECA Digital lets show and play (26.3, ADR 0012): with a minor's signal or none, nothing explicit,
 * whether the episode or its whole podcast is marked. Elsewhere, and for an adult, everything.
 */
data class ExplicitRule(val hidesExplicit: Boolean = false, val explicitPodcasts: Set<String> = emptySet()) {
    fun allows(podcast: Podcast): Boolean = !hidesExplicit || !podcast.explicit

    fun allows(episode: Episode): Boolean =
        !hidesExplicit || (!episode.explicit && episode.podcastId !in explicitPodcasts)
}

/**
 * The rule as it stands, for lists and the player alike. Only the screens and the player read it: backup, sync, OPML
 * and downloads keep seeing everything, so nothing hidden is lost.
 */
class ExplicitContent(val rule: StateFlow<ExplicitRule> = MutableStateFlow(ExplicitRule())) {
    companion object {
        /** Follows the age signal and the library's explicit podcasts for as long as [scope] lives. */
        fun following(childSafety: ChildSafety, podcastRepository: PodcastRepository, scope: CoroutineScope) =
            ExplicitContent(
                combine(childSafety.state, podcastRepository.getPodcasts()) { safety, podcasts ->
                    ExplicitRule(safety.hidesExplicit, podcasts.filter { it.explicit }.map { it.id }.toSet())
                }.stateIn(scope, SharingStarted.Eagerly, ExplicitRule(childSafety.state.value.hidesExplicit))
            )
    }
}
