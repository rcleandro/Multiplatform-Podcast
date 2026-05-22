package br.com.carvalho.podcast.feature.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.domain.player.SKIP_BACKWARD_SECONDS
import br.com.carvalho.podcast.domain.player.SKIP_FORWARD_SECONDS
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

private const val TAG = "PlayerViewModel"
@OptIn(FlowPreview::class)
class PlayerViewModel(
    private val audioPlayer: AudioPlayer,
    private val playerRepository: PlayerRepository,
    private val podcastRepository: PodcastRepository,
    private val episodeDownloader: EpisodeDownloader,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = audioPlayer.playerState
    private var sleepTimerJob: Job? = null

    private var restored = false

    init {
        viewModelScope.launch(dispatchers.io) {
            audioPlayer.isReady
                .filter { it }
                .collect {
                    if (restored) {
                        AppLogger.d(TAG, "isReady re-emitted but already restored, ignoring")
                        return@collect
                    }
                    restored = true

                    AppLogger.d(TAG, "Restoring playback state...")
                    playerRepository.getSavedPlaybackState()?.let { saved ->
                        saved.episodeId?.let { id ->
                            val episode = podcastRepository.getEpisodeById(id)
                            if (episode != null) {
                                val resolvedEpisode = episode.copy(localPath = episodeDownloader.getLocalPath(episode.id))
                                AppLogger.i(TAG, "Restoring episode: ${resolvedEpisode.title} at ${saved.position}ms")
                                audioPlayer.setQueue(saved.queue)
                                audioPlayer.prepare(resolvedEpisode, saved.position)
                            }
                        }
                        audioPlayer.setSpeed(saved.speed)
                    }
                }
        }

        playerState
            .filter { it.isPlaying }
            .debounce(2000)
            .onEach { saveState(it) }
            .launchIn(viewModelScope)

        playerState
            .filter { !it.isPlaying && it.currentEpisode != null }
            .distinctUntilChanged { old, new -> old.position == new.position }
            .onEach { saveState(it) }
            .launchIn(viewModelScope)
    }

    private suspend fun saveState(state: PlayerState) = withContext(dispatchers.io) {
        val episode = state.currentEpisode ?: return@withContext
        playerRepository.savePlaybackState(
            episodeId = episode.id,
            position = state.position,
            speed = state.speed,
            queue = state.queue
        )
        val duration = state.duration
        if (duration != null && duration > 0 && state.position > duration * 0.95) {
            podcastRepository.markEpisodeAsPlayed(episode.id)
        } else if (state.position > 0) {
            podcastRepository.updateEpisodeProgress(episode.id, state.position)
        }
    }

    fun play(episode: Episode) = viewModelScope.launch(dispatchers.io) {
        val resolvedEpisode = episode.copy(localPath = episodeDownloader.getLocalPath(episode.id))
        audioPlayer.play(resolvedEpisode)
    }

    fun pause() = audioPlayer.pause()

    fun resume() = audioPlayer.resume()

    fun seekTo(positionMs: Long) = audioPlayer.seekTo(positionMs)

    fun skipForward() = audioPlayer.skipForward(seconds = SKIP_FORWARD_SECONDS)

    fun skipBackward() = audioPlayer.skipBackward(seconds = SKIP_BACKWARD_SECONDS)

    fun setSpeed(speed: Float) = audioPlayer.setSpeed(speed)

    fun playNext() = audioPlayer.playNext()

    fun playPrevious() = audioPlayer.playPrevious()

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null) {
            AppLogger.i(TAG, "Sleep timer cancelled")
            audioPlayer.setSleepTimer(null, null)
            return
        }

        AppLogger.i(TAG, "Setting sleep timer for $minutes minutes")
        val totalDuration = minutes.minutes
        val timeSource = TimeSource.Monotonic
        val mark = timeSource.markNow()

        sleepTimerJob = viewModelScope.launch(dispatchers.io) {
            var remaining = totalDuration
            while (remaining.isPositive()) {
                audioPlayer.setSleepTimer(remaining.inWholeMilliseconds, minutes)
                delay(1000)
                remaining = totalDuration - mark.elapsedNow()
            }
            AppLogger.i(TAG, "Sleep timer finished. Pausing playback.")
            audioPlayer.setSleepTimer(null, null)
            pause()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        audioPlayer.release()
    }
}
