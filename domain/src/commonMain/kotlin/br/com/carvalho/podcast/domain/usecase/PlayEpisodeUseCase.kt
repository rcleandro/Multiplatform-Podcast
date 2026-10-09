package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.AudioPlayer

private const val TAG = "PlayEpisodeUseCase"

/** What every "play" button does, wherever it is. */
class PlayEpisodeUseCase(private val audioPlayer: AudioPlayer) {
    /**
     * Pauses or resumes [episode] if it is the current one. Otherwise plays it, and the queue the user arranged follows
     * it (18.6). The player finds the downloaded file itself.
     */
    suspend operator fun invoke(episode: Episode) {
        val state = audioPlayer.playerState.value
        if (state.currentEpisode?.id == episode.id) {
            if (state.isPlaying) audioPlayer.pause() else audioPlayer.resume()
            return
        }
        AppLogger.i(TAG, "Playing ${episode.id}")
        audioPlayer.play(episode)
    }
}
