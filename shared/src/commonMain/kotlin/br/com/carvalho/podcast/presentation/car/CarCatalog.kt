package br.com.carvalho.podcast.presentation.car

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.ExplicitContent
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import kotlinx.coroutines.flow.first

/**
 * What CarPlay lists (25.13), as Android Auto's MediaTree does (13.5): the library's podcasts, a podcast's episodes
 * and the queue. Nothing explicit or hidden after a report under Brazil's ECA Digital (26.3, 26.7).
 */
class CarCatalog(
    private val repository: PodcastRepository,
    private val player: AudioPlayer,
    private val explicitContent: ExplicitContent = ExplicitContent(),
) {
    suspend fun podcasts(): List<Podcast> {
        val rule = explicitContent.rule.value
        return repository.getPodcasts().first().filter(rule::allows)
    }

    suspend fun episodes(podcastId: String): List<Episode> {
        val rule = explicitContent.rule.value
        return repository.getEpisodes(podcastId).first().filter(rule::allows)
    }

    fun queue(): List<Episode> = player.playerState.value.queue.filter(explicitContent.rule.value::allows)

    /** Plays [episodeId], or pauses and resumes it when it is the one playing. */
    suspend fun play(episodeId: String) {
        repository.getEpisodeById(episodeId)?.let { PlayEpisodeUseCase(player)(it) }
    }
}
