package br.com.carvalho.podcast.presentation.navigation

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/** What a key does anywhere in the app on a computer (21.7). */
enum class Shortcut { PLAY_PAUSE, SKIP_BACKWARD, SKIP_FORWARD, BACK }

/** The [Shortcut] of [key], if it has one. */
fun shortcutFor(key: Key): Shortcut? = when (key) {
    Key.Spacebar, Key.MediaPlayPause -> Shortcut.PLAY_PAUSE
    Key.DirectionLeft -> Shortcut.SKIP_BACKWARD
    Key.DirectionRight -> Shortcut.SKIP_FORWARD
    Key.Escape -> Shortcut.BACK
    else -> null
}

/**
 * Runs the [Shortcut] of each key pressed while this element itself has the focus, which it takes when shown. A
 * text field that has the focus keeps every key, since typing a space reaches here too; Escape hands the focus back,
 * so a second Escape goes back. A key with Ctrl, Alt or Cmd is left to the system.
 */
@Composable
fun Modifier.keyboardShortcuts(onShortcut: (Shortcut) -> Unit): Modifier {
    val focus = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(focus) { focus.requestFocus() }
    return onKeyEvent { event ->
        val plain = !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed
        val shortcut = shortcutFor(event.key)?.takeIf { event.type == KeyEventType.KeyDown && plain }
        when {
            shortcut == null -> false
            isFocused -> {
                onShortcut(shortcut)
                true
            }
            shortcut == Shortcut.BACK -> focus.requestFocus()
            else -> false
        }
    }
        .focusRequester(focus)
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
}
