package br.com.carvalho.podcast.presentation.util

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@Composable
fun formatPublishDate(timestamp: Long): String {
    if (timestamp <= 0) return stringResource(Res.string.date_some_days_ago)

    val now = getCurrentTimestamp()
    val diffSeconds = (now - timestamp) / 1000
    val diffMinutes = diffSeconds / 60
    val diffHours = diffMinutes / 60
    val diffDays = diffHours / 24

    return when {
        diffSeconds < 60 -> stringResource(Res.string.date_just_now)
        diffMinutes < 60 -> stringResource(Res.string.date_minutes_ago, diffMinutes.toInt())
        diffHours < 24 -> stringResource(Res.string.date_hours_ago, diffHours.toInt())
        diffDays == 1L -> stringResource(Res.string.date_yesterday)
        diffDays < 7 -> stringResource(Res.string.date_days_ago, diffDays.toInt())
        else -> {
            val instant = Instant.fromEpochMilliseconds(timestamp)
            val date = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "${date.day}/${date.month.number}/${date.year}"
        }
    }
}

@Composable
fun formatDuration(durationSeconds: Long): String {
    val h = durationSeconds / 3600
    val m = (durationSeconds % 3600) / 60
    return if (h > 0) {
        stringResource(Res.string.duration_h_m, h.toInt(), m.toInt())
    } else {
        stringResource(Res.string.duration_m, m.toInt())
    }
}
