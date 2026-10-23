package br.com.carvalho.podcast.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Writes the baseline profile of 20.1: the code a cold start runs until the library shows, so it ships compiled.
 * Run with a phone connected, then copy the file it prints into androidApp/src/main/baseline-prof.txt:
 * `./gradlew :benchmark:connectedBaselineProfileAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.class=br.com.carvalho.podcast.benchmark.BaselineProfileGenerator`
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startUntilTheLibraryShows() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), TIMEOUT_MS)
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE = "br.com.carvalho.podcast"
        const val TIMEOUT_MS = 10_000L
    }
}
