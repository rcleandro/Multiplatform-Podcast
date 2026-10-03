package br.com.carvalho.podcast.core.designsystem.component

import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_open_player
import br.com.carvalho.podcast.core.designsystem.Sizes
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.designsystem.generated.resources.Res
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_download
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_download_failed
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_download_queued
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_downloaded
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_downloaded_label
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_downloading
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_loading
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_pause
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_play
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_played
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_resume_progress
import br.com.carvalho.podcast.core.designsystem.generated.resources.ds_unplayed_count
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ComponentsTest {

    private fun text(res: org.jetbrains.compose.resources.StringResource, vararg args: Any) =
        runBlocking { getString(res, *args) }

    @Test
    fun downloadButtonRunsTheActionOfEachState() = runComposeUiTest {
        var state: DownloadState by mutableStateOf(DownloadState.Idle)
        val calls = mutableListOf<String>()
        setContent {
            PodcastTheme {
                DownloadButton(
                    state = state,
                    onDownload = { calls += "download" },
                    onCancel = { calls += "cancel" },
                    onRemove = { calls += "remove" },
                )
            }
        }
        val expected = listOf(
            DownloadState.Idle to (text(Res.string.ds_download) to "download"),
            DownloadState.Queued to (text(Res.string.ds_download_queued) to "cancel"),
            DownloadState.Downloading(0.64f) to (text(Res.string.ds_downloading, 64) to "cancel"),
            DownloadState.Downloaded to (text(Res.string.ds_downloaded) to "remove"),
            DownloadState.Failed to (text(Res.string.ds_download_failed) to "download"),
        )
        expected.forEach { (newState, labelAndAction) ->
            state = newState
            waitForIdle()
            onNodeWithContentDescription(labelAndAction.first).performClick()
        }
        assertEquals(expected.map { it.second.second }, calls)
    }

    @Test
    fun playPauseButtonDescribesItsState() = runComposeUiTest {
        var isPlaying by mutableStateOf(false)
        var isLoading by mutableStateOf(false)
        var clicks = 0
        setContent {
            PodcastTheme { PlayPauseButton(isPlaying = isPlaying, isLoading = isLoading, onClick = { clicks++ }) }
        }
        onNodeWithContentDescription(text(Res.string.ds_play)).performClick()
        isPlaying = true
        waitForIdle()
        onNodeWithContentDescription(text(Res.string.ds_pause)).performClick()
        isLoading = true
        waitForIdle()
        onNodeWithContentDescription(text(Res.string.ds_loading)).assertIsNotEnabled()
        assertEquals(2, clicks)
    }

    @Test
    fun startedEpisodeButtonSaysHowMuchWasPlayed() = runComposeUiTest {
        setContent {
            PodcastTheme {
                PlayPauseButton(isPlaying = false, onClick = {}, style = PlayButtonStyle.Tonal, progress = 0.62f)
            }
        }
        onNodeWithContentDescription(text(Res.string.ds_resume_progress, 62)).assertExists()
    }

    @Test
    fun filterChipRowReportsTheTappedOption() = runComposeUiTest {
        var selected by mutableStateOf(0)
        setContent {
            PodcastTheme {
                FilterChipRow(
                    options = listOf(FilterOption("Todos"), FilterOption("Tecnologia", 5)),
                    selectedIndex = selected,
                    onSelected = { selected = it },
                )
            }
        }
        onNodeWithText("Tecnologia  5").performClick()
        onNodeWithText("Tecnologia  5").assertIsSelected()
        assertEquals(1, selected)
    }

    @Test
    fun episodeRowShowsPlayedAndDownloadedMarkers() = runComposeUiTest {
        var plays = 0
        setContent {
            PodcastTheme {
                EpisodeRow(
                    title = "Como funciona o Pix",
                    metadata = "12 set · 1 h 08 min",
                    imageUrl = null,
                    playback = EpisodePlayback(isPlayed = true, progress = 0.5f),
                    downloadState = DownloadState.Downloaded,
                    onClick = {}, onPlay = { plays++ }, onDownload = {}, onCancelDownload = {}, onRemoveDownload = {},
                )
            }
        }
        onNodeWithText(text(Res.string.ds_played)).assertExists()
        onNodeWithText(text(Res.string.ds_downloaded_label)).assertExists()
        // A played episode offers a plain replay, not "resume at 50%".
        onNodeWithContentDescription(text(Res.string.ds_play)).performClick()
        assertEquals(1, plays)
    }

    @Test
    fun miniPlayerShowsTheEpisodeAndTogglesPlayback() = runComposeUiTest {
        var toggles = 0
        var opened = 0
        setContent {
            PodcastTheme {
                MiniPlayer(
                    title = "Teste Episodio",
                    subtitle = "Teste Podcast",
                    imageUrl = null,
                    isPlaying = true,
                    isLoading = false,
                    progress = 0.3f,
                    onPlayPause = { toggles++ },
                    onClick = { opened++ },
                )
            }
        }
        onNodeWithContentDescription(text(Res.string.ds_pause)).performClick()
        onNodeWithText("Teste Episodio").performClick()
        assertEquals(1, toggles)
        assertEquals(1, opened)
    }

    @Test
    fun podcastCardShowsTitleAuthorAndUnplayedBadge() = runComposeUiTest {
        var clicked = false
        setContent {
            PodcastTheme {
                PodcastCard(title = "Teste Podcast", author = "Teste Author", imageUrl = null, unplayedCount = 3,
                    onClick = { clicked = true })
            }
        }
        onNodeWithText("Teste Author").assertExists()
        onNodeWithContentDescription(runBlocking { getPluralString(Res.plurals.ds_unplayed_count, 3, 3) }).assertExists()
        onNodeWithText("Teste Podcast").performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun controlsMeetTheMinimumTouchTarget() = runComposeUiTest {
        setContent {
            PodcastTheme {
                androidx.compose.foundation.layout.Row {
                    PlayPauseButton(isPlaying = false, onClick = {})
                    DownloadButton(state = DownloadState.Idle, onDownload = {}, onCancel = {}, onRemove = {})
                }
            }
        }
        listOf(text(Res.string.ds_play), text(Res.string.ds_download)).forEach { label ->
            onNodeWithContentDescription(label)
                .assertWidthIsAtLeast(Sizes.touchTarget)
                .assertHeightIsAtLeast(Sizes.touchTarget)
        }
    }

    @Test
    fun miniPlayerAnnouncesThatItOpensThePlayer() = runComposeUiTest {
        setContent {
            PodcastTheme {
                MiniPlayer(title = "Ep", subtitle = null, imageUrl = null, isPlaying = false, isLoading = false,
                    progress = 0f, onPlayPause = {}, onClick = {})
            }
        }
        val label = text(Res.string.ds_open_player)
        onNode(SemanticsMatcher("click label is '$label'") { it.config.getOrNull(SemanticsActions.OnClick)?.label == label })
            .assertExists()
    }

    @Test
    fun stateMessageTitleIsAHeading() = runComposeUiTest {
        setContent {
            PodcastTheme { EmptyState(icon = androidx.compose.material.icons.Icons.Rounded.Mic, title = "Vazio", message = null) }
        }
        onNode(isHeading() and hasText("Vazio")).assertExists()
    }
}
