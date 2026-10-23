package br.com.carvalho.podcast.core.player

import android.os.Bundle
import androidx.media3.session.SessionCommand
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionCommandsTest {
    @Test
    fun androidAutoCanBrowseAndSearchAndTheJumpsStayAvailable() {
        val jump = SessionCommand("JUMP", Bundle.EMPTY)

        val commands = sessionCommandsWith(jump)

        listOf(
            SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT,
            SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN,
            SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM,
            SessionCommand.COMMAND_CODE_LIBRARY_SEARCH,
        ).forEach { assertTrue("command $it", commands.contains(it)) }
        assertTrue(commands.contains(jump))
    }
}
