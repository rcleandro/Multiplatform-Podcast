package br.com.carvalho.podcast.feature.episode.presentation

import br.com.carvalho.podcast.core.ui.generated.resources.add_to_queue
import br.com.carvalho.podcast.core.ui.generated.resources.play_next
import br.com.carvalho.podcast.core.ui.generated.resources.share
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.download_cd
import br.com.carvalho.podcast.core.ui.generated.resources.mark_as_played
import br.com.carvalho.podcast.core.ui.generated.resources.pause
import br.com.carvalho.podcast.core.ui.generated.resources.play
import br.com.carvalho.podcast.core.ui.generated.resources.resume_remaining
import br.com.carvalho.podcast.core.ui.generated.resources.bookmark_delete
import br.com.carvalho.podcast.core.ui.generated.resources.bookmarks
import br.com.carvalho.podcast.domain.model.Bookmark
import br.com.carvalho.podcast.domain.model.Episode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class EpisodeDetailContentTest {

    private fun text(res: StringResource) = runBlocking { getString(res) }

    private fun episode(positionMs: Long = 0) = Episode(
        id = "e1", podcastId = "p", podcastTitle = "Podcast", title = "Episode", description = null,
        audioUrl = "a", imageUrl = null, duration = 3_600, publishDate = 0, isPlayed = false,
        playbackPosition = positionMs, isDownloaded = false, fileSize = null,
    )

    @Test
    fun theEpisodeIsSharedFromTheBar() = runComposeUiTest {
        var shared = 0
        setContent {
            PodcastTheme {
                EpisodeDetailContent(EpisodeDetailUiState(episode = episode()), EpisodeDetailActions(onShare = { shared++ }))
            }
        }

        onNodeWithContentDescription(text(Res.string.share)).performClick()

        assertEquals(1, shared)
    }

    @Test
    fun aNewEpisodeOffersToPlay() = runComposeUiTest {
        setContent { PodcastTheme { EpisodeDetailContent(EpisodeDetailUiState(episode = episode()), EpisodeDetailActions()) } }
        onNodeWithText(text(Res.string.play)).assertExists()
    }

    @Test
    fun aStartedEpisodeOffersToResumeWithWhatIsLeft() = runComposeUiTest {
        setContent {
            PodcastTheme {
                EpisodeDetailContent(EpisodeDetailUiState(episode = episode(positionMs = 1_080_000)), EpisodeDetailActions())
            }
        }
        onNodeWithText(runBlocking { getString(Res.string.resume_remaining, "42min") }).assertHasClickAction()
    }

    @Test
    fun theEpisodeThatIsPlayingOffersToPause() = runComposeUiTest {
        setContent {
            PodcastTheme { EpisodeDetailContent(EpisodeDetailUiState(episode = episode(), isPlaying = true), EpisodeDetailActions()) }
        }
        onNodeWithText(text(Res.string.pause)).assertExists()
    }

    @Test
    fun theScreenDownloadsAndMarksAsPlayed() = runComposeUiTest {
        var downloads = 0
        var marks = 0
        setContent {
            PodcastTheme {
                EpisodeDetailContent(
                    EpisodeDetailUiState(episode = episode()),
                    EpisodeDetailActions(onDownload = { downloads++ }, onMarkPlayed = { marks++ }),
                )
            }
        }
        onNodeWithText(text(Res.string.download_cd)).performClick()
        onNodeWithText(text(Res.string.mark_as_played)).performClick()

        assertEquals(1, downloads)
        assertEquals(1, marks)
    }

    @Test
    fun theEpisodeGoesToTheQueueNextOrLast() = runComposeUiTest {
        val asked = mutableListOf<Boolean>()
        setContent {
            PodcastTheme {
                EpisodeDetailContent(EpisodeDetailUiState(episode = episode()), EpisodeDetailActions(onEnqueue = { asked += it }))
            }
        }
        onNodeWithText(text(Res.string.play_next)).performClick()
        onNodeWithText(text(Res.string.add_to_queue)).performClick()

        assertEquals(listOf(true, false), asked)
    }

    @Test
    fun theBookmarksShowAboveTheDescriptionOnlyWhenThereAreAny() = runComposeUiTest {
        var state by mutableStateOf(EpisodeDetailUiState(episode = episode()))
        var played: Bookmark? = null
        var deleted: Bookmark? = null
        setContent {
            PodcastTheme {
                EpisodeDetailContent(
                    state,
                    EpisodeDetailActions(onPlayBookmark = { played = it }, onDeleteBookmark = { deleted = it }),
                )
            }
        }
        onNodeWithText(text(Res.string.bookmarks)).assertDoesNotExist()

        val bookmark = Bookmark(id = 1, episodeId = "e1", positionMs = 754_000, note = "The quote", createdAt = 0)
        state = state.copy(bookmarks = listOf(bookmark))
        onNodeWithText("12:34").performScrollTo().performClick()
        onNodeWithContentDescription(text(Res.string.bookmark_delete)).performClick()

        assertEquals(bookmark, played)
        assertEquals(bookmark, deleted)
    }
}
