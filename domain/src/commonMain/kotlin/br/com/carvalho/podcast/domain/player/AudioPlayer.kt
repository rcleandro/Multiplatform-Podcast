package br.com.carvalho.podcast.domain.player

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import kotlinx.coroutines.flow.StateFlow

/** The app's player. One instance for the whole process; restoring the last session is its job, not the screens'. */
interface AudioPlayer {
    val playerState: StateFlow<PlayerState>
    suspend fun play(episode: Episode)
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun skipForward(seconds: Int)
    fun skipBackward(seconds: Int)
    fun setQueue(episodes: List<Episode>)
    fun playNext()
    fun playPrevious()

    /** `null` cancels it. */
    fun setSleepTimer(timer: SleepTimer?)
}
