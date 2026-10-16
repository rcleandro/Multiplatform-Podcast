package br.com.carvalho.podcast.domain.repository

import androidx.paging.PagingData
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import br.com.carvalho.podcast.domain.model.EpisodeListFilter
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastPlayback
import br.com.carvalho.podcast.domain.model.SmartPlaylist
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions") // one function per library query or change
interface PodcastRepository {
    fun getPodcasts(): Flow<List<Podcast>>
    fun getLibrary(): Flow<List<LibraryEntry>>

    /** Saves the custom order: [podcastIds] from first to last. */
    suspend fun reorderLibrary(podcastIds: List<String>)
    suspend fun getPodcastById(id: String): Podcast?
    fun getPodcastByIdFlow(id: String): Flow<Podcast?>
    fun getEpisodes(podcastId: String): Flow<List<Episode>>

    fun getEpisodeCount(podcastId: String): Flow<Int>

    /** The newest episode not played yet, for the podcast's "Latest" button. */
    fun getLatestUnplayedEpisode(podcastId: String): Flow<Episode?>
    fun getEpisodesPaged(
        podcastId: String,
        filter: EpisodeFilter,
        oldestFirst: Boolean = false,
    ): Flow<PagingData<Episode>>

    /** How many of a podcast's newest unheard episodes stay downloaded (18.19). */
    suspend fun setPodcastAutoDownload(podcastId: String, count: Int)

    /** Whether a podcast's new episodes notify (18.18). */
    suspend fun setPodcastNotifications(podcastId: String, enabled: Boolean)

    /** What waits in "New" (18.7), newest first. */
    suspend fun getNewEpisodes(): List<Episode>

    /** How a podcast plays (18.14). */
    suspend fun setPodcastPlayback(podcastId: String, playback: PodcastPlayback)

    /** The order the user chose for a podcast's episodes (18.10). */
    suspend fun setEpisodeOrder(podcastId: String, oldestFirst: Boolean)

    fun getDownloadedEpisodes(): Flow<List<Episode>>
    fun getUnplayedEpisodes(): Flow<List<Episode>>

    /** Started and not finished, for "Continue listening". */
    fun getInProgressEpisodes(): Flow<List<Episode>>
    suspend fun getEpisodeById(id: String): Episode?

    /** The saved filters of the Episodes tab (19.4), in the order they were made. */
    fun getSmartPlaylists(): Flow<List<SmartPlaylist>>

    /** Adds [playlist], or changes the one with its id. */
    suspend fun saveSmartPlaylist(playlist: SmartPlaylist)
    suspend fun deleteSmartPlaylist(id: Long)

    /** What matches [playlist] now, newest first. */
    fun getSmartPlaylistEpisodesPaged(playlist: SmartPlaylist): Flow<PagingData<Episode>>
    suspend fun getSmartPlaylistEpisodes(playlist: SmartPlaylist): List<Episode>

    /** Every episode the user did something with: played, started, saved or in "History" (for the backup, 19.11). */
    suspend fun getEpisodesWithHistory(): List<Episode>

    /** Every episode played or begun, with when it changed, for the library sync (19.9b). */
    suspend fun getSyncedEpisodes(): List<Episode>

    /** The latest of those changes, to know when there is something new to sync. */
    fun lastEpisodeChange(): Flow<Long>

    /** Played and progress as a synced device had them at [updatedAt] (19.9b). */
    suspend fun applyEpisodeState(id: String, played: Boolean, position: Long, updatedAt: Long)

    /** The podcasts taken out of the library: feed address and when (19.9b). */
    suspend fun getRemovedPodcasts(): Map<String, Long>
    fun searchEpisodes(query: String): Flow<List<Episode>>
    fun searchEpisodesPaged(
        query: String?,
        filter: EpisodeListFilter = EpisodeListFilter.ALL,
    ): Flow<PagingData<Episode>>
    suspend fun updateEpisodeProgress(id: String, progress: Long)
    suspend fun markEpisodeAsPlayed(id: String)

    /** Takes an episode out of "New" (18.7): dismissed, or started. */
    suspend fun dismissNewEpisode(id: String)

    /** Started playing at [at]: out of "New" (18.7) and to the top of "History" (18.12). */
    suspend fun markEpisodeStarted(id: String, at: Long)

    /** Empties "History"; nothing else about the episodes changes. */
    suspend fun clearHistory()

    suspend fun dismissAllNewEpisodes()

    /** Back to not played, from the start. */
    suspend fun markEpisodeAsUnplayed(id: String)

    /** Into "Saved", or out of it (18.12). */
    suspend fun setEpisodeSaved(id: String, saved: Boolean)

    /** Saves a podcast and its episodes all at once: if any of it fails, nothing is saved. */
    suspend fun saveFeed(podcast: Podcast, episodes: List<Episode>)
    suspend fun deletePodcast(id: String)
    suspend fun markOlderEpisodesAsPlayed(podcastId: String, publishDate: Long)

    /** Back to not played, from the start, for the episode published at [publishDate] and every older one. */
    suspend fun markOlderEpisodesAsUnplayed(podcastId: String, publishDate: Long)
}
