package br.com.carvalho.podcast.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.material3.ColorProviders
import br.com.carvalho.podcast.MainActivity
import br.com.carvalho.podcast.core.AppConfig
import br.com.carvalho.podcast.core.designsystem.DarkColorScheme
import br.com.carvalho.podcast.core.designsystem.LightColorScheme
import br.com.carvalho.podcast.core.image.createImageLoader
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.core.ui.generated.resources.play
import br.com.carvalho.podcast.core.ui.generated.resources.skip_backward
import br.com.carvalho.podcast.core.ui.generated.resources.skip_forward
import br.com.carvalho.podcast.core.ui.generated.resources.widget_nothing_playing
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.PlayEpisodeUseCase
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform.getKoin

/** Episodes to continue shown under the player, a row of the cover screen panel's grid twice. */
private const val CONTINUE_LIMIT = 8

/** Covers are drawn at most this big, in pixels: a widget's bitmaps share a small memory budget. */
private const val ARTWORK_PX = 256

private val colors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)

/**
 * The player as a widget (21.9), for the home screen and for the panels of a flip phone's cover screen: the episode
 * playing with its controls and, when tall enough, the started episodes. It reads the process' [AudioPlayer], the
 * same one the app and the media notification drive.
 */
class PlayerWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = getKoin()
        val player = koin.get<AudioPlayer>()
        val repository = koin.get<PodcastRepository>()
        val loader = Artwork(context)
        // Only what the widget shows: the position ticks every second and would redraw it as often.
        val shown = combine(
            player.playerState.map { it.currentEpisode to it.isPlaying }.distinctUntilChanged(),
            repository.getInProgressEpisodes().distinctUntilChanged(),
        ) { (current, isPlaying), started ->
            PlayerWidgetState(
                episode = current?.let { loader.episode(it) },
                isPlaying = isPlaying,
                continueListening = started.filter { it.id != current?.id }.take(CONTINUE_LIMIT).map { loader.episode(it) },
            )
        }
        val initial = shown.first()
        val texts = PlayerWidgetTexts(
            nothingPlaying = getString(Res.string.widget_nothing_playing),
            play = getString(Res.string.play),
            pause = getString(Res.string.pause),
            skipBackward = getString(Res.string.skip_backward, AppConfig.SKIP_BACKWARD_SECONDS),
            skipForward = getString(Res.string.skip_forward, AppConfig.SKIP_FORWARD_SECONDS),
        )
        val actions = PlayerWidgetActions(
            openApp = actionStartActivity<MainActivity>(),
            playPause = actionRunCallback<PlayPauseAction>(),
            skipBackward = actionRunCallback<SkipBackwardAction>(),
            skipForward = actionRunCallback<SkipForwardAction>(),
            playEpisode = { actionRunCallback<PlayEpisodeAction>(actionParametersOf(EPISODE_ID to it.id)) },
        )
        provideContent {
            // Observed while the widget's session lasts: an update during it recomposes instead of starting over,
            // so a state read once would stay as it was (the episode restored after a cold start never showed).
            val state by shown.collectAsState(initial)
            GlanceTheme(colors = colors) { PlayerWidgetContent(state, texts, actions) }
        }
    }

    companion object {
        /**
         * Redraws the widgets whenever what they show changes: another episode, play or pause, or the started
         * episodes. Position changes are left out; the widget does not show progress.
         */
        fun keepUpdated(context: Context, scope: CoroutineScope) {
            val koin = getKoin()
            combine(
                koin.get<AudioPlayer>().playerState.map { it.currentEpisode?.id to it.isPlaying },
                koin.get<PodcastRepository>().getInProgressEpisodes().map { episodes -> episodes.map { it.id } },
                ::Pair,
            )
                .distinctUntilChanged()
                .onEach { PlayerWidget().updateAll(context) }
                .launchIn(scope)
        }
    }
}

class PlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlayerWidget()
}

/**
 * The same widget offered apart for a flip phone's cover screen, filling a panel by default; [PlayerWidget.updateAll]
 * redraws both, since they share the widget class.
 */
class CoverScreenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlayerWidget()
}

/** Loads covers small enough for a widget, through the app's image loader and its cache. */
private class Artwork(private val context: Context) {
    private val loader = createImageLoader(context)

    suspend fun episode(episode: Episode) = WidgetEpisode(
        id = episode.id,
        title = episode.title,
        podcastTitle = episode.podcastTitle,
        artwork = episode.imageUrl?.let { bitmap(it) },
    )

    private suspend fun bitmap(url: String): Bitmap? {
        val request = ImageRequest.Builder(context).data(url).size(ARTWORK_PX).allowHardware(false).build()
        return loader.execute(request).image?.toBitmap()
    }
}

private val EPISODE_ID = ActionParameters.Key<String>("episodeId")

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val player = getKoin().get<AudioPlayer>()
        if (player.playerState.value.isPlaying) player.pause() else player.resume()
    }
}

class SkipBackwardAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        getKoin().get<AudioPlayer>().skipBackward(AppConfig.SKIP_BACKWARD_SECONDS)
    }
}

class SkipForwardAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        getKoin().get<AudioPlayer>().skipForward(AppConfig.SKIP_FORWARD_SECONDS)
    }
}

class PlayEpisodeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[EPISODE_ID] ?: return
        val koin = getKoin()
        koin.get<PodcastRepository>().getEpisodeById(id)?.let { koin.get<PlayEpisodeUseCase>()(it) }
    }
}
