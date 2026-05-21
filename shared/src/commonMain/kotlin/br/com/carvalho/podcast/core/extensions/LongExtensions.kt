package br.com.carvalho.podcast.core.extensions

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val TAG = "LongExtensions"

fun Long.toDate(): String {
    if (this <= 0) return "Há alguns dias"

    return try {
        val now = getCurrentTimestamp()
        val diffSeconds = (now - this) / 1000
        val diffMinutes = diffSeconds / 60
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        when {
            diffSeconds < 60 -> "Agora"
            diffMinutes < 60 -> "Há $diffMinutes min"
            diffHours < 24 -> "Há $diffHours h"
            diffDays == 1L -> "Ontem"
            diffDays < 7 -> "Há $diffDays d"
            else -> {
                val instant = Instant.fromEpochMilliseconds(this)
                val date = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                "${date.day}/${date.month.number}/${date.year}"
            }
        }
    } catch (e: Exception) {
        AppLogger.e(TAG, "Failed to format date for timestamp=$this", e)
        "Há alguns dias"
    }
}

fun Long.toDuration(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}min"
}



fun Long.toTime(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}
