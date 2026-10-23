package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.AgeGroup
import br.com.carvalho.podcast.domain.model.ChildSafetyState
import br.com.carvalho.podcast.domain.model.ageGroupOf
import br.com.carvalho.podcast.domain.repository.FakeAgeSignalSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChildSafetyTest {

    @Test
    fun theStoresAgeRangeTellsAnAdultFromAMinor() {
        assertEquals(AgeGroup.ADULT, ageGroupOf(lower = 18, upper = null))
        assertEquals(AgeGroup.MINOR, ageGroupOf(lower = 13, upper = 15))
        assertEquals(AgeGroup.MINOR, ageGroupOf(lower = null, upper = 17))
        assertEquals(AgeGroup.UNKNOWN, ageGroupOf(lower = null, upper = null))
        // Below a gate of 18 and nothing more said: not enough to call it either way.
        assertEquals(AgeGroup.UNKNOWN, ageGroupOf(lower = 16, upper = null))
    }

    @Test
    fun inBrazilSomeoneIsUnknownUntilTheStoreAnswers() = runTest {
        val source = FakeAgeSignalSource(countries = setOf("US", "br"), ageGroup = AgeGroup.MINOR)
        val safety = ChildSafety(source)
        assertEquals(ChildSafetyState(inBrazil = true, ageGroup = AgeGroup.UNKNOWN), safety.state.value)
        assertTrue(safety.state.value.hidesExplicit)

        safety.refresh()

        assertEquals(AgeGroup.MINOR, safety.state.value.ageGroup)
        assertTrue(safety.state.value.isMinor)
    }

    @Test
    fun anAdultInBrazilAndAnyoneElsewhereSeeTheAppAsItWas() = runTest {
        val adult = ChildSafety(FakeAgeSignalSource(countries = setOf("BR"), ageGroup = AgeGroup.ADULT))
        adult.refresh()
        assertFalse(adult.state.value.hidesExplicit)

        val abroad = FakeAgeSignalSource(countries = setOf("PT"), ageGroup = AgeGroup.MINOR)
        val elsewhere = ChildSafety(abroad)
        elsewhere.refresh()
        assertFalse(elsewhere.state.value.hidesExplicit)
        assertFalse(elsewhere.state.value.isMinor)
        assertEquals(0, abroad.asked)
    }

    @Test
    fun aStoreThatFailsKeepsTheLastAnswer() = runTest {
        val source = FakeAgeSignalSource(countries = setOf("BR"), ageGroup = AgeGroup.MINOR)
        val safety = ChildSafety(source)
        safety.refresh()

        source.failure = IllegalStateException("Play Store not found")
        source.ageGroup = AgeGroup.ADULT
        safety.refresh()

        assertEquals(AgeGroup.MINOR, safety.state.value.ageGroup)
    }
}
