package br.com.carvalho.podcast.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PinLockoutTest {
    @Test
    fun aTypoCostsNothingAndThenEachWrongPinWaitsTwiceAsLongUpToAnHour() {
        val waits = (1..14).map { PinLockout.waitAfter(it) / SECOND }

        assertEquals(listOf(0L, 0L, 0L, 0L, 30L, 60L, 120L, 240L, 480L, 960L, 1920L, 3600L, 3600L, 3600L), waits)
    }

    @Test
    fun theWaitStartsAtTheWrongPinAndRunsOut() {
        val fifth = PinLockout.failed(PinAttempts(failures = 4), now = 1_000)

        assertEquals(PinAttempts(failures = 5, lockedUntil = 1_000 + 30 * SECOND), fifth)
        assertEquals(30 * SECOND, PinLockout.remainingMs(fifth, now = 1_000))
        assertEquals(0, PinLockout.remainingMs(fifth, now = 1_000 + 30 * SECOND))
    }

    private companion object {
        const val SECOND = 1_000L
    }
}
