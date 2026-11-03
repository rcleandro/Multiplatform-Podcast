package br.com.carvalho.podcast.presentation.component

import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.extensions.toDuration
import br.com.carvalho.podcast.domain.model.Episode

private const val SECONDS_PER_MINUTE = 60

/** What is left of a started episode, in the same style as its length ("1h 5min"); null when not started or done. */
fun Episode.remainingDuration(): String? {
    val durationMs = duration * AppConfig.MILLIS_PER_SECOND
    if (isPlayed || playbackPosition <= 0 || durationMs <= 0) return null
    val leftSeconds = ((durationMs - playbackPosition) / AppConfig.MILLIS_PER_SECOND)
        .coerceAtLeast(SECONDS_PER_MINUTE.toLong())
    return leftSeconds.toDuration()
}
