package br.com.carvalho.podcast.presentation.widget

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.model.SkipIntervals
import br.com.carvalho.podcast.domain.usecase.ExplicitRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Started episodes a widget shows under the player. */
private const val CONTINUE_LIMIT = 8

/**
 * What the iOS widget shows (25.12), as the Android PlayerWidget does (21.9): the episode playing, whether it plays,
 * the jumps chosen in Settings and the started episodes besides it, nothing explicit under the ECA Digital (26.3).
 */
data class PlayerWidgetState(
    val episode: Episode?,
    val isPlaying: Boolean,
    val skipIntervals: SkipIntervals,
    val continueListening: List<Episode>,
)

/** The [PlayerWidgetState] of [player] and the [started] episodes; the position, which ticks, is left out. */
fun playerWidgetState(
    player: Flow<PlayerState>,
    started: Flow<List<Episode>>,
    rule: Flow<ExplicitRule>,
): Flow<PlayerWidgetState> = combine(
    player.map { Triple(it.currentEpisode, it.isPlaying, it.skipIntervals) }.distinctUntilChanged(),
    combine(started, rule) { episodes, explicit -> episodes.filter(explicit::allows) },
) { (episode, isPlaying, skips), episodes ->
    PlayerWidgetState(
        episode = episode,
        isPlaying = isPlaying,
        skipIntervals = skips,
        continueListening = episodes.filter { it.id != episode?.id }.take(CONTINUE_LIMIT),
    )
}.distinctUntilChanged()
