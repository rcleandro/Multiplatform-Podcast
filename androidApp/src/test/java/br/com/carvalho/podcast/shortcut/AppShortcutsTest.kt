package br.com.carvalho.podcast.shortcut

import android.app.Application
import androidx.core.content.pm.ShortcutManagerCompat
import br.com.carvalho.podcast.presentation.navigation.RootComponent
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// A bare application: the real one starts the player and the sync.
@Config(sdk = [35], application = Application::class)
class AppShortcutsTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun theLauncherGetsThreeShortcutsThatOpenWhatTheyName() = runTest {
        AppShortcuts.publish(context)

        val shortcuts = ShortcutManagerCompat.getDynamicShortcuts(context).sortedBy { it.rank }
        assertEquals(listOf("Continue listening", "Queue", "Add Podcast"), shortcuts.map { it.shortLabel.toString() })

        val actions = shortcuts.map { it.intent.action }
        val roots = actions.map { RootComponent(DefaultComponentContext(LifecycleRegistry())) }
        val resumes = actions.zip(roots).map { (action, root) -> AppShortcuts.open(action, root) }

        assertEquals(listOf(true, false, false), resumes)
        assertTrue(roots[0].state.value.isPlayerOpen)
        assertTrue(roots[1].state.value.isQueueOpen)
        assertTrue(roots[2].state.value.isAddingPodcast)
        assertFalse(AppShortcuts.open("android.intent.action.MAIN", roots[0]))
    }
}
