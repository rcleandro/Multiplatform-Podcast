package br.com.carvalho.podcast.core.safety

import android.app.Activity
import br.com.carvalho.podcast.domain.model.AgeGroup
import com.google.android.play.agesignals.AgeSignalsAccessResult
import com.google.android.play.agesignals.AgeSignalsResult
import com.google.android.play.agesignals.model.AgeSignalsStatus
import com.google.android.play.agesignals.testing.FakeAgeSignalsManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayAgeSignalSourceTest {
    private val context = RuntimeEnvironment.getApplication()
    private val manager = FakeAgeSignalsManager()
    private val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

    private fun access(status: Int) =
        manager.setNextAgeSignalsAccessResult(AgeSignalsAccessResult.builder().setAgeSignalsStatus(status).build())

    private fun range(lower: Int?, upper: Int?) =
        manager.setNextAgeSignalsResult(AgeSignalsResult.builder().setAgeLower(lower).setAgeUpper(upper).build())

    @Test
    fun aSharedRangeTellsAMinorFromAnAdult() = runTest {
        val source = PlayAgeSignalSource(context, manager) { activity }
        access(AgeSignalsStatus.SHARED)

        range(13, 15)
        assertEquals(AgeGroup.MINOR, source.ageGroup())
        range(18, null)
        assertEquals(AgeGroup.ADULT, source.ageGroup())
    }

    @Test
    fun aDeclinedShareOrNoScreenInFrontLeavesItUnknown() = runTest {
        access(AgeSignalsStatus.NOT_SHARED)
        range(13, 15)
        assertEquals(AgeGroup.UNKNOWN, PlayAgeSignalSource(context, manager) { activity }.ageGroup())

        assertEquals(AgeGroup.UNKNOWN, PlayAgeSignalSource(context, manager) { null }.ageGroup())
    }
}
