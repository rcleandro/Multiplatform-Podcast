package br.com.carvalho.podcast.wear

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.library_title
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.core.ui.generated.resources.play
import br.com.carvalho.podcast.core.ui.generated.resources.widget_nothing_playing
import br.com.carvalho.podcast.domain.player.AudioPlayer
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import org.jetbrains.compose.resources.getString
import org.koin.android.ext.android.inject

/**
 * "Now playing" as a tile (19.9f): the podcast over the episode, and play or pause at the bottom. Both open the app's
 * player, the button also playing or pausing there: Wear OS does not let a tile start the media service. With nothing
 * loaded it says so and opens the app.
 */
// ponytail: no cover, which a tile needs as an image resource; add it if the tile feels bare.
class NowPlayingTileService : TileService() {
    private val player: AudioPlayer by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        scope.future {
            val state = player.restoredState()
            val episode = state.currentEpisode
            val title = episode?.title ?: getString(Res.string.widget_nothing_playing)
            val podcast = episode?.podcastTitle ?: getString(Res.string.library_title)
            val button = getString(if (state.isPlaying) Res.string.pause else Res.string.play)
            val openApp = clickable(openAppAction(openPlayer = episode != null))
            val playPause = clickable(openAppAction(openPlayer = true, toggle = true))
            val layout = materialScope(this@NowPlayingTileService, requestParams.deviceConfiguration) {
                primaryLayout(
                    titleSlot = { text(podcast.layoutString) },
                    mainSlot = {
                        text(title.layoutString, maxLines = TITLE_LINES, modifier = LayoutModifier.clickable(openApp))
                    },
                    bottomSlot = {
                        if (episode == null) {
                            textEdgeButton(onClick = openApp) { text(podcast.layoutString) }
                        } else {
                            textEdgeButton(onClick = playPause) { text(button.layoutString) }
                        }
                    },
                )
            }
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
                .build()
        }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> =
        scope.future { ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build() }

    /** The app, on its player when asked; play or pause happens there, since a tile may not start playback. */
    private fun openAppAction(openPlayer: Boolean, toggle: Boolean = false) = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setPackageName(packageName)
                .setClassName(WearActivity::class.java.name)
                .addKeyToExtraMapping(WearActivity.EXTRA_OPEN_PLAYER, ActionBuilders.booleanExtra(openPlayer))
                .addKeyToExtraMapping(WearActivity.EXTRA_TOGGLE_PLAYBACK, ActionBuilders.booleanExtra(toggle))
                .build(),
        )
        .build()

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private companion object {
        const val RESOURCES_VERSION = "1"
        const val TITLE_LINES = 3
    }
}
