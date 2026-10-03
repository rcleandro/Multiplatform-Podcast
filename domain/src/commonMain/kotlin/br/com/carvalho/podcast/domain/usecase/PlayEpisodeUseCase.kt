package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository

private const val TAG = "PlayEpisodeUseCase"

/** What every "play" button does, wherever it is. */
class PlayEpisodeUseCase(
    private val audioPlayer: AudioPlayer,
    private val podcastRepository: PodcastRepository,
) {
    /**
     * Pauses or resumes [episode] if it is the current one. Otherwise plays it followed by [queue]; without a queue,
     * by the newer episodes of its podcast. The player finds the downloaded file itself.
     */
    suspend operator fun invoke(episode: Episode, queue: List<Episode>? = null) {
        val state = audioPlayer.playerState.value
        if (state.currentEpisode?.id == episode.id) {
            if (state.isPlaying) audioPlayer.pause() else audioPlayer.resume()
            return
        }
        AppLogger.i(TAG, "Playing ${episode.id}")
        audioPlayer.setQueue(queue ?: newerEpisodesFrom(episode))
        audioPlayer.play(episode)
    }

    private suspend fun newerEpisodesFrom(episode: Episode): List<Episode> =
        podcastRepository.getEpisodesSince(episode.podcastId, episode.publishDate)
            .dropWhile { it.id != episode.id }
            .ifEmpty { listOf(episode) }
}
