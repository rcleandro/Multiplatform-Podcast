package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import br.com.carvalho.podcast.core.designsystem.LocalTabletopFold
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.TabletopFold
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The player in each screen type the app supports (21.8): phone, tablet held upright, phone on its side, a flip
 * phone's cover screen, a large window and a foldable half open on a table. Record with the "Record snapshots" workflow (Linux);
 * `verifyRoborazziAndroidHostTest` checks them in CI.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PlayerSnapshotTest {

    private val state = PlayerState(
        currentEpisode = Episode(
            id = "e1", podcastId = "p", podcastTitle = "Hipsters Ponto Tech", title = "Como funciona o Pix por dentro",
            description = null, audioUrl = "", imageUrl = null, duration = 3600, publishDate = 0, isPlayed = false,
            playbackPosition = 0, isDownloaded = false, fileSize = null,
        ),
        position = 1_200_000,
        // Paused: playing animates the progress without end, and a capture waits for the screen to settle.
        isPlaying = false,
    )

    private fun snap(name: String, content: @Composable () -> Unit = { PlayerContent(state, PlayerActions()) }) =
        captureRoboImage("snapshots/player-$name.png") {
            PodcastTheme(darkTheme = true) { content() }
        }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun compact() = snap("compact")

    @Test
    @Config(qualifiers = "w700dp-h1000dp-xhdpi")
    fun tabletUpright() = snap("tablet-upright")

    @Test
    @Config(qualifiers = "w780dp-h360dp-xxhdpi")
    fun short() = snap("short")

    @Test
    @Config(qualifiers = "w469dp-h318dp-xxhdpi")
    fun coverScreen() = snap("cover-screen")

    @Test
    @Config(qualifiers = "w1280dp-h800dp-mdpi")
    fun expanded() = snap("expanded")

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun tabletop() = snap("tabletop") {
        // The fold across the middle of a 891 dp screen at xxhdpi (3 px per dp).
        CompositionLocalProvider(LocalTabletopFold provides TabletopFold(top = FOLD_TOP_PX, bottom = FOLD_BOTTOM_PX)) {
            PlayerContent(state, PlayerActions())
        }
    }

    private companion object {
        const val FOLD_TOP_PX = 1320
        const val FOLD_BOTTOM_PX = 1356
    }
}
