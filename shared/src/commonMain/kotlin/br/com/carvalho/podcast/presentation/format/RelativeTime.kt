package br.com.carvalho.podcast.presentation.format

import androidx.compose.runtime.Composable
import br.com.carvalho.podcast.shared.Res
import br.com.carvalho.podcast.shared.date_short
import br.com.carvalho.podcast.shared.time_days_ago
import br.com.carvalho.podcast.shared.time_hours_ago
import br.com.carvalho.podcast.shared.time_just_now
import br.com.carvalho.podcast.shared.time_minutes_ago
import br.com.carvalho.podcast.shared.time_yesterday
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** How long ago something was published, as a value; [text] turns it into words. */
sealed interface RelativeTime {
    data object JustNow : RelativeTime
    data class Minutes(val count: Int) : RelativeTime
    data class Hours(val count: Int) : RelativeTime
    data object Yesterday : RelativeTime
    data class Days(val count: Int) : RelativeTime
    data class Date(val day: Int, val month: Int, val year: Int) : RelativeTime
}

private const val DAYS_SHOWN_AS_COUNT = 7

/** Null when the feed gave no date (timestamp 0 or negative). */
fun relativeTime(timestampMs: Long, nowMs: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): RelativeTime? {
    if (timestampMs <= 0) return null
    val elapsed = (nowMs - timestampMs).coerceAtLeast(0).milliseconds
    return when {
        elapsed < 1.minutes -> RelativeTime.JustNow
        elapsed < 1.hours -> RelativeTime.Minutes(elapsed.inWholeMinutes.toInt())
        elapsed < 1.days -> RelativeTime.Hours(elapsed.inWholeHours.toInt())
        elapsed < 2.days -> RelativeTime.Yesterday
        elapsed < DAYS_SHOWN_AS_COUNT.days -> RelativeTime.Days(elapsed.inWholeDays.toInt())
        else -> Instant.fromEpochMilliseconds(timestampMs).toLocalDateTime(timeZone).let {
            RelativeTime.Date(it.day, it.month.number, it.year)
        }
    }
}

@Composable
fun RelativeTime.text(): String = when (this) {
    RelativeTime.JustNow -> stringResource(Res.string.time_just_now)
    is RelativeTime.Minutes -> pluralStringResource(Res.plurals.time_minutes_ago, count, count)
    is RelativeTime.Hours -> pluralStringResource(Res.plurals.time_hours_ago, count, count)
    RelativeTime.Yesterday -> stringResource(Res.string.time_yesterday)
    is RelativeTime.Days -> pluralStringResource(Res.plurals.time_days_ago, count, count)
    is RelativeTime.Date -> stringResource(Res.string.date_short, day, month, year)
}
