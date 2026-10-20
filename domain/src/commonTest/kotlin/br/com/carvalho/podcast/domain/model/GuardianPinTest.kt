package br.com.carvalho.podcast.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GuardianPinTest {
    @Test
    fun onlyFourDigitsMakeAPin() {
        assertTrue(GuardianPin.isValid("0420"))
        assertFalse(GuardianPin.isValid("042"))
        assertFalse(GuardianPin.isValid("04a0"))
    }

    @Test
    fun theStoredPinMatchesItsDigitsAndNeverHoldsThem() {
        val stored = GuardianPin.hash("1234")

        assertTrue(GuardianPin.matches("1234", stored))
        assertFalse(GuardianPin.matches("4321", stored))
        assertFalse("1234" in stored)
        // A fresh salt each time: the same PIN never looks the same twice.
        assertNotEquals(stored, GuardianPin.hash("1234"))
    }
}
