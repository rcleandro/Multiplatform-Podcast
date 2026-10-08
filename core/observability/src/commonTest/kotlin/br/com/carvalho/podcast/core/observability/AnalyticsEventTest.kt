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

    /** One of each event; the `when` stops compiling when an event is added, so it gets a sample here too. */
    private fun everyEvent(): List<AnalyticsEvent> {
        val error = IllegalStateException("https://feeds.example.com/private?token=secret")
        val samples = PlaySource.entries.map { AnalyticsEvent.PlayEpisode(EPISODE, it) } + listOf(
            AnalyticsEvent.LoadEpisodeDetail(EPISODE),
            AnalyticsEvent.LoadEpisodeDetailError(EPISODE, error),
            AnalyticsEvent.AddPodcastAttempt(HOST),
            AnalyticsEvent.AddPodcastSuccess(HOST),
            AnalyticsEvent.AddPodcastFailure(HOST, error),
            AnalyticsEvent.DeletePodcast(HOST),
            AnalyticsEvent.RefreshAllPodcasts,
            AnalyticsEvent.RefreshPodcast(HOST),
            AnalyticsEvent.SetEpisodeFilter("UNPLAYED"),
            AnalyticsEvent.DownloadEpisode(EPISODE),
            AnalyticsEvent.DeleteDownload(EPISODE),
            AnalyticsEvent.MarkPlayed(EPISODE, played = true),
            AnalyticsEvent.MarkPlayed(EPISODE, played = false),
            AnalyticsEvent.MarkOlderPlayed(HOST, publishDate = 0, played = true),
            AnalyticsEvent.MarkOlderPlayed(HOST, publishDate = 0, played = false),
            AnalyticsEvent.PlayInPlayer(EPISODE),
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
        )
        samples.forEach { covered(it) }
        return samples
    }

    @Suppress("CyclomaticComplexMethod")
    private fun covered(event: AnalyticsEvent) = when (event) {
        is AnalyticsEvent.PlayEpisode, is AnalyticsEvent.LoadEpisodeDetail, is AnalyticsEvent.LoadEpisodeDetailError,
        is AnalyticsEvent.AddPodcastAttempt, is AnalyticsEvent.AddPodcastSuccess, is AnalyticsEvent.AddPodcastFailure,
        is AnalyticsEvent.DeletePodcast, AnalyticsEvent.RefreshAllPodcasts, is AnalyticsEvent.RefreshPodcast,
        is AnalyticsEvent.SetEpisodeFilter, is AnalyticsEvent.DownloadEpisode, is AnalyticsEvent.DeleteDownload,
        is AnalyticsEvent.MarkPlayed, is AnalyticsEvent.MarkOlderPlayed, is AnalyticsEvent.PlayInPlayer,
        AnalyticsEvent.Pause, AnalyticsEvent.Resume, is AnalyticsEvent.Seek, AnalyticsEvent.SkipForward,
        AnalyticsEvent.SkipBackward, is AnalyticsEvent.SetSpeed, AnalyticsEvent.PlayNext, AnalyticsEvent.PlayPrevious,
        is AnalyticsEvent.SetSleepTimer, AnalyticsEvent.CancelSleepTimer -> Unit
    }

    /** Firebase: up to 40 characters, a letter first, then letters, digits and `_`; reserved prefixes rejected. */
    private fun isValidFirebaseName(name: String) =
        name.length <= MAX_NAME_LENGTH && FIREBASE_NAME.matches(name) && RESERVED_PREFIXES.none(name::startsWith)

    private companion object {
        const val EPISODE = "a1b2c3"
        const val HOST = "feeds.example.com"
        const val MINUTES = 30
        const val MAX_NAME_LENGTH = 40
        val FIREBASE_NAME = Regex("[a-z][a-z0-9_]*")
        val RESERVED_PREFIXES = listOf("firebase_", "google_", "ga_")
    }
}
