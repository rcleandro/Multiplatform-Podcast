package br.com.carvalho.podcast.widget

import android.app.Application
import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasText
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// A bare application: the real one starts the player and keeps the widget updated.
@Config(sdk = [35], application = Application::class)
class PlayerWidgetContentTest {

    private class Noop : ActionCallback {
        override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) = Unit
    }

    private val texts = PlayerWidgetTexts(
        nothingPlaying = "Nothing playing",
        play = "Play",
        pause = "Pause",
        skipBackward = "Back 10 s",
        skipForward = "Forward 30 s",
    )
    private val actions = actionRunCallback<Noop>().let { noop ->
        PlayerWidgetActions(noop, noop, noop, noop, playEpisode = { noop })
    }
    private val playing = PlayerWidgetState(
        episode = WidgetEpisode("e1", "Como funciona o Pix", "Hipsters", artwork = null),
        isPlaying = true,
        continueListening = listOf(WidgetEpisode("e2", "Rust no backend", "Hipsters", artwork = null)),
    )

    @Test
    fun withNothingPlayingItSaysSo() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(PANEL_WIDTH, PANEL_HEIGHT))
        provideComposable { PlayerWidgetContent(PlayerWidgetState(), texts, actions) }

        onNode(hasText("Nothing playing")).assertExists()
    }

    @Test
    fun itShowsTheEpisodeWithItsControls() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(PANEL_WIDTH, PANEL_HEIGHT))
        provideComposable { PlayerWidgetContent(playing, texts, actions) }

        onNode(hasText("Como funciona o Pix")).assertExists()
        onNode(hasText("Hipsters")).assertExists()
        onNode(hasContentDescriptionEqualTo("Pause")).assertExists()
        onNode(hasContentDescriptionEqualTo("Back 10 s")).assertExists()
        onNode(hasContentDescriptionEqualTo("Forward 30 s")).assertExists()
    }

    @Test
    fun aTallWidgetAlsoShowsTheEpisodesToContinue() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(PANEL_WIDTH, PANEL_HEIGHT))
        provideComposable { PlayerWidgetContent(playing, texts, actions) }

        onNode(hasText("Rust no backend")).assertExists()
    }

    @Test
    fun aShortWidgetIsThePlayerAlone() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(PANEL_WIDTH, SHORT_HEIGHT))
        provideComposable { PlayerWidgetContent(playing, texts, actions) }

        onNode(hasText("Rust no backend")).assertDoesNotExist()
    }

    private companion object {
        /** About the panel of a flip phone's cover screen. */
        val PANEL_WIDTH = 460.dp
        val PANEL_HEIGHT = 460.dp
        val SHORT_HEIGHT = 140.dp
    }
}
