package br.com.carvalho.podcast.core.observability

/**
 * Every analytics event the app sends. Parameters carry counts and enum names only: never a feed URL (private feeds
 * carry tokens), a title, nor anything that tells which podcast or episode it was about, not even the feed's host or
 * a directory category (listening history can reveal religion, politics or health; LGPD Art. 6º, III, and Art. 11;
 * 27.2, 27.5). `AnalyticsEventTest` checks names and keys against
 * Firebase's limits.
 */
sealed class AnalyticsEvent(val name: String, val params: Map<String, Any?> = emptyMap()) {
    enum class PlaySource(val key: String) {
        LIBRARY("library"),
        SEARCH("search"),
        PODCAST("detail"),
        EPISODE("episode_detail"),
    }

    class PlayEpisode(source: PlaySource) : AnalyticsEvent("play_episode_from_${source.key}")

    data object LoadEpisodeDetail : AnalyticsEvent("load_episode_detail")

    class LoadEpisodeDetailError(error: Throwable) :
        AnalyticsEvent("load_episode_detail_error", mapOf(ERROR to error::class.simpleName))

    /** A directory search (18.1). The term stays out: what someone looks for can say a lot about them. */
    class SearchDirectory(resultCount: Int) : AnalyticsEvent("search_directory", mapOf("results" to resultCount))

    /** A category's chart opened in "Add podcast" (18.2). */
    data object BrowseCategory : AnalyticsEvent("browse_category")

    /** A podcast link opened in the app (18.4). */
    data object OpenPodcastLink : AnalyticsEvent("open_podcast_link")

    data object AddPodcastAttempt : AnalyticsEvent("add_podcast_attempt")

    data object AddPodcastSuccess : AnalyticsEvent("add_podcast_success")

    class AddPodcastFailure(error: Throwable) :
        AnalyticsEvent("add_podcast_failure", mapOf(ERROR to error::class.simpleName))

    data object DeletePodcast : AnalyticsEvent("delete_podcast")

    data object RefreshAllPodcasts : AnalyticsEvent("refresh_all_podcasts")

    data object RefreshPodcast : AnalyticsEvent("refresh_podcast_detail")

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

    data object DownloadEpisode : AnalyticsEvent("download_episode_from_detail")

    data object DeleteDownload : AnalyticsEvent("delete_download_from_detail")

    class MarkPlayed(played: Boolean) : AnalyticsEvent(if (played) "mark_as_played" else "mark_as_unplayed")

    class MarkOlderPlayed(played: Boolean) :
        AnalyticsEvent(if (played) "mark_older_as_played" else "mark_older_as_unplayed")

    /** An episode picked from the player's queue. */
    data object PlayInPlayer : AnalyticsEvent("play_episode")

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

    /** A smart playlist made or changed (19.4); its name and filters stay on the device. */
    class SaveSmartPlaylist(isNew: Boolean) : AnalyticsEvent("save_smart_playlist", mapOf("is_new" to isNew))

    data object DeleteSmartPlaylist : AnalyticsEvent("delete_smart_playlist")

    class PlaySmartPlaylist(episodes: Int) : AnalyticsEvent("play_smart_playlist", mapOf("episodes" to episodes))

    /** "Similar to this" opened from a podcast's page (19.6), with how many it found. */
    class ShowSimilar(found: Int) : AnalyticsEvent("show_similar", mapOf("found" to found))

    /** Played from a line of the transcript (19.2); whether it was found by a search, never the words. */
    class PlayFromTranscript(searching: Boolean) :
        AnalyticsEvent("play_from_transcript", mapOf("searching" to searching))

    /** Played from a bookmark on the episode's page. */
    data object PlayBookmark : AnalyticsEvent("play_bookmark")

    private companion object {
        const val ERROR = "error"
    }
}
