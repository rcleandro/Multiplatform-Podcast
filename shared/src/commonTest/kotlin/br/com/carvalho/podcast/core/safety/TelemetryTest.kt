package br.com.carvalho.podcast.core.safety

import br.com.carvalho.podcast.domain.model.AgeGroup
import br.com.carvalho.podcast.domain.repository.FakeAgeSignalSource
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import br.com.carvalho.podcast.domain.usecase.ChildSafety
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TelemetryTest {
    @Test
    fun aMinorInBrazilSendsNothingWhateverTheSwitchSays() = runTest {
        val preferences = FakePreferencesRepository()
        val signal = FakeAgeSignalSource(countries = setOf("BR"), ageGroup = AgeGroup.ADULT)
        val childSafety = ChildSafety(signal)
        childSafety.refresh()
        assertTrue(telemetryAllowed(preferences, childSafety))

        signal.ageGroup = AgeGroup.MINOR
        childSafety.refresh()

        assertFalse(telemetryAllowed(preferences, childSafety))
        assertFalse(telemetryAllowedFlow(preferences, childSafety).first())
    }
}
