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

    /** A directory search (18.1). The term stays out: what someone looks for can say a lot about them. */
    class SearchDirectory(resultCount: Int) : AnalyticsEvent("search_directory", mapOf("results" to resultCount))

    /** A category's chart opened in "Add podcast" (18.2). */
    class BrowseCategory(category: String) : AnalyticsEvent("browse_category", mapOf("category" to category))

    /** A podcast link opened in the app (18.4): only the feed's host, never its address. */
    class OpenPodcastLink(host: String) : AnalyticsEvent("open_podcast_link", mapOf(HOST to host))

    class AddPodcastAttempt(host: String) : AnalyticsEvent("add_podcast_attempt", mapOf(HOST to host))

    class AddPodcastSuccess(host: String) : AnalyticsEvent("add_podcast_success", mapOf(HOST to host))

    class AddPodcastFailure(host: String, error: Throwable) :
        AnalyticsEvent("add_podcast_failure", mapOf(HOST to host, ERROR to error::class.simpleName))

    class DeletePodcast(host: String) : AnalyticsEvent("delete_podcast", mapOf(HOST to host))

    data object RefreshAllPodcasts : AnalyticsEvent("refresh_all_podcasts")

    class RefreshPodcast(host: String) : AnalyticsEvent("refresh_podcast_detail", mapOf(HOST to host))

    class SetEpisodeFilter(filter: String) : AnalyticsEvent("set_episode_filter", mapOf("filter" to filter))

    /** "History" emptied (18.12). */
    data object ClearHistory : AnalyticsEvent("clear_history")

    /** An episode put into "Saved" or taken out of it (18.12). */
    class SaveEpisode(saved: Boolean) : AnalyticsEvent(if (saved) "save_episode" else "unsave_episode")

    /** One action done to several selected episodes at once (18.11). */
    class ActOnEpisodes(action: String, count: Int) :
        AnalyticsEvent("act_on_episodes", mapOf("action" to action, "count" to count))

    /** A podcast's own speed or skipped start and end changed (18.14). */
    data object SetPodcastPlayback : AnalyticsEvent("set_podcast_playback")

    /** A podcast's episodes listed oldest or newest first (18.10). */
    class SetEpisodeOrder(oldestFirst: Boolean) :
        AnalyticsEvent("set_episode_order", mapOf("order" to if (oldestFirst) "oldest_first" else "newest_first"))

    /** An episode, or all of them, taken out of "New" without hearing it (18.7). */
    class DismissNew(all: Boolean) : AnalyticsEvent(if (all) "dismiss_all_new_episodes" else "dismiss_new_episode")

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

    /** More time on a running sleep timer (18.15). */
    class ExtendSleepTimer(minutes: Int) : AnalyticsEvent("extend_sleep_timer", mapOf("minutes" to minutes))

    data object CancelSleepTimer : AnalyticsEvent("cancel_sleep_timer")

    /** A moment marked (19.3); whether it has a note, never the note. */
    class AddBookmark(hasNote: Boolean) : AnalyticsEvent("add_bookmark", mapOf("has_note" to hasNote))

    data object DeleteBookmark : AnalyticsEvent("delete_bookmark")

    /** Played from a bookmark on the episode's page. */
    data object PlayBookmark : AnalyticsEvent("play_bookmark")

    private companion object {
        const val EPISODE_ID = "episode_id"
        const val HOST = "host"
        const val ERROR = "error"
    }
}
