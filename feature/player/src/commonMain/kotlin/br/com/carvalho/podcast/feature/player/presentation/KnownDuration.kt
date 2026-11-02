package br.com.carvalho.podcast.feature.player.presentation

import br.com.carvalho.podcast.domain.model.PlayerState

private const val MS_PER_SECOND = 1000L

/** The player's duration once the audio loads; until then, the one the feed gave (seconds). 0 when neither knows. */
fun PlayerState.knownDurationMs(): Long =
    duration?.takeIf { it > 0 } ?: ((currentEpisode?.duration ?: 0L) * MS_PER_SECOND)
