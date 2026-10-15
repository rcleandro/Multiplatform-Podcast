package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Replay30
import androidx.compose.material.icons.rounded.Replay5
import kotlin.test.Test
import kotlin.test.assertSame

class SkipIconsTest {
    @Test
    fun theIconShowsTheConfiguredJumpWhenMaterialHasIt() {
        assertSame(Icons.Rounded.Forward10, skipForwardIcon(seconds = 10))
        assertSame(Icons.Rounded.Replay30, skipBackwardIcon(seconds = 30))
        assertSame(Icons.Rounded.Forward5, skipForwardIcon(seconds = 5))
        assertSame(Icons.Rounded.Replay5, skipBackwardIcon(seconds = 5))
    }

    @Test
    fun anyOtherJumpGetsAPlainArrowInsteadOfAWrongNumber() {
        assertSame(Icons.Rounded.FastForward, skipForwardIcon(seconds = 15))
        assertSame(Icons.Rounded.FastRewind, skipBackwardIcon(seconds = 15))
    }
}
