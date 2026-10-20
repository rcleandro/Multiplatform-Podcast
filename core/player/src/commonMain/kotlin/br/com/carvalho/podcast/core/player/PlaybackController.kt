package br.com.carvalho.podcast.core.player

import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.observability.Metrics
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.PodcastPlayback
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.player.ChapterSource
import br.com.carvalho.podcast.domain.player.SleepTimer
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.ExplicitRule
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
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val TAG = "PlaybackController"

/**
 * The playback rules, once for every platform: queue, next and previous, end of episode, progress, saving and
 * restoring the session, sleep timer and speed. [PlatformPlayer] only plays what it is told. Lives as long as the
 * process; nothing releases it.
 */
// One function per player command, plus the event handling; its collaborators, an end hook and the test scope.
@Suppress("TooManyFunctions", "LongParameterList")
class PlaybackController(
    private val engine: PlatformPlayer,
    private val playerRepository: PlayerRepository,
    private val podcastRepository: PodcastRepository,
    private val episodeDownloader: EpisodeDownloader,
    private val dispatchers: CoroutineDispatchers,
    /** Called when an episode ends, with the queue it was in: Settings may want the next one downloaded (18.5). */
    private val onFinished: suspend (finished: Episode, queue: List<Episode>) -> Unit = { _, _ -> },
    /** The jumps chosen in Settings (18.5), for the remote buttons and everything that reads the player's state. */
    private val skipIntervals: StateFlow<SkipIntervals> = MutableStateFlow(SkipIntervals()),
    /** Where the chapters of the episode loaded come from (19.1). */
    private val chapterSource: ChapterSource = ChapterSource { emptyList() },
    /** Where the progress loop and the sleep timer run; tests pass one they can end. */
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.main),
    /** The clock listening time is measured with (19.5); tests pass their virtual one. */
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
    /** What may play under Brazil's ECA Digital (26.3): nothing explicit for a minor or without a signal. */
    private val explicitRule: StateFlow<ExplicitRule> = MutableStateFlow(ExplicitRule()),
) : AudioPlayer {
    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var chaptersJob: Job? = null

    /** Heard since the last save (19.5): real time, and how far the episode moved in it. */
    private var heardMs = 0L
    private var movedMs = 0L

    /** When and where the progress was last read while playing; null while paused. */
    private var lastTick: Pair<ComparableTimeMark, Long>? = null

    /** What the platform was last told about chapters, so it hears again only when the chapter changes. */
    private var shownChapter: String? = null

    /** What is left of a timer of minutes; extending it adds here, while it runs. */
    private var sleepRemainingMs = 0L
    private var lastSavedPosition: Long? = null

    /** The speed saved with the session; a podcast with its own speed (18.14) plays at that one instead. */
    private var generalSpeed = PlayerState().speed

    /** How the current episode's podcast plays (18.14). */
    private var playback = PodcastPlayback()

    /** Set when an episode is asked to play, cleared by the first sound; feeds [Metrics.TIME_TO_AUDIO]. */
    private var playRequest: Pair<TimeMark, String>? = null

    init {
        engine.setListener { event -> scope.launch { onEvent(event) } }
        scope.launch {
            skipIntervals.collect { intervals ->
                _playerState.update { it.copy(skipIntervals = intervals) }
                engine.setSkipIntervals(intervals)
            }
        }
        scope.launch {
            engine.awaitReady()
            restoreSession()
        }
        // A signal that arrives while something explicit plays stops it.
        scope.launch {
            explicitRule.collect { rule ->
                val current = _playerState.value.currentEpisode
                if (current != null && !rule.allows(current) && _playerState.value.isPlaying) pause()
            }
        }
    }

    override suspend fun play(episode: Episode) {
        if (!explicitRule.value.allows(episode)) {
            AppLogger.i(TAG, "Not playing ${episode.id}: explicit, hidden by the age signal")
            return
        }
        // The time heard of the episode leaving goes to its own podcast.
        _playerState.value.currentEpisode?.let { withContext(dispatchers.io) { saveListening(it) } }
        val requested = TimeSource.Monotonic.markNow()
        val playable = withLocalFile(episode)
        playRequest = requested to if (playable.localPath != null) "local" else "stream"
        playback = podcastPlayback(episode)
        // An episode not begun skips its podcast's jingle (18.14); one already started resumes where it stopped.
        val start = startPosition(episode).takeIf { it > 0 } ?: introEnd(episode)
        val speed = playback.speed ?: generalSpeed
        // The queue holds what comes next: the episode played leaves it, wherever it was (18.6).
        _playerState.update { state ->
            state.copy(
                currentEpisode = playable,
                position = start,
                duration = null,
                isBuffering = true,
                speed = speed,
                queue = state.queue.filter { it.id != episode.id },
                chapters = emptyList(),
            )
        }
        lastSavedPosition = null
        // Started: no longer waiting in "New" (18.7), and at the top of "History" (18.12).
        runCatching { podcastRepository.markEpisodeStarted(episode.id, getCurrentTimestamp()) }
        withContext(dispatchers.main) {
            engine.load(playable, positionMs = start, playWhenReady = true)
            engine.setSpeed(speed)
        }
        loadChapters(playable)
    }

    override fun pause() {
        _playerState.update { it.copy(isPlaying = false) }
        onMain { engine.pause() }
    }

    override fun resume() {
        val current = _playerState.value.currentEpisode ?: return
        if (!explicitRule.value.allows(current)) return
        _playerState.update { it.copy(isPlaying = true) }
        onMain {
            engine.setSpeed(_playerState.value.speed)
            engine.play()
        }
    }

    override fun seekTo(positionMs: Long) {
        val duration = _playerState.value.duration
        val target = if (duration != null) positionMs.coerceIn(0, duration) else positionMs.coerceAtLeast(0)
        _playerState.update { it.copy(position = target) }
        onMain { engine.seekTo(target) }
        showChapter()
    }

    override fun skipForward(seconds: Int) = seekBy(seconds * AppConfig.MILLIS_PER_SECOND)

    override fun skipBackward(seconds: Int) = seekBy(-seconds * AppConfig.MILLIS_PER_SECOND)

    private fun seekBy(deltaMs: Long) = seekTo(_playerState.value.position + deltaMs)

    /** Kept across pauses and episodes; the platforms used to reset it to 1x on pause. */
    override fun setSpeed(speed: Float) {
        // Changed while a podcast with its own speed plays, it lasts that episode; Settings keep the podcast's.
        if (playback.speed == null) generalSpeed = speed
        _playerState.update { it.copy(speed = speed) }
        onMain { engine.setSpeed(speed) }
    }

    /** Saved at once: a queue arranged while paused survives the app closing. */
    override fun setQueue(episodes: List<Episode>) {
        val current = _playerState.value.currentEpisode?.id
        _playerState.update {
            it.copy(
                queue = episodes.filter { episode -> episode.id != current },
                queueUpdatedAt = getCurrentTimestamp(),
            )
        }
        // ponytail: with nothing playing there is no session row to save the queue in; it lives until the app closes.
        scope.launch { save() }
    }

    override fun playNext() {
        val next = _playerState.value.queue.firstOrNull { explicitRule.value.allows(it) }
        if (next == null) {
            pause()
            return
        }
        scope.launch { play(next) }
    }

    /** A finished episode leaves the queue, so there is nothing before the current one: it starts over. */
    override fun playPrevious() = seekTo(0)

    override fun setSleepTimer(timer: SleepTimer?) {
        sleepTimerJob?.cancel()
        _playerState.update { it.copy(sleepTimer = timer, sleepTimerMillis = null) }
        onMain { engine.setVolume(FULL_VOLUME) }
        if (timer !is SleepTimer.Minutes) return
        sleepRemainingMs = timer.minutes.minutes.inWholeMilliseconds
        sleepTimerJob = scope.launch {
            // ponytail: counts down with delay() so tests drive it in virtual time; scheduling lag adds a few
            // milliseconds per tick (about 2 s over 30 min). Use a TimeSource if that ever matters.
            while (sleepRemainingMs > 0) {
                showSleepRemaining()
                val tick = minOf(AppConfig.SLEEP_TIMER_TICK_MS, sleepRemainingMs)
                delay(tick)
                sleepRemainingMs -= tick
            }
            AppLogger.i(TAG, "Sleep timer finished")
            _playerState.update { it.copy(sleepTimer = null, sleepTimerMillis = null) }
            pause()
            // Paused first, so the next play is heard at full volume and nothing jumps back up before the pause.
            onMain { engine.setVolume(FULL_VOLUME) }
        }
    }

    override fun extendSleepTimer(minutes: Int) {
        if (_playerState.value.sleepTimer !is SleepTimer.Minutes || sleepRemainingMs <= 0) return
        sleepRemainingMs += minutes.minutes.inWholeMilliseconds
        showSleepRemaining()
    }

    /** The time left on screen, and the volume going down with it over the last minute (18.15). */
    private fun showSleepRemaining() {
        val remaining = sleepRemainingMs
        _playerState.update { it.copy(sleepTimerMillis = remaining) }
        val volume = (remaining.toFloat() / SLEEP_FADE_MS).coerceAtMost(FULL_VOLUME)
        onMain { engine.setVolume(volume) }
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
            playRequest?.let { (requested, source) ->
                Metrics.record(Metrics.TIME_TO_AUDIO, requested.elapsedNow(), "source" to source)
            }
            playRequest = null
            startProgress()
        } else {
            progressJob?.cancel()
            readProgress()
            countListening()
            lastTick = null
            save()
        }
    }

    private fun onRemote(command: RemoteCommand) = when (command) {
        RemoteCommand.PLAY -> resume()
        RemoteCommand.PAUSE -> pause()
        RemoteCommand.SKIP_FORWARD -> skipForward(skipIntervals.value.forwardSeconds)
        RemoteCommand.SKIP_BACKWARD -> skipBackward(skipIntervals.value.backwardSeconds)
        RemoteCommand.NEXT -> playNext()
        RemoteCommand.PREVIOUS -> playPrevious()
    }

    /** Marks the episode as played before anything else, then stops for the sleep timer or moves on. */
    private suspend fun onEnded() {
        progressJob?.cancel()
        val finished = _playerState.value.currentEpisode ?: return
        withContext(dispatchers.io) {
            podcastRepository.markEpisodeAsPlayed(finished.id)
            onFinished(finished, _playerState.value.queue)
        }
        if (_playerState.value.sleepTimer == SleepTimer.EndOfEpisode) {
            _playerState.update { it.copy(isPlaying = false, sleepTimer = null) }
            return
        }
        playNext()
    }

    private fun startProgress() {
        progressJob?.cancel()
        lastTick = null
        progressJob = scope.launch {
            while (isActive) {
                readProgress()
                countListening()
                if (reachedOutro()) {
                    // Its own coroutine: ending cancels this loop.
                    scope.launch {
                        pause()
                        onEnded()
                    }
                    break
                }
                if (movedPastLastSave()) save()
                delay(PROGRESS_TICK_MS)
            }
        }
    }

    private fun readProgress() {
        _playerState.update { it.copy(position = engine.positionMs, duration = engine.durationMs ?: it.duration) }
        showChapter()
    }

    /** Looked up after the episode starts loading, so the sound never waits for them. */
    private fun loadChapters(episode: Episode) {
        chaptersJob?.cancel()
        shownChapter = null
        chaptersJob = scope.launch {
            val chapters = withContext(dispatchers.io) { chapterSource.chapters(episode) }
            if (_playerState.value.currentEpisode?.id != episode.id) return@launch
            _playerState.update { it.copy(chapters = chapters) }
            showChapter()
        }
    }

    private fun showChapter() {
        val title = _playerState.value.currentChapter?.title?.ifBlank { null }
        if (title == shownChapter) return
        shownChapter = title
        onMain { engine.showChapter(title) }
    }

    /**
     * Adds the time since the last tick to what was heard, with how far the episode moved in it; a move backwards or
     * faster than any speed plays is a seek, and does not count.
     */
    private fun countListening() {
        val now = timeSource.markNow()
        val position = _playerState.value.position
        lastTick?.let { (then, from) ->
            val elapsed = (now - then).inWholeMilliseconds
            val moved = position - from
            if (moved in 0..elapsed * MAX_MOVE_PER_MS) {
                heardMs += elapsed
                movedMs += moved
            }
        }
        lastTick = now to position
    }

    /** What was heard of [episode] since the last save, to its podcast's totals. */
    private suspend fun saveListening(episode: Episode) {
        if (heardMs <= 0) return
        val heard = heardMs
        val saved = movedMs - heardMs
        heardMs = 0
        movedMs = 0
        runCatching { playerRepository.addListening(episode.podcastId, getCurrentTimestamp(), heard, saved) }
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
            saveListening(episode)
            playerRepository.savePlaybackState(
                episode.id,
                state.position,
                generalSpeed,
                state.queue,
                state.queueUpdatedAt,
            )
            val duration = state.duration
            if (duration != null && duration > 0 && state.position > duration * AppConfig.PLAYBACK_FINISHED_THRESHOLD) {
                podcastRepository.markEpisodeAsPlayed(episode.id)
            } else if (state.position > 0) {
                podcastRepository.updateEpisodeProgress(episode.id, state.position)
            }
        }
    }

    /**
     * Once per process: the saved queue and speed, and the saved episode loaded paused where it stopped. If the
     * platform is already playing something (Android's service outlived the screens), that episode is adopted as it is.
     */
    private suspend fun restoreSession() {
        val saved = withContext(dispatchers.io) { playerRepository.getSavedPlaybackState() }
        val adopted = engine.loadedEpisodeId
        val episodeId = adopted ?: saved?.episodeId
        val episode = episodeId?.let { withContext(dispatchers.io) { podcastRepository.getEpisodeById(it) } }
        playback = episode?.let { podcastPlayback(it) } ?: PodcastPlayback()
        if (saved != null) {
            // Before 18.6 the queue also held the current episode and those before it: only what follows it is next.
            val next = saved.queue.indexOfFirst { it.id == saved.episodeId }.let { saved.queue.drop(it + 1) }
            generalSpeed = saved.speed
            val speed = playback.speed ?: saved.speed
            _playerState.update { it.copy(speed = speed, queue = next, queueUpdatedAt = saved.queueUpdatedAt) }
            if (adopted == null) engine.setSpeed(speed)
        }
        if (episode == null || _playerState.value.currentEpisode != null) return
        val playable = withLocalFile(episode)
        if (adopted != null) {
            AppLogger.i(TAG, "Adopting ${episode.id}, already loaded by the platform")
            _playerState.update { it.copy(currentEpisode = playable) }
            readProgress()
            loadChapters(playable)
            return
        }
        val position = saved?.position ?: 0
        AppLogger.i(TAG, "Restoring ${episode.id} at $position ms")
        _playerState.update { it.copy(currentEpisode = playable, position = position) }
        lastSavedPosition = position
        engine.load(playable, position, playWhenReady = false)
        loadChapters(playable)
    }

    /**
     * Where a started episode stopped; a heard one starts over. Read from the database: the queue and the screens may
     * hold the episode as it was before it was last heard.
     */
    private suspend fun startPosition(episode: Episode): Long {
        val stored = runCatching { podcastRepository.getEpisodeById(episode.id) }.getOrNull() ?: episode
        return if (stored.isPlayed) 0 else stored.playbackPosition
    }

    private suspend fun podcastPlayback(episode: Episode): PodcastPlayback =
        runCatching { podcastRepository.getPodcastById(episode.podcastId)?.playback }.getOrNull() ?: PodcastPlayback()

    /** Where the podcast's jingle ends, unless that is the whole episode. */
    private fun introEnd(episode: Episode): Long {
        val intro = playback.skipIntroSeconds.toLong()
        return if (intro > 0 && (episode.duration <= 0 || intro < episode.duration)) {
            intro * AppConfig.MILLIS_PER_SECOND
        } else {
            0
        }
    }

    /** Into the podcast's closing credits (18.14), which count as the end of the episode. */
    private fun reachedOutro(): Boolean {
        val outroMs = playback.skipOutroSeconds * AppConfig.MILLIS_PER_SECOND
        val state = _playerState.value
        val duration = state.duration ?: 0
        return outroMs > 0 && duration > outroMs && state.position >= duration - outroMs
    }

    /** Queue items and saved sessions do not carry the downloaded file; look it up every time. */
    private suspend fun withLocalFile(episode: Episode) =
        episode.copy(localPath = episodeDownloader.getLocalPath(episode.id) ?: episode.localPath)

    private fun onMain(block: () -> Unit) {
        scope.launch { block() }
    }

    private companion object {
        /** How often the position shown on screen is refreshed while playing. */
        const val PROGRESS_TICK_MS = 500L

        /** The sleep timer's last minute, over which the volume goes down to nothing. */
        const val SLEEP_FADE_MS = 60_000L
        const val FULL_VOLUME = 1f

        /** Faster than the fastest speed with silences skipped: a move beyond this is a seek. */
        const val MAX_MOVE_PER_MS = 4
    }
}
