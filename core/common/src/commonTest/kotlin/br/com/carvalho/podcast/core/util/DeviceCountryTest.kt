package br.com.carvalho.podcast.core.util

import kotlin.test.Test
import kotlin.test.assertTrue

class DeviceCountryTest {
    /** The directory search sends it as `country`: two capital letters, or nothing when the system has no region. */
    @Test
    fun theCountryIsAnIsoCodeOrNothing() {
        val country = deviceCountry()

        assertTrue(country == null || Regex("[A-Z]{2}").matches(country), "Got $country")
    }
}
