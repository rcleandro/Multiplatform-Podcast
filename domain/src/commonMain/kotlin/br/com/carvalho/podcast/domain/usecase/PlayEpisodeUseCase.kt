package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository

private const val TAG = "PlayEpisodeUseCase"

/** What every "play" button does, wherever it is. */
class PlayEpisodeUseCase(
    private val audioPlayer: AudioPlayer,
    private val episodeDownloader: EpisodeDownloader,
    private val podcastRepository: PodcastRepository,
) {
    /**
     * Pauses or resumes [episode] if it is the current one. Otherwise plays it from its downloaded file when there is
     * one, followed by [queue]; without a queue, by the newer episodes of its podcast.
     */
    suspend operator fun invoke(episode: Episode, queue: List<Episode>? = null) {
        val state = audioPlayer.playerState.value
        if (state.currentEpisode?.id == episode.id) {
            if (state.isPlaying) audioPlayer.pause() else audioPlayer.resume()
            return
        }
        val playing = episode.copy(localPath = episodeDownloader.getLocalPath(episode.id))
        AppLogger.i(TAG, "Playing ${episode.id} (downloaded: ${playing.localPath != null})")
        audioPlayer.setQueue(queue ?: newerEpisodesFrom(episode))
        audioPlayer.play(playing)
    }

    private suspend fun newerEpisodesFrom(episode: Episode): List<Episode> =
        podcastRepository.getEpisodesSince(episode.podcastId, episode.publishDate)
            .dropWhile { it.id != episode.id }
            .ifEmpty { listOf(episode) }
}
