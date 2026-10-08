package br.com.carvalho.podcast.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class KeyboardShortcutsModifierTest {

    @Test
    fun aKeyNothingHandledRunsItsShortcut() = runComposeUiTest {
        val shortcuts = mutableListOf<Shortcut>()
        setContent {
            Box(modifier = Modifier.keyboardShortcuts { shortcuts += it }.testTag(ROOT))
        }

        onNodeWithTag(ROOT).performKeyInput {
            pressKey(Key.Spacebar)
            pressKey(Key.DirectionRight)
            pressKey(Key.Escape)
            withKeyDown(Key.MetaLeft) { pressKey(Key.DirectionLeft) }
        }

        assertEquals(listOf(Shortcut.PLAY_PAUSE, Shortcut.SKIP_FORWARD, Shortcut.BACK), shortcuts)
    }

    @Test
    fun typingASpaceInATextFieldDoesNotPlay() = runComposeUiTest {
        val shortcuts = mutableListOf<Shortcut>()
        setContent {
            Column(modifier = Modifier.keyboardShortcuts { shortcuts += it }.testTag(ROOT)) {
                BasicTextField(value = "", onValueChange = {}, modifier = Modifier.testTag(FIELD))
            }
        }

        onNodeWithTag(FIELD).performClick()
        onNodeWithTag(FIELD).performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(emptyList(), shortcuts)

        // Escape leaves the field; then the keys are shortcuts again.
        onNodeWithTag(FIELD).performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag(ROOT).performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(listOf(Shortcut.PLAY_PAUSE), shortcuts)
    }

    private companion object {
        const val ROOT = "root"
        const val FIELD = "field"
    }
}
