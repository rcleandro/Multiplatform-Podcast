package br.com.carvalho.podcast.domain.repository

import androidx.paging.PagingData
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import br.com.carvalho.podcast.domain.model.EpisodeListFilter
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.Podcast
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

    /** The order the user chose for a podcast's episodes (18.10). */
    suspend fun setEpisodeOrder(podcastId: String, oldestFirst: Boolean)

    fun getDownloadedEpisodes(): Flow<List<Episode>>
    fun getUnplayedEpisodes(): Flow<List<Episode>>

    /** Started and not finished, for "Continue listening". */
    fun getInProgressEpisodes(): Flow<List<Episode>>
    suspend fun getEpisodeById(id: String): Episode?
    fun searchEpisodes(query: String): Flow<List<Episode>>
    fun searchEpisodesPaged(
        query: String?,
        filter: EpisodeListFilter = EpisodeListFilter.ALL,
    ): Flow<PagingData<Episode>>
    suspend fun updateEpisodeProgress(id: String, progress: Long)
    suspend fun markEpisodeAsPlayed(id: String)

    /** Takes an episode out of "New" (18.7): dismissed, or started. */
    suspend fun dismissNewEpisode(id: String)

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
