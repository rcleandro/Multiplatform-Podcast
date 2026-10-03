package br.com.carvalho.podcast.core.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

class LongExtensionsTest {
    @Test
    fun durationShowsHoursOnlyWhenThereAreAny() {
        assertEquals("45min", 2_700L.toDuration())
        assertEquals("1h 5m", 3_900L.toDuration())
    }

    @Test
    fun timePadsTheSeconds() {
        assertEquals("1:05", 65_000L.toTime())
        assertEquals("0:00", 0L.toTime())
    }
}
