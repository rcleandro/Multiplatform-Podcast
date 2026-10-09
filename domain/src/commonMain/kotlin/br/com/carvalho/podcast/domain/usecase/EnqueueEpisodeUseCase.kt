package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.AudioPlayer

/** What "Play next" or "Add to queue" did, for the message the screen shows. */
enum class Enqueued { NEXT, LAST, PLAYING, ALREADY_PLAYING }

/**
 * "Play next" and "Add to queue" from any episode (18.6). An episode already queued moves instead of showing twice.
 * With nothing playing there is no mini player to reach the queue from, so the episode plays at once.
 */
class EnqueueEpisodeUseCase(private val audioPlayer: AudioPlayer) {
    suspend operator fun invoke(episode: Episode, next: Boolean): Enqueued {
        val state = audioPlayer.playerState.value
        val current = state.currentEpisode
        return when {
            current == null -> {
                audioPlayer.play(episode)
                Enqueued.PLAYING
            }
            current.id == episode.id -> Enqueued.ALREADY_PLAYING
            else -> {
                val others = state.queue.filter { it.id != episode.id }
                audioPlayer.setQueue(if (next) listOf(episode) + others else others + episode)
                if (next) Enqueued.NEXT else Enqueued.LAST
            }
        }
    }
}
