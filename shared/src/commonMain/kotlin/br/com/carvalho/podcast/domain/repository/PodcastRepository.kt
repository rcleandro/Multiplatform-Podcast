package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface PodcastRepository {
    fun getPodcasts(): Flow<List<Podcast>>
    suspend fun getPodcastById(id: String): Podcast?
    fun getPodcastByIdFlow(id: String): Flow<Podcast?>
    fun getEpisodes(podcastId: String): Flow<List<Episode>>
    fun getEpisodesPaged(podcastId: String): Flow<PagingData<Episode>>
    fun getDownloadedEpisodes(): Flow<List<Episode>>
    fun getUnplayedEpisodes(): Flow<List<Episode>>
    suspend fun getEpisodeById(id: String): Episode?
    fun searchEpisodes(query: String): Flow<List<Episode>>
    fun searchEpisodesPaged(query: String?): Flow<PagingData<Episode>>
    suspend fun updateEpisodeProgress(id: String, progress: Long)
    suspend fun markEpisodeAsPlayed(id: String)
    suspend fun savePodcast(podcast: Podcast)
    suspend fun saveEpisodes(episodes: List<Episode>)
    suspend fun deletePodcast(id: String)
}
