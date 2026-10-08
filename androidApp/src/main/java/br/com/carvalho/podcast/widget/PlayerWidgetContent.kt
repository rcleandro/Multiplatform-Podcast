package br.com.carvalho.podcast.widget

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.media3.session.R as Media3R

/** What the widget shows, read from the player and the library when the widget updates. */
data class PlayerWidgetState(
    val episode: WidgetEpisode? = null,
    val isPlaying: Boolean = false,
    /** Started episodes other than the current one, played by a tap. */
    val continueListening: List<WidgetEpisode> = emptyList(),
)

data class WidgetEpisode(val id: String, val title: String, val podcastTitle: String?, val artwork: Bitmap?)

/** The widget's texts, resolved from the app's resources before drawing. */
data class PlayerWidgetTexts(
    val nothingPlaying: String,
    val play: String,
    val pause: String,
    val skipBackward: String,
    val skipForward: String,
)

/** The widget's taps; the real ones run callbacks against the player, the tests use plain actions. */
data class PlayerWidgetActions(
    val openApp: Action,
    val playPause: Action,
    val skipBackward: Action,
    val skipForward: Action,
    val playEpisode: (WidgetEpisode) -> Action,
)

private val CORNER = 24.dp
private val ARTWORK = 88.dp
private val TILE_ARTWORK = 56.dp
private val CONTROL = 40.dp
private val PLAY_CONTROL = 52.dp
private val TITLE_SIZE = 16.sp
private val SUBTITLE_SIZE = 13.sp
private val TILE_TITLE_SIZE = 12.sp

/** Below this height the widget is the player alone; above it, the started episodes too. */
private val GRID_MIN_HEIGHT = 220.dp
private const val GRID_COLUMNS = 4

/**
 * The player widget: cover, episode, podcast and the controls on top, and the episodes to continue below when it is
 * tall enough, as on a flip phone's cover screen panel.
 */
@Composable
fun PlayerWidgetContent(state: PlayerWidgetState, texts: PlayerWidgetTexts, actions: PlayerWidgetActions) {
    val showGrid = LocalSize.current.height >= GRID_MIN_HEIGHT && state.continueListening.isNotEmpty()
    Column(
        // The player alone sits in the middle; with the grid, it leads from the top.
        verticalAlignment = if (showGrid) Alignment.Top else Alignment.CenterVertically,
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(CORNER)
            .padding(16.dp),
    ) {
        val episode = state.episode
        if (episode == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier.fillMaxSize().clickable(actions.openApp),
            ) {
                Text(texts.nothingPlaying, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = TITLE_SIZE))
            }
        } else {
            NowPlaying(episode, state.isPlaying, texts, actions)
            if (showGrid) {
                Spacer(modifier = GlanceModifier.height(16.dp))
                LazyVerticalGrid(gridCells = GridCells.Fixed(GRID_COLUMNS)) {
                    items(state.continueListening, itemId = { it.id.hashCode().toLong() }) { item ->
                        Tile(item, actions.playEpisode(item))
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlaying(episode: WidgetEpisode, isPlaying: Boolean, texts: PlayerWidgetTexts, actions: PlayerWidgetActions) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
        Artwork(episode.artwork, ARTWORK, modifier = GlanceModifier.clickable(actions.openApp))
        Spacer(modifier = GlanceModifier.width(16.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Column(modifier = GlanceModifier.clickable(actions.openApp)) {
                Text(
                    text = episode.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = TITLE_SIZE,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                episode.podcastTitle?.let {
                    Text(
                        text = it,
                        maxLines = 1,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = SUBTITLE_SIZE),
                    )
                }
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The app skips 10 s back and 30 s forward (AppConfig), the same icons as the notification.
                Control(Media3R.drawable.media3_icon_skip_back_10, texts.skipBackward, actions.skipBackward)
                Spacer(modifier = GlanceModifier.width(12.dp))
                CircleIconButton(
                    imageProvider = ImageProvider(
                        if (isPlaying) Media3R.drawable.media3_icon_pause else Media3R.drawable.media3_icon_play,
                    ),
                    contentDescription = if (isPlaying) texts.pause else texts.play,
                    onClick = actions.playPause,
                    backgroundColor = GlanceTheme.colors.primary,
                    contentColor = GlanceTheme.colors.onPrimary,
                    modifier = GlanceModifier.size(PLAY_CONTROL),
                )
                Spacer(modifier = GlanceModifier.width(12.dp))
                Control(Media3R.drawable.media3_icon_skip_forward_30, texts.skipForward, actions.skipForward)
            }
        }
    }
}

@Composable
private fun Control(@DrawableRes icon: Int, description: String, onClick: Action) {
    CircleIconButton(
        imageProvider = ImageProvider(icon),
        contentDescription = description,
        onClick = onClick,
        backgroundColor = null,
        contentColor = GlanceTheme.colors.onSurfaceVariant,
        modifier = GlanceModifier.size(CONTROL),
    )
}

@Composable
private fun Tile(episode: WidgetEpisode, onClick: Action) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = GlanceModifier.padding(4.dp).clickable(onClick),
    ) {
        Artwork(episode.artwork, TILE_ARTWORK)
        Text(
            text = episode.title,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = TILE_TITLE_SIZE),
        )
    }
}

@Composable
private fun Artwork(bitmap: Bitmap?, size: androidx.compose.ui.unit.Dp, modifier: GlanceModifier = GlanceModifier) {
    val sized = modifier.size(size).cornerRadius(12.dp)
    if (bitmap != null) {
        Image(ImageProvider(bitmap), contentDescription = null, contentScale = ContentScale.Crop, modifier = sized)
    } else {
        Box(modifier = sized.background(GlanceTheme.colors.surfaceVariant)) {}
    }
}
