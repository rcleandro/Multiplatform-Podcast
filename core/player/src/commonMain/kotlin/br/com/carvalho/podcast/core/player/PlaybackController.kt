package br.com.carvalho.podcast.core.player

import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.SleepTimer
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes

private const val TAG = "PlaybackController"

/**
 * The playback rules, once for every platform: queue, next and previous, end of episode, progress, saving and
 * restoring the session, sleep timer and speed. [PlatformPlayer] only plays what it is told. Lives as long as the
 * process; nothing releases it.
 */
@Suppress("TooManyFunctions") // one function per player command, plus the event handling
class PlaybackController(
    private val engine: PlatformPlayer,
    private val playerRepository: PlayerRepository,
    private val podcastRepository: PodcastRepository,
    private val episodeDownloader: EpisodeDownloader,
    private val dispatchers: CoroutineDispatchers,
    /** Where the progress loop and the sleep timer run; tests pass one they can end. */
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.main),
) {
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastSavedPosition: Long? = null

    init {
        engine.setListener { event -> scope.launch { onEvent(event) } }
        scope.launch {
            engine.awaitReady()
            restoreSession()
        }
    }

    suspend fun play(episode: Episode) {
        val playable = withLocalFile(episode)
        _playerState.update { it.copy(currentEpisode = playable, position = 0, duration = null, isBuffering = true) }
        lastSavedPosition = null
        withContext(dispatchers.main) {
            engine.load(playable, positionMs = 0, playWhenReady = true)
            engine.setSpeed(_playerState.value.speed)
        }
    }

    fun pause() {
        _playerState.update { it.copy(isPlaying = false) }
        onMain { engine.pause() }
    }

    fun resume() {
        if (_playerState.value.currentEpisode == null) return
        _playerState.update { it.copy(isPlaying = true) }
        onMain {
            engine.setSpeed(_playerState.value.speed)
            engine.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = _playerState.value.duration
        val target = if (duration != null) positionMs.coerceIn(0, duration) else positionMs.coerceAtLeast(0)
        _playerState.update { it.copy(position = target) }
        onMain { engine.seekTo(target) }
    }

    fun skipForward(seconds: Int) = seekTo(_playerState.value.position + seconds * AppConfig.MILLIS_PER_SECOND)

    fun skipBackward(seconds: Int) = seekTo(_playerState.value.position - seconds * AppConfig.MILLIS_PER_SECOND)

    /** Kept across pauses and episodes; the platforms used to reset it to 1x on pause. */
    fun setSpeed(speed: Float) {
        _playerState.update { it.copy(speed = speed) }
        onMain { engine.setSpeed(speed) }
    }

    fun setQueue(episodes: List<Episode>) {
        _playerState.update { it.copy(queue = episodes) }
    }

    fun playNext() {
        val next = neighbour(offset = 1)
        if (next == null) {
            pause()
            return
        }
        scope.launch { play(next) }
    }

    fun playPrevious() {
        val previous = neighbour(offset = -1)
        if (previous == null) seekTo(0) else scope.launch { play(previous) }
    }

    fun setSleepTimer(timer: SleepTimer?) {
        sleepTimerJob?.cancel()
        _playerState.update { it.copy(sleepTimer = timer, sleepTimerMillis = null) }
        if (timer !is SleepTimer.Minutes) return
        sleepTimerJob = scope.launch {
            // ponytail: counts down with delay() so tests drive it in virtual time; scheduling lag adds a few
            // milliseconds per tick (about 2 s over 30 min). Use a TimeSource if that ever matters.
            var remaining = timer.minutes.minutes.inWholeMilliseconds
            while (remaining > 0) {
                _playerState.update { it.copy(sleepTimerMillis = remaining) }
                val tick = minOf(AppConfig.SLEEP_TIMER_TICK_MS, remaining)
                delay(tick)
                remaining -= tick
            }
            AppLogger.i(TAG, "Sleep timer finished")
            _playerState.update { it.copy(sleepTimer = null, sleepTimerMillis = null) }
            pause()
        }
    }

    private suspend fun onEvent(event: PlatformEvent) {
        when (event) {
            is PlatformEvent.PlayingChanged -> onPlayingChanged(event.isPlaying)
            is PlatformEvent.BufferingChanged -> _playerState.update { it.copy(isBuffering = event.isBuffering) }
            PlatformEvent.Ended -> onEnded()
            is PlatformEvent.Remote -> onRemote(event.command)
        }
    }

    private suspend fun onPlayingChanged(isPlaying: Boolean) {
        _playerState.update { it.copy(isPlaying = isPlaying, isBuffering = if (isPlaying) false else it.isBuffering) }
        if (isPlaying) {
            startProgress()
        } else {
            progressJob?.cancel()
            readProgress()
            save()
        }
    }

    private fun onRemote(command: RemoteCommand) = when (command) {
        RemoteCommand.PLAY -> resume()
        RemoteCommand.PAUSE -> pause()
        RemoteCommand.SKIP_FORWARD -> skipForward(AppConfig.SKIP_FORWARD_SECONDS)
        RemoteCommand.SKIP_BACKWARD -> skipBackward(AppConfig.SKIP_BACKWARD_SECONDS)
        RemoteCommand.NEXT -> playNext()
        RemoteCommand.PREVIOUS -> playPrevious()
    }

    /** Marks the episode as played before anything else, then stops for the sleep timer or moves on. */
    private suspend fun onEnded() {
        progressJob?.cancel()
        val finished = _playerState.value.currentEpisode ?: return
        withContext(dispatchers.io) { podcastRepository.markEpisodeAsPlayed(finished.id) }
        if (_playerState.value.sleepTimer == SleepTimer.EndOfEpisode) {
            _playerState.update { it.copy(isPlaying = false, sleepTimer = null) }
            return
        }
        playNext()
    }

    private fun startProgress() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                readProgress()
                if (movedPastLastSave()) save()
                delay(PROGRESS_TICK_MS)
            }
        }
    }

    private fun readProgress() {
        _playerState.update { it.copy(position = engine.positionMs, duration = engine.durationMs ?: it.duration) }
    }

    private fun movedPastLastSave(): Boolean {
        val saved = lastSavedPosition ?: return true
        return abs(_playerState.value.position - saved) >= AppConfig.PLAYBACK_SAVE_INTERVAL_MS
    }

    /** The session (episode, position, speed, queue) and the episode's progress, or "played" near its end. */
    private suspend fun save() {
        val state = _playerState.value
        val episode = state.currentEpisode ?: return
        lastSavedPosition = state.position
        withContext(dispatchers.io) {
            playerRepository.savePlaybackState(episode.id, state.position, state.speed, state.queue)
            val duration = state.duration
            if (duration != null && duration > 0 && state.position > duration * AppConfig.PLAYBACK_FINISHED_THRESHOLD) {
                podcastRepository.markEpisodeAsPlayed(episode.id)
            } else if (state.position > 0) {
                podcastRepository.updateEpisodeProgress(episode.id, state.position)
            }
        }
    }

    private suspend fun restoreSession() {
        val saved = withContext(dispatchers.io) { playerRepository.getSavedPlaybackState() } ?: return
        val episode = saved.episodeId?.let { withContext(dispatchers.io) { podcastRepository.getEpisodeById(it) } }
        _playerState.update { it.copy(speed = saved.speed, queue = saved.queue) }
        engine.setSpeed(saved.speed)
        if (episode == null || _playerState.value.currentEpisode != null) return
        AppLogger.i(TAG, "Restoring ${episode.id} at ${saved.position} ms")
        val playable = withLocalFile(episode)
        _playerState.update { it.copy(currentEpisode = playable, position = saved.position) }
        lastSavedPosition = saved.position
        engine.load(playable, saved.position, playWhenReady = false)
    }

    /** Queue items and saved sessions do not carry the downloaded file; look it up every time. */
    private fun withLocalFile(episode: Episode) =
        episode.copy(localPath = episodeDownloader.getLocalPath(episode.id) ?: episode.localPath)

    private fun neighbour(offset: Int): Episode? {
        val state = _playerState.value
        val index = state.queue.indexOfFirst { it.id == state.currentEpisode?.id }
        if (index == -1) return null
        return state.queue.getOrNull(index + offset)
    }

    private fun onMain(block: () -> Unit) {
        scope.launch { block() }
    }

    private companion object {
        /** How often the position shown on screen is refreshed while playing. */
        const val PROGRESS_TICK_MS = 500L
    }
}
