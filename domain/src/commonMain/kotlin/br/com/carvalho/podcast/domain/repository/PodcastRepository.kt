package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.Podcast
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface PodcastRepository {
    fun getPodcasts(): Flow<List<Podcast>>
    fun getLibrary(): Flow<List<LibraryEntry>>

    /** Saves the custom order: [podcastIds] from first to last. */
    suspend fun reorderLibrary(podcastIds: List<String>)
    suspend fun getPodcastById(id: String): Podcast?
    fun getPodcastByIdFlow(id: String): Flow<Podcast?>
    fun getEpisodes(podcastId: String): Flow<List<Episode>>
    fun getEpisodesPaged(podcastId: String, filter: EpisodeFilter): Flow<PagingData<Episode>>

    /** The podcast's episodes published at or after [publishDate], oldest first: what plays after one of them. */
    suspend fun getEpisodesSince(podcastId: String, publishDate: Long): List<Episode>
    fun getDownloadedEpisodes(): Flow<List<Episode>>
    fun getUnplayedEpisodes(): Flow<List<Episode>>
    suspend fun getEpisodeById(id: String): Episode?
    fun searchEpisodes(query: String): Flow<List<Episode>>
    fun searchEpisodesPaged(query: String?): Flow<PagingData<Episode>>
    suspend fun updateEpisodeProgress(id: String, progress: Long)
    suspend fun markEpisodeAsPlayed(id: String)
    /** Saves a podcast and its episodes all at once: if any of it fails, nothing is saved. */
    suspend fun saveFeed(podcast: Podcast, episodes: List<Episode>)
    suspend fun deletePodcast(id: String)
    suspend fun markOlderEpisodesAsPlayed(podcastId: String, publishDate: Long)
}
