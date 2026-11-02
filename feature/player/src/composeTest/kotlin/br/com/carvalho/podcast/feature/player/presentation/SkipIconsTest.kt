package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.material.icons.Icons
import br.com.carvalho.podcast.core.designsystem.icons.FastForward
import br.com.carvalho.podcast.core.designsystem.icons.FastRewind
import br.com.carvalho.podcast.core.designsystem.icons.Forward10
import br.com.carvalho.podcast.core.designsystem.icons.Forward5
import br.com.carvalho.podcast.core.designsystem.icons.Replay30
import br.com.carvalho.podcast.core.designsystem.icons.Replay5
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
