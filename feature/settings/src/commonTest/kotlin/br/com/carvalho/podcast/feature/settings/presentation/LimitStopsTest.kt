package br.com.carvalho.podcast.feature.settings.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class LimitStopsTest {
    @Test
    fun smallSizesGoGigabyteByGigabyteUpToWhatFits() {
        assertEquals(listOf(HALF_GB) + (1L..10L).map { it * GB } + null, limitStops(capacity = 12 * GB, current = null))
    }

    @Test
    fun largerSizesTakeLongerSteps() {
        // 128 GB free, as on the Razr: then 15 to 50 by fives and 60 to 100 by tens.
        val stops = limitStops(capacity = 128 * GB, current = null)

        val expected = listOf(HALF_GB) + (1L..10L).map { it * GB } + (15L..50L step 5).map { it * GB } +
            (60L..100L step 10).map { it * GB } + null
        assertEquals(expected, stops)
    }

    @Test
    fun aLimitChosenBeforeStaysAmongTheStops() {
        val stops = limitStops(capacity = 12 * GB, current = 7 * GB / 2)

        assertEquals(listOf(HALF_GB, 1 * GB, 2 * GB, 3 * GB, 7 * GB / 2, 4 * GB), stops.take(6))
    }

    @Test
    fun aNearlyFullDiskStillOffersHalfAGigabyteAndAnUnknownOneUpTo20() {
        assertEquals(listOf(HALF_GB, null), limitStops(capacity = HALF_GB / 2, current = null))
        assertEquals(15 * GB, limitStops(capacity = null, current = null).dropLast(1).last())
    }

    private companion object {
        const val GB = 1024L * 1024 * 1024
        const val HALF_GB = GB / 2
    }
}
