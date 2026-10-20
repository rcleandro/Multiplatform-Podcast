package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.AgeGroup
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakeAgeSignalSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class UsageReminderTest {
    private val player = FakeAudioPlayer()
    private val signal = FakeAgeSignalSource(countries = setOf("BR"), ageGroup = AgeGroup.MINOR)
    private val childSafety = ChildSafety(signal)

    private fun TestScope.reminder() = UsageReminder(player, childSafety, backgroundScope).also { runCurrent() }

    @Test
    fun aMinorIsOfferedABreakAfterAnHourOfListeningPausesIncluded() = runTest {
        childSafety.refresh()
        val reminder = reminder()

        player.resume()
        advanceTimeBy(40.minutes)
        player.pause()
        advanceTimeBy(2.hours)
        assertFalse(reminder.due.value)

        player.resume()
        advanceTimeBy(21.minutes)
        assertTrue(reminder.due.value)

        reminder.dismiss()
        advanceTimeBy(59.minutes)
        assertFalse(reminder.due.value)
    }

    @Test
    fun anAdultIsNeverInterrupted() = runTest {
        signal.ageGroup = AgeGroup.ADULT
        childSafety.refresh()
        val reminder = reminder()

        player.resume()
        advanceTimeBy(3.hours)

        assertFalse(reminder.due.value)
    }
}
