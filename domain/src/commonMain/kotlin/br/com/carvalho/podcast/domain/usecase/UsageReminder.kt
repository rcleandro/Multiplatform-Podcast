package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.player.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private val DEFAULT_LIMIT = 1.hours
private val TICK = 1.minutes

/**
 * A break suggested to a minor in Brazil after [limit] of listening (26.5, ECA Digital art. 17): counted while
 * something plays, screen on or off; [due] stays up until the screen shows it and it is dismissed.
 */
// ponytail: counted in memory, so a closed app starts over; keep it in preferences if the count must survive.
class UsageReminder(
    player: AudioPlayer,
    childSafety: ChildSafety,
    scope: CoroutineScope,
    private val limit: Duration = DEFAULT_LIMIT,
    private val tick: Duration = TICK,
) {
    private val _due = MutableStateFlow(false)
    val due: StateFlow<Boolean> = _due.asStateFlow()

    private var heard = Duration.ZERO

    init {
        scope.launch {
            combine(
                player.playerState.map { it.isPlaying }.distinctUntilChanged(),
                childSafety.state.map { it.isMinor }.distinctUntilChanged(),
            ) { playing, minor -> playing && minor }
                .collectLatest { counting -> if (counting) count() }
        }
    }

    fun dismiss() {
        _due.value = false
    }

    private suspend fun count() {
        while (true) {
            delay(tick)
            heard += tick
            if (heard >= limit) {
                heard = Duration.ZERO
                _due.value = true
            }
        }
    }
}
