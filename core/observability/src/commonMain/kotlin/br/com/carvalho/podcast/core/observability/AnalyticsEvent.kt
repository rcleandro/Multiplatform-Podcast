package br.com.carvalho.podcast.core.observability

/**
 * Every analytics event the app sends. Parameters carry ids, hosts and enum names only: never a feed URL (private
 * feeds carry tokens) nor a title. `AnalyticsEventTest` checks names and keys against Firebase's limits.
 */
sealed class AnalyticsEvent(val name: String, val params: Map<String, Any?> = emptyMap()) {
    enum class PlaySource(val key: String) {
        LIBRARY("library"),
        SEARCH("search"),
        PODCAST("detail"),
        EPISODE("episode_detail"),
    }

    class PlayEpisode(episodeId: String, source: PlaySource) :
        AnalyticsEvent("play_episode_from_${source.key}", mapOf(EPISODE_ID to episodeId))

    class LoadEpisodeDetail(episodeId: String) : AnalyticsEvent("load_episode_detail", mapOf(EPISODE_ID to episodeId))

    class LoadEpisodeDetailError(episodeId: String, error: Throwable) :
        AnalyticsEvent("load_episode_detail_error", mapOf(EPISODE_ID to episodeId, ERROR to error::class.simpleName))

    class AddPodcastAttempt(host: String) : AnalyticsEvent("add_podcast_attempt", mapOf(HOST to host))

    class AddPodcastSuccess(host: String) : AnalyticsEvent("add_podcast_success", mapOf(HOST to host))

    class AddPodcastFailure(host: String, error: Throwable) :
        AnalyticsEvent("add_podcast_failure", mapOf(HOST to host, ERROR to error::class.simpleName))

    class DeletePodcast(host: String) : AnalyticsEvent("delete_podcast", mapOf(HOST to host))

    data object RefreshAllPodcasts : AnalyticsEvent("refresh_all_podcasts")

    class RefreshPodcast(host: String) : AnalyticsEvent("refresh_podcast_detail", mapOf(HOST to host))

    class SetEpisodeFilter(filter: String) : AnalyticsEvent("set_episode_filter", mapOf("filter" to filter))

    class DownloadEpisode(episodeId: String) :
        AnalyticsEvent("download_episode_from_detail", mapOf(EPISODE_ID to episodeId))

    class DeleteDownload(episodeId: String) :
        AnalyticsEvent("delete_download_from_detail", mapOf(EPISODE_ID to episodeId))

    class MarkPlayed(episodeId: String, played: Boolean) :
        AnalyticsEvent(if (played) "mark_as_played" else "mark_as_unplayed", mapOf(EPISODE_ID to episodeId))

    class MarkOlderPlayed(host: String, publishDate: Long, played: Boolean) : AnalyticsEvent(
        if (played) "mark_older_as_played" else "mark_older_as_unplayed",
        mapOf(HOST to host, "publish_date" to publishDate),
    )

    /** An episode picked from the player's queue. */
    class PlayInPlayer(episodeId: String) : AnalyticsEvent("play_episode", mapOf(EPISODE_ID to episodeId))

    data object Pause : AnalyticsEvent("pause_episode")

    data object Resume : AnalyticsEvent("resume_episode")

    class Seek(positionMs: Long) : AnalyticsEvent("seek_episode", mapOf("position_ms" to positionMs))

    data object SkipForward : AnalyticsEvent("skip_forward")

    data object SkipBackward : AnalyticsEvent("skip_backward")

    class SetSpeed(speed: Float) : AnalyticsEvent("set_speed", mapOf("speed" to speed))

    data object PlayNext : AnalyticsEvent("play_next")

    data object PlayPrevious : AnalyticsEvent("play_previous")

    /** `minutes` is null for "end of episode". */
    class SetSleepTimer(minutes: Int?) :
        AnalyticsEvent("set_sleep_timer", mapOf("minutes" to (minutes ?: "end_of_episode")))

    data object CancelSleepTimer : AnalyticsEvent("cancel_sleep_timer")

    private companion object {
        const val EPISODE_ID = "episode_id"
        const val HOST = "host"
        const val ERROR = "error"
    }
}
