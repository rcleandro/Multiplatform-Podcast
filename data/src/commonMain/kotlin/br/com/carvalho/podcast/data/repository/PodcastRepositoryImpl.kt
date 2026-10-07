package br.com.carvalho.podcast.data.repository

import br.com.carvalho.podcast.data.local.dao.EpisodeDao
import br.com.carvalho.podcast.data.local.dao.PodcastDao
import br.com.carvalho.podcast.data.mapper.toDomain
import br.com.carvalho.podcast.data.mapper.toEntity
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.core.util.AppLogger
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import br.com.carvalho.podcast.domain.model.EpisodeListFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PAGE_SIZE = 20
private const val IN_PROGRESS_LIMIT = 10

private const val TAG = "PodcastRepository"

class PodcastRepositoryImpl(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao
) : PodcastRepository {

    override fun getLibrary(): Flow<List<LibraryEntry>> = podcastDao.getLibrary().map { rows ->
        rows.map { LibraryEntry(it.podcast.toDomain(), it.unplayedCount, it.latestEpisodeDate) }
    }

    override suspend fun reorderLibrary(podcastIds: List<String>) = podcastDao.reorder(podcastIds)

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

    override fun getEpisodesPaged(podcastId: String, filter: EpisodeFilter): Flow<PagingData<Episode>> =
        pagedEpisodes {
            episodeDao.pagingSourceByPodcast(
                podcastId,
                onlyUnplayed = filter == EpisodeFilter.UNPLAYED,
                onlyDownloaded = filter == EpisodeFilter.DOWNLOADED,
            )
        }

    override suspend fun getEpisodesSince(podcastId: String, publishDate: Long): List<Episode> =
        episodeDao.getSince(podcastId, publishDate).map { it.toDomain() }

    override fun searchEpisodes(query: String): Flow<List<Episode>> {
        return episodeDao.search(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchEpisodesPaged(query: String?, filter: EpisodeListFilter): Flow<PagingData<Episode>> =
        pagedEpisodes {
            episodeDao.searchPagingSource(
                query.orEmpty(),
                onlyInProgress = filter == EpisodeListFilter.IN_PROGRESS,
                onlyDownloaded = filter == EpisodeListFilter.DOWNLOADED,
            )
        }

    private fun pagedEpisodes(source: () -> PagingSource<Int, EpisodeEntity>): Flow<PagingData<Episode>> =
        Pager(config = PagingConfig(pageSize = PAGE_SIZE), pagingSourceFactory = source)
            .flow
            .map { page -> page.map { it.toDomain() } }

    override fun getDownloadedEpisodes(): Flow<List<Episode>> {
        return episodeDao.getDownloaded().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getInProgressEpisodes(): Flow<List<Episode>> =
        episodeDao.getInProgress(IN_PROGRESS_LIMIT).map { entities -> entities.map { it.toDomain() } }

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

    override suspend fun markEpisodeAsUnplayed(id: String) {
        episodeDao.updatePlayback(id, false, 0L)
    }

    override suspend fun saveFeed(podcast: Podcast, episodes: List<Episode>) {
        AppLogger.d(TAG, "Saving podcast ${podcast.title} with ${episodes.size} episodes")
        episodeDao.saveFeed(podcast.toEntity(), episodes.map { it.toEntity() })
    }

    /** One statement: the episodes go with the podcast through the foreign key's ON DELETE CASCADE. */
    override suspend fun deletePodcast(id: String) {
        AppLogger.i(TAG, "Deleting podcast id: $id")
        podcastDao.deleteById(id)
    }

    override suspend fun markOlderEpisodesAsPlayed(podcastId: String, publishDate: Long) {
        episodeDao.markOlderAsPlayed(podcastId, publishDate)
    }
}
