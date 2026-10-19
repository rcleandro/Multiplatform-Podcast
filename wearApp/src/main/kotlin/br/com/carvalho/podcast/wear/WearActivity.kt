package br.com.carvalho.podcast.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import br.com.carvalho.podcast.domain.player.AudioPlayer
import org.koin.android.ext.android.inject

class WearActivity : ComponentActivity() {
    private val player: AudioPlayer by inject()

    /** Bumped by each request to show the player, from the tile or the face (19.9f). */
    private var playerRequests by mutableIntStateOf(0)

    /** Play or pause asked from the tile: done once on screen, the only place the system lets playback start. */
    private var togglePending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent { WearApp(playerRequests) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!togglePending) return
        togglePending = false
        if (player.playerState.value.isPlaying) player.pause() else player.resume()
    }

    private fun handle(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) playerRequests++
        togglePending = intent.getBooleanExtra(EXTRA_TOGGLE_PLAYBACK, false)
        // Once only: a recreated activity must not play or pause again.
        intent.removeExtra(EXTRA_OPEN_PLAYER)
        intent.removeExtra(EXTRA_TOGGLE_PLAYBACK)
    }

    companion object {
        const val EXTRA_OPEN_PLAYER = "openPlayer"
        const val EXTRA_TOGGLE_PLAYBACK = "togglePlayback"
    }
}
