package br.com.carvalho.podcast.core.player

import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.SessionCommand
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionCommandsTest {
    private val jump = SessionCommand("JUMP", Bundle.EMPTY)

    @Test
    fun androidAutoCanBrowseAndSearchAndTheJumpsStayAvailable() {
        val commands = sessionCommandsFor(trusted = true, jump)

        LIBRARY.forEach { assertTrue("command $it", commands.contains(it)) }
        assertTrue(commands.contains(jump))
        assertTrue(playerCommandsFor(trusted = true).contains(Player.COMMAND_SET_MEDIA_ITEM))
    }

    @Test
    fun anotherAppNeitherReadsTheLibraryNorChangesWhatPlays() {
        val commands = sessionCommandsFor(trusted = false, jump)
        val player = playerCommandsFor(trusted = false)

        LIBRARY.forEach { assertFalse("command $it", commands.contains(it)) }
        assertTrue(commands.contains(jump))
        listOf(Player.COMMAND_PLAY_PAUSE, Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_GET_METADATA)
            .forEach { assertTrue("player command $it", player.contains(it)) }
        listOf(Player.COMMAND_SET_MEDIA_ITEM, Player.COMMAND_CHANGE_MEDIA_ITEMS, Player.COMMAND_GET_TIMELINE)
            .forEach { assertFalse("player command $it", player.contains(it)) }
    }

    @Test
    fun androidAutoBrowsesEvenWithoutMediasTrustButOnlyWhenGoogleSignedIt() {
        assertTrue(browsesLibrary(trusted = true, packageName = "any.app") { false })
        assertTrue(browsesLibrary(trusted = false, packageName = ANDROID_AUTO_PACKAGE) { true })
        assertFalse(browsesLibrary(trusted = false, packageName = ANDROID_AUTO_PACKAGE) { false })
        assertFalse(browsesLibrary(trusted = false, packageName = "any.app") { true })
    }

    private companion object {
        val LIBRARY = listOf(
            SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT,
            SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN,
            SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM,
            SessionCommand.COMMAND_CODE_LIBRARY_SEARCH,
        )
    }
}
