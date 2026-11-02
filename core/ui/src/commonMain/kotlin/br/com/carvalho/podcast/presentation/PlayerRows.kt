package br.com.carvalho.podcast.presentation

import br.com.carvalho.podcast.domain.model.PlayerState

/**
 * The player as an episode list needs it, to mark the row that plays: the position, which moves twice a second, and
 * everything else the rows never show are left at their defaults (23.3).
 */
fun PlayerState.forRows(): PlayerState =
    PlayerState(currentEpisode = currentEpisode, isPlaying = isPlaying, isBuffering = isBuffering)
