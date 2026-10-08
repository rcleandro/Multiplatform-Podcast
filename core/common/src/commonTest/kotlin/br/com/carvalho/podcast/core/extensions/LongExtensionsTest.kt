package br.com.carvalho.podcast.core.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

class LongExtensionsTest {
    @Test
    fun durationShowsHoursOnlyWhenThereAreAnyAndAlwaysSaysMin() {
        assertEquals("45min", 2_700L.toDuration())
        assertEquals("1h 5min", 3_900L.toDuration())
        assertEquals("2h", 7_200L.toDuration())
    }

    @Test
    fun timePadsTheSecondsAndShowsHoursPastAnHour() {
        assertEquals("1:05", 65_000L.toTime())
        assertEquals("0:00", 0L.toTime())
        assertEquals("1:02:03", 3_723_000L.toTime())
        assertEquals("2:52:58", 10_378_000L.toTime())
    }
}
