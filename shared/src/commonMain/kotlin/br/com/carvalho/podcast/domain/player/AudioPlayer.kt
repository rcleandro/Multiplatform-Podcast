package br.com.carvalho.podcast.domain.player

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import kotlinx.coroutines.flow.StateFlow

const val SKIP_FORWARD_SECONDS = 30
const val SKIP_BACKWARD_SECONDS = 10

interface AudioPlayer {
    val playerState: StateFlow<PlayerState>
    val isReady: StateFlow<Boolean>
    suspend fun play(episode: Episode)
    fun prepare(episode: Episode, positionMs: Long)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun skipForward(seconds: Int)
    fun skipBackward(seconds: Int)
    fun setSleepTimer(millis: Long?, selectedMinutes: Int?)
    fun setQueue(episodes: List<Episode>)
    fun playNext()
    fun playPrevious()
    fun release()
}

expect fun createAudioPlayer(): AudioPlayer
