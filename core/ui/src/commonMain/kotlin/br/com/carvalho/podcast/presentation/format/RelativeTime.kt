package br.com.carvalho.podcast.presentation.format

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** How long ago something was published, as a value; the screens turn it into words (`text()`). */
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

/**
 * Where an episode goes in a list divided by date. The windows count elapsed time, like [relativeTime], so an
 * episode that reads "6 days ago" sits under "Last 7 days".
 */
enum class DateGroup { LAST_24_HOURS, LAST_7_DAYS, LAST_30_DAYS, OLDER }

private const val MONTH_DAYS = 30

/** Without a date (timestamp 0 or negative) an episode goes with the oldest. */
fun dateGroup(timestampMs: Long, nowMs: Long): DateGroup {
    if (timestampMs <= 0) return DateGroup.OLDER
    val elapsed = (nowMs - timestampMs).coerceAtLeast(0).milliseconds
    return when {
        elapsed < 1.days -> DateGroup.LAST_24_HOURS
        elapsed < DAYS_SHOWN_AS_COUNT.days -> DateGroup.LAST_7_DAYS
        elapsed < MONTH_DAYS.days -> DateGroup.LAST_30_DAYS
        else -> DateGroup.OLDER
    }
}
