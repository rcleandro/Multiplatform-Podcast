package br.com.carvalho.podcast.feature.player.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.carvalho.podcast.core.ui.generated.resources.sleep_timer
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.next
import br.com.carvalho.podcast.core.ui.generated.resources.previous
import br.com.carvalho.podcast.core.ui.generated.resources.state_on
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.PlayerState
import br.com.carvalho.podcast.domain.player.SleepTimer
import br.com.carvalho.podcast.domain.model.Podcast
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class PlayerContentTest {

    private fun text(res: StringResource) = runBlocking { getString(res) }

    private fun episode(id: String) = Episode(
        id = id, podcastId = "p", podcastTitle = "Podcast", title = "Episode $id", description = null,
        audioUrl = "", imageUrl = null, duration = 60, publishDate = 0, isPlayed = false,
        playbackPosition = 0, isDownloaded = false, fileSize = null,
    )

    @Test
    fun aShortWindowStillReachesTheBottomButtons() = runComposeUiTest {
        val queue = listOf(episode("1"))
        setContent {
            PodcastTheme {
                Box(modifier = Modifier.size(width = 400.dp, height = 360.dp)) {
                    PlayerContent(state = PlayerState(currentEpisode = queue.first(), queue = queue), actions = PlayerActions())
                }
            }
        }
        onNodeWithText(text(Res.string.sleep_timer)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun playerDisablesNextOnTheLastEpisodeOfTheQueue() = runComposeUiTest {
        val queue = listOf(episode("1"), episode("2"))
        setContent {
            PodcastTheme {
                PlayerContent(state = PlayerState(currentEpisode = queue.last(), queue = queue), actions = PlayerActions())
            }
        }
        onNodeWithContentDescription(text(Res.string.next)).assertIsNotEnabled()
        onNodeWithContentDescription(text(Res.string.previous)).assertIsEnabled()
    }


    @Test
    fun activeSleepTimerIsAnnouncedNotJustColored() = runComposeUiTest {
        val queue = listOf(episode("1"))
        var timer by mutableStateOf<SleepTimer?>(null)
        setContent {
            PodcastTheme {
                PlayerContent(
                    state = PlayerState(currentEpisode = queue.first(), queue = queue, sleepTimer = timer),
                    actions = PlayerActions(),
                )
            }
        }
        onNode(hasStateDescription(text(Res.string.state_on))).assertDoesNotExist()
        // The end-of-episode timer has no countdown and must still show as on.
        timer = SleepTimer.EndOfEpisode
        waitForIdle()
        onNode(hasStateDescription(text(Res.string.state_on))).assertExists()
    }
}
