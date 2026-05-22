package br.com.carvalho.podcast.data.repository

import br.com.carvalho.podcast.data.local.dao.EpisodeDao
import br.com.carvalho.podcast.data.local.dao.PodcastDao
import br.com.carvalho.podcast.data.mapper.toDomain
import br.com.carvalho.podcast.data.mapper.toEntity
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.core.util.AppLogger
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "PodcastRepository"

class PodcastRepositoryImpl(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao
) : PodcastRepository {

    override fun getPodcasts(): Flow<List<Podcast>> {
        return podcastDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPodcastById(id: String): Podcast? {
        return podcastDao.getById(id)?.toDomain()
    }

    override fun getPodcastByIdFlow(id: String): Flow<Podcast?> {
        return podcastDao.getByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getEpisodeById(id: String): Episode? {
        return episodeDao.getById(id)?.toDomain()
    }

    override fun getEpisodes(podcastId: String): Flow<List<Episode>> {
        return episodeDao.getByPodcast(podcastId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEpisodesPaged(podcastId: String): Flow<PagingData<Episode>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { EpisodePagingSource(episodeDao, podcastId) }
        ).flow
    }

    override fun searchEpisodes(query: String): Flow<List<Episode>> {
        return episodeDao.search(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchEpisodesPaged(query: String?): Flow<PagingData<Episode>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { EpisodePagingSource(episodeDao, query = query) }
        ).flow
    }

    override fun getDownloadedEpisodes(): Flow<List<Episode>> {
        return episodeDao.getDownloaded().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getUnplayedEpisodes(): Flow<List<Episode>> {
        return episodeDao.getUnplayed().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateEpisodeProgress(id: String, progress: Long) {
        episodeDao.updatePlayback(id, false, progress)
    }

    override suspend fun markEpisodeAsPlayed(id: String) {
        episodeDao.updatePlayback(id, true, 0L)
    }

    override suspend fun savePodcast(podcast: Podcast) {
        AppLogger.d(TAG, "Saving podcast: ${podcast.title}")
        podcastDao.insert(podcast.toEntity())
    }

    override suspend fun saveEpisodes(episodes: List<Episode>) {
        AppLogger.d(TAG, "Saving ${episodes.size} episodes")
        episodeDao.insertAll(episodes.map { it.toEntity() })
    }

    override suspend fun deletePodcast(id: String) {
        AppLogger.i(TAG, "Deleting podcast id: $id")
        episodeDao.deleteByPodcast(id)
        podcastDao.deleteById(id)
    }

    override suspend fun markOlderEpisodesAsPlayed(podcastId: String, publishDate: Long) {
        episodeDao.markOlderAsPlayed(podcastId, publishDate)
    }
}
