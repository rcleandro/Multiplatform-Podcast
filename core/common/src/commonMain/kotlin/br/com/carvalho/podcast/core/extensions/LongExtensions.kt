package br.com.carvalho.podcast.core.extensions

private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3600
private const val MILLIS_PER_SECOND = 1000

/** A length in seconds as "45min", "1h 5min" or "2h": the one style for episode lengths and what is left of them. */
fun Long.toDuration(): String {
    val h = this / SECONDS_PER_HOUR
    val m = (this % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    return when {
        h == 0L -> "${m}min"
        m == 0L -> "${h}h"
        else -> "${h}h ${m}min"
    }
}

/** A position in milliseconds as "1:05", or "1:02:03" past an hour. */
fun Long.toTime(): String {
    val totalSeconds = this / MILLIS_PER_SECOND
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = (totalSeconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$seconds" else "$minutes:$seconds"
}
