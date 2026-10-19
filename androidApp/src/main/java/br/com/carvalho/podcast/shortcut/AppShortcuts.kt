package br.com.carvalho.podcast.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import br.com.carvalho.podcast.MainActivity
import br.com.carvalho.podcast.R
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.add_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.continue_listening
import br.com.carvalho.podcast.core.ui.generated.resources.queue
import br.com.carvalho.podcast.presentation.navigation.RootComponent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * The launcher's shortcuts (19.7): long press the app icon to resume the last episode, open the queue or add a
 * podcast. Dynamic rather than declared in XML, so their names come from the app's own strings and language.
 */
object AppShortcuts {
    private const val RESUME = "br.com.carvalho.podcast.shortcut.RESUME"
    private const val QUEUE = "br.com.carvalho.podcast.shortcut.QUEUE"
    private const val ADD_PODCAST = "br.com.carvalho.podcast.shortcut.ADD_PODCAST"

    private class Shortcut(val action: String, val label: StringResource, val icon: Int)

    private val shortcuts = listOf(
        Shortcut(RESUME, Res.string.continue_listening, R.drawable.ic_shortcut_play),
        Shortcut(QUEUE, Res.string.queue, R.drawable.ic_shortcut_queue),
        Shortcut(ADD_PODCAST, Res.string.add_podcast, R.drawable.ic_shortcut_add),
    )

    /** Sets the shortcuts in the language the app speaks now; called again when it changes. */
    suspend fun publish(context: Context) {
        val infos = shortcuts.mapIndexed { rank, shortcut ->
            val label = getString(shortcut.label)
            ShortcutInfoCompat.Builder(context, shortcut.action)
                .setShortLabel(label)
                .setIcon(IconCompat.createWithResource(context, shortcut.icon))
                .setIntent(Intent(context, MainActivity::class.java).setAction(shortcut.action))
                .setRank(rank)
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, infos)
    }

    /** Opens what a shortcut's [action] asks for; true when it asks to resume playing, which the caller does. */
    fun open(action: String?, root: RootComponent): Boolean {
        when (action) {
            RESUME -> root.onPlayerClicked()
            QUEUE -> root.onQueueClicked()
            ADD_PODCAST -> root.onAddPodcast()
            else -> return false
        }
        return action == RESUME
    }
}
