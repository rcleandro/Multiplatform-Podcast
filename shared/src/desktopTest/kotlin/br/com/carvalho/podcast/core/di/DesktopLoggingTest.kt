package br.com.carvalho.podcast.core.di

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopLoggingTest {
    @AfterTest
    fun clear() {
        System.clearProperty(JPACKAGE_VERSION)
    }

    // The installers log from info up, like Android's release (22.9); `./gradlew run` keeps the debug lines.
    @Test
    fun onlyTheAppRunFromGradleLogsDebugLines() {
        assertTrue(isUnpackaged())

        System.setProperty(JPACKAGE_VERSION, "1.0.0")

        assertFalse(isUnpackaged())
    }

    private companion object {
        const val JPACKAGE_VERSION = "jpackage.app-version"
    }
}
