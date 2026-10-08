package br.com.carvalho.podcast.presentation.navigation

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeyboardShortcutsTest {

    @Test
    fun spaceAndTheMediaKeyPlayOrPause() {
        assertEquals(Shortcut.PLAY_PAUSE, shortcutFor(Key.Spacebar))
        assertEquals(Shortcut.PLAY_PAUSE, shortcutFor(Key.MediaPlayPause))
    }

    @Test
    fun arrowsSkipAndEscapeGoesBack() {
        assertEquals(Shortcut.SKIP_BACKWARD, shortcutFor(Key.DirectionLeft))
        assertEquals(Shortcut.SKIP_FORWARD, shortcutFor(Key.DirectionRight))
        assertEquals(Shortcut.BACK, shortcutFor(Key.Escape))
    }

    @Test
    fun otherKeysDoNothing() {
        assertNull(shortcutFor(Key.A))
        assertNull(shortcutFor(Key.Enter))
    }
}
