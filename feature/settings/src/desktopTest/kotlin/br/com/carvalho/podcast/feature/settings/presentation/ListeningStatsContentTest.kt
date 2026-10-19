package br.com.carvalho.podcast.feature.settings.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import br.com.carvalho.podcast.core.designsystem.PodcastTheme
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.date_day_month
import br.com.carvalho.podcast.core.ui.generated.resources.stats_empty_title
import br.com.carvalho.podcast.core.ui.generated.resources.stats_saved
import br.com.carvalho.podcast.core.ui.generated.resources.stats_this_week
import br.com.carvalho.podcast.core.ui.generated.resources.stats_week_of
import br.com.carvalho.podcast.domain.model.ListeningStats
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastListening
import br.com.carvalho.podcast.domain.model.WeekListening
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ListeningStatsContentTest {
    private fun text(res: StringResource) = runBlocking { getString(res) }

    private val podcast = Podcast(
        id = "p1", title = "Tech Show", description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = "p1", siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )

    // Weeks starting on Mondays from 2026-08-24 to 2026-10-12.
    private val stats = ListeningStats(
        weeks = (0 until ListeningStats.WEEKS).map { WeekListening(FIRST_MONDAY + it * 7L, it * HOUR_MS) },
        podcasts = listOf(PodcastListening(podcast, 3 * HOUR_MS + 5 * MINUTE_MS)),
        totalListenedMs = 28 * HOUR_MS,
        totalSavedMs = 45 * MINUTE_MS,
    )

    @Test
    fun theWeekTheTotalsAndEachPodcastShow() = runComposeUiTest {
        setContent { PodcastTheme { ListeningStatsContent(stats, onBack = {}) } }

        onNodeWithText(text(Res.string.stats_this_week)).assertIsDisplayed()
        onNodeWithText("28h").assertIsDisplayed()
        onNodeWithText(text(Res.string.stats_saved)).assertIsDisplayed()
        onNodeWithText("45min").assertIsDisplayed()
        // Each bar tells its week and time to a screen reader.
        val monday = runBlocking { getString(Res.string.date_day_month, 12, 10) }
        val lastWeek = runBlocking { getString(Res.string.stats_week_of, monday, "7h") }
        onNodeWithContentDescription(lastWeek).assertExists()
        onNodeWithText("Tech Show").assertExists()
        onNodeWithText("3h 5min").assertExists()
    }

    @Test
    fun nothingHeardSaysSo() = runComposeUiTest {
        setContent { PodcastTheme { ListeningStatsContent(ListeningStats(), onBack = {}) } }

        onNodeWithText(text(Res.string.stats_empty_title)).assertIsDisplayed()
    }

    @Test
    fun aCoverScreenShowsTheTotalsFirst() = runDesktopComposeUiTest(width = COVER_WIDTH, height = COVER_HEIGHT) {
        setContent { PodcastTheme { ListeningStatsContent(stats, onBack = {}) } }
        val window = onRoot().getBoundsInRoot()

        val week = onNodeWithText(text(Res.string.stats_this_week)).getBoundsInRoot()
        assertTrue(week.bottom <= window.bottom && week.right <= window.right, "this week at $week is cut off")
    }

    private companion object {
        const val MINUTE_MS = 60_000L
        const val HOUR_MS = 60 * MINUTE_MS
        const val FIRST_MONDAY = 20_689L
        const val COVER_WIDTH = 469
        const val COVER_HEIGHT = 318
    }
}
