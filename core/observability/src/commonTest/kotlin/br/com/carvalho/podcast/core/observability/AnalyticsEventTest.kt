package br.com.carvalho.podcast.core.observability

import br.com.carvalho.podcast.core.observability.AnalyticsEvent.PlaySource
import kotlin.test.Test
import kotlin.test.assertTrue

class AnalyticsEventTest {
    @Test
    fun namesAndParameterKeysFollowFirebaseRules() {
        val invalid = everyEvent().flatMap { listOf(it.name) + it.params.keys }.filterNot(::isValidFirebaseName)

        assertTrue(invalid.isEmpty(), "Invalid names: $invalid")
    }

    @Test
    fun parametersNeverCarryAFeedUrl() {
        val leaking = everyEvent().filter { event -> event.params.values.any { "://" in it.toString() } }

        assertTrue(leaking.isEmpty(), "Events with a URL: ${leaking.map { it.name }}")
    }

    // Listening history is personal data that can reveal religion, politics or health (LGPD Art. 11): no event may
    // say which podcast or episode it was about, by id or by a value that pins it down, such as a publish date or the
    // feed's host, nor which directory category interested the person (27.2, 27.5).
    @Test
    fun parametersNeverIdentifyWhatWasListenedTo() {
        val identifying = everyEvent().filter { event -> event.params.keys.any { it in IDENTIFYING_KEYS } }

        assertTrue(identifying.isEmpty(), "Events that identify what was listened to: ${identifying.map { it.name }}")
    }

    /** One of each event; the `when` stops compiling when an event is added, so it gets a sample here too. */
    private fun everyEvent(): List<AnalyticsEvent> {
        val error = IllegalStateException("https://feeds.example.com/private?token=secret")
        val samples = PlaySource.entries.map { AnalyticsEvent.PlayEpisode(it) } + listOf(
            AnalyticsEvent.LoadEpisodeDetail,
            AnalyticsEvent.LoadEpisodeDetailError(error),
            AnalyticsEvent.SearchDirectory(resultCount = 3),
            AnalyticsEvent.BrowseCategory,
            AnalyticsEvent.OpenPodcastLink,
            AnalyticsEvent.AddPodcastAttempt,
            AnalyticsEvent.AddPodcastSuccess,
            AnalyticsEvent.AddPodcastFailure(error),
            AnalyticsEvent.DeletePodcast,
            AnalyticsEvent.RefreshAllPodcasts,
            AnalyticsEvent.RefreshPodcast,
            AnalyticsEvent.SetEpisodeFilter("UNPLAYED"),
            AnalyticsEvent.SetEpisodeOrder(oldestFirst = true),
            AnalyticsEvent.ActOnEpisodes("MARK_PLAYED", count = 3),
            AnalyticsEvent.SaveEpisode(saved = true),
            AnalyticsEvent.SaveEpisode(saved = false),
            AnalyticsEvent.ClearHistory,
            AnalyticsEvent.SetPodcastPlayback,
            AnalyticsEvent.DismissNew(all = false),
            AnalyticsEvent.DismissNew(all = true),
            AnalyticsEvent.DownloadEpisode,
            AnalyticsEvent.DeleteDownload,
            AnalyticsEvent.MarkPlayed(played = true),
            AnalyticsEvent.MarkPlayed(played = false),
            AnalyticsEvent.MarkOlderPlayed(played = true),
            AnalyticsEvent.MarkOlderPlayed(played = false),
            AnalyticsEvent.PlayInPlayer,
            AnalyticsEvent.Pause,
            AnalyticsEvent.Resume,
            AnalyticsEvent.Seek(0),
            AnalyticsEvent.SkipForward,
            AnalyticsEvent.SkipBackward,
            AnalyticsEvent.SetSpeed(1f),
            AnalyticsEvent.PlayNext,
            AnalyticsEvent.PlayPrevious,
            AnalyticsEvent.SetSleepTimer(MINUTES),
            AnalyticsEvent.SetSleepTimer(null),
            AnalyticsEvent.CancelSleepTimer,
            AnalyticsEvent.ExtendSleepTimer(minutes = 5),
            AnalyticsEvent.AddBookmark(hasNote = true),
            AnalyticsEvent.DeleteBookmark,
            AnalyticsEvent.PlayBookmark,
            AnalyticsEvent.SaveSmartPlaylist(isNew = true),
            AnalyticsEvent.DeleteSmartPlaylist,
            AnalyticsEvent.PlaySmartPlaylist(episodes = 3),
            AnalyticsEvent.ShowSimilar(found = 4),
            AnalyticsEvent.PlayFromTranscript(searching = true),
        )
        samples.forEach { covered(it) }
        return samples
    }

    @Suppress("CyclomaticComplexMethod")
    private fun covered(event: AnalyticsEvent) = when (event) {
        is AnalyticsEvent.PlayEpisode, AnalyticsEvent.LoadEpisodeDetail, is AnalyticsEvent.LoadEpisodeDetailError,
        is AnalyticsEvent.SearchDirectory, AnalyticsEvent.BrowseCategory, AnalyticsEvent.OpenPodcastLink,
        AnalyticsEvent.AddPodcastAttempt, AnalyticsEvent.AddPodcastSuccess, is AnalyticsEvent.AddPodcastFailure,
        AnalyticsEvent.DeletePodcast, AnalyticsEvent.RefreshAllPodcasts, AnalyticsEvent.RefreshPodcast,
        is AnalyticsEvent.SetEpisodeFilter, is AnalyticsEvent.SetEpisodeOrder,
        is AnalyticsEvent.ActOnEpisodes, is AnalyticsEvent.SaveEpisode,
        AnalyticsEvent.ClearHistory, AnalyticsEvent.SetPodcastPlayback, is AnalyticsEvent.DismissNew, AnalyticsEvent.DownloadEpisode, AnalyticsEvent.DeleteDownload,
        is AnalyticsEvent.MarkPlayed, is AnalyticsEvent.MarkOlderPlayed, AnalyticsEvent.PlayInPlayer,
        AnalyticsEvent.Pause, AnalyticsEvent.Resume, is AnalyticsEvent.Seek, AnalyticsEvent.SkipForward,
        AnalyticsEvent.SkipBackward, is AnalyticsEvent.SetSpeed, AnalyticsEvent.PlayNext, AnalyticsEvent.PlayPrevious,
        is AnalyticsEvent.SetSleepTimer, AnalyticsEvent.CancelSleepTimer, is AnalyticsEvent.ExtendSleepTimer,
        is AnalyticsEvent.AddBookmark, AnalyticsEvent.DeleteBookmark, AnalyticsEvent.PlayBookmark,
        is AnalyticsEvent.SaveSmartPlaylist, AnalyticsEvent.DeleteSmartPlaylist, is AnalyticsEvent.PlaySmartPlaylist,
        is AnalyticsEvent.ShowSimilar, is AnalyticsEvent.PlayFromTranscript -> Unit
    }

    /** Firebase: up to 40 characters, a letter first, then letters, digits and `_`; reserved prefixes rejected. */
    private fun isValidFirebaseName(name: String) =
        name.length <= MAX_NAME_LENGTH && FIREBASE_NAME.matches(name) && RESERVED_PREFIXES.none(name::startsWith)

    private companion object {
        const val MINUTES = 30
        const val MAX_NAME_LENGTH = 40
        val FIREBASE_NAME = Regex("[a-z][a-z0-9_]*")
        val RESERVED_PREFIXES = listOf("firebase_", "google_", "ga_")
        val IDENTIFYING_KEYS = setOf("episode_id", "publish_date", "host", "category")
    }
}
