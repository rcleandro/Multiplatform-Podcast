package br.com.carvalho.podcast.domain.model

import kotlinx.serialization.Serializable

/**
 * How one podcast plays (18.14): its own speed, or the general one when null, and the seconds skipped at the start of
 * an episode not yet begun (a jingle) and at its end (the credits).
 */
@Serializable
data class PodcastPlayback(
    val speed: Float? = null,
    val skipIntroSeconds: Int = 0,
    val skipOutroSeconds: Int = 0,
)
