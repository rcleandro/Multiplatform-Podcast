package br.com.carvalho.podcast.presentation.format

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RelativeTimeTest {
    // 2026-10-02T12:00:00Z
    private val now = 1_790_942_400_000L

    private fun ago(duration: Duration) = relativeTime(now - duration.inWholeMilliseconds, now, TimeZone.UTC)

    @Test
    fun missingDateHasNoText() {
        assertNull(relativeTime(0, now))
    }

    @Test
    fun picksTheUnitByAge() {
        assertEquals(RelativeTime.JustNow, ago(30.seconds))
        assertEquals(RelativeTime.Minutes(5), ago(5.minutes))
        assertEquals(RelativeTime.Hours(3), ago(3.hours))
        assertEquals(RelativeTime.Yesterday, ago(30.hours))
        assertEquals(RelativeTime.Days(4), ago(4.days))
    }

    @Test
    fun olderThanAWeekIsACalendarDate() {
        assertEquals(RelativeTime.Date(day = 22, month = 9, year = 2026), ago(10.days))
    }

    @Test
    fun futureDatesCountAsNow() {
        assertEquals(RelativeTime.JustNow, ago((-2).hours))
    }
}
