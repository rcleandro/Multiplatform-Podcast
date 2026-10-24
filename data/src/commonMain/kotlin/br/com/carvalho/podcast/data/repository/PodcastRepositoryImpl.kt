package br.com.carvalho.podcast.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.data.local.dao.EpisodeDao
import br.com.carvalho.podcast.data.local.dao.PodcastDao
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.SmartPlaylistEntity
import br.com.carvalho.podcast.data.local.searchMatch
import br.com.carvalho.podcast.data.mapper.toDomain
import br.com.carvalho.podcast.data.mapper.toEntity
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import br.com.carvalho.podcast.domain.model.EpisodeListFilter
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.model.PodcastPlayback
import br.com.carvalho.podcast.domain.model.SmartPlaylist
import br.com.carvalho.podcast.domain.model.topCategories
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private const val PAGE_SIZE = 20
private const val SECONDS_PER_MINUTE = 60L
private const val IN_PROGRESS_LIMIT = 10

private const val TAG = "PodcastRepository"

@Suppress("TooManyFunctions") // implements PodcastRepository
class PodcastRepositoryImpl(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    /** Stamps played and progress changes and removals for the library sync (19.9b). */
    private val now: () -> Long = ::getCurrentTimestamp,
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

    override fun getSmartPlaylists(): Flow<List<SmartPlaylist>> = podcastDao.smartPlaylists().map { rows ->
        rows.map {
            SmartPlaylist(
                id = it.id,
                name = it.name,
                category = PodcastCategory.entries.firstOrNull { category -> category.name == it.category },
                unplayedOnly = it.unplayedOnly,
                downloadedOnly = it.downloadedOnly,
                maxMinutes = it.maxMinutes,
            )
        }
    }

    override suspend fun saveSmartPlaylist(playlist: SmartPlaylist) = podcastDao.saveSmartPlaylist(
        SmartPlaylistEntity(
            id = playlist.id,
            name = playlist.name.trim(),
            category = playlist.category?.name,
            unplayedOnly = playlist.unplayedOnly,
            downloadedOnly = playlist.downloadedOnly,
            maxMinutes = playlist.maxMinutes,
        )
    )

    override suspend fun deleteSmartPlaylist(id: Long) = podcastDao.deleteSmartPlaylist(id)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getSmartPlaylistEpisodesPaged(playlist: SmartPlaylist): Flow<PagingData<Episode>> =
        flow { emit(podcastsOf(playlist.category)) }.flatMapLatest { ids ->
            pagedEpisodes {
                episodeDao.smartPlaylistPagingSource(
                    anyPodcast = playlist.category == null,
                    podcastIds = ids,
                    onlyUnplayed = playlist.unplayedOnly,
                    onlyDownloaded = playlist.downloadedOnly,
                    maxSeconds = playlist.maxSeconds,
                )
            }
        }

    override suspend fun getSmartPlaylistEpisodes(playlist: SmartPlaylist): List<Episode> =
        episodeDao.smartPlaylist(
            anyPodcast = playlist.category == null,
            podcastIds = podcastsOf(playlist.category),
            onlyUnplayed = playlist.unplayedOnly,
            onlyDownloaded = playlist.downloadedOnly,
            maxSeconds = playlist.maxSeconds,
        ).map { it.toDomain() }

    /** The podcasts of [category], read from their feeds' categories the way the library's chips read them. */
    private suspend fun podcastsOf(category: PodcastCategory?): List<String> {
        if (category == null) return emptyList()
        return podcastDao.getAll().first().map { it.toDomain() }.filter { category in it.topCategories }.map { it.id }
    }

    private val SmartPlaylist.maxSeconds: Long get() = (maxMinutes ?: 0) * SECONDS_PER_MINUTE

    override suspend fun getEpisodesWithHistory(): List<Episode> = episodeDao.getWithHistory().map { it.toDomain() }

    override suspend fun getSyncedEpisodes(): List<Episode> = episodeDao.getChanged().map { it.toDomain() }

    override fun lastEpisodeChange(): Flow<Long> = episodeDao.lastChange()

    override suspend fun applyEpisodeState(id: String, played: Boolean, position: Long, updatedAt: Long) =
        episodeDao.updatePlayback(id, played, position, updatedAt)

    override suspend fun getRemovedPodcasts(): Map<String, Long> =
        podcastDao.removed().associate { it.feedUrl to it.removedAt }

    override fun getEpisodes(podcastId: String): Flow<List<Episode>> {
        return episodeDao.getByPodcast(podcastId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEpisodesPaged(
        podcastId: String,
        filter: EpisodeFilter,
        oldestFirst: Boolean,
    ): Flow<PagingData<Episode>> =
        pagedEpisodes {
            episodeDao.pagingSourceByPodcast(
                podcastId,
                onlyUnplayed = filter == EpisodeFilter.UNPLAYED,
                onlyDownloaded = filter == EpisodeFilter.DOWNLOADED,
                oldestFirst = oldestFirst,
            )
        }

    override suspend fun setPodcastAutoDownload(podcastId: String, count: Int) =
        podcastDao.setAutoDownload(podcastId, count)

    override suspend fun setPodcastNotifications(podcastId: String, enabled: Boolean) =
        podcastDao.setNotifications(podcastId, enabled)

    override suspend fun getNewEpisodes(): List<Episode> = episodeDao.getNew().map { it.toDomain() }

    override suspend fun setPodcastPlayback(podcastId: String, playback: PodcastPlayback) =
        podcastDao.setPlayback(podcastId, playback.speed, playback.skipIntroSeconds, playback.skipOutroSeconds)

    override suspend fun setEpisodeOrder(podcastId: String, oldestFirst: Boolean) =
        podcastDao.setOldestFirst(podcastId, oldestFirst)

    override fun searchEpisodes(query: String): Flow<List<Episode>> {
        val match = searchMatch(query)
        if (match.isEmpty()) return flowOf(emptyList())
        return episodeDao.search(match).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchEpisodesPaged(query: String?, filter: EpisodeListFilter): Flow<PagingData<Episode>> =
        pagedEpisodes {
            episodeDao.searchPagingSource(
                searchMatch(query.orEmpty()),
                onlyInProgress = filter == EpisodeListFilter.IN_PROGRESS,
                onlyDownloaded = filter == EpisodeListFilter.DOWNLOADED,
                onlyNew = filter == EpisodeListFilter.NEW,
                onlySaved = filter == EpisodeListFilter.SAVED,
                history = filter == EpisodeListFilter.HISTORY,
            )
        }

    private fun pagedEpisodes(source: () -> PagingSource<Int, EpisodeEntity>): Flow<PagingData<Episode>> =
        Pager(config = PagingConfig(pageSize = PAGE_SIZE), pagingSourceFactory = source)
            .flow
            .map { page -> page.map { it.toDomain() } }

    override suspend fun setEpisodeSaved(id: String, saved: Boolean) = episodeDao.setSaved(id, saved)

    override fun getDownloadedEpisodes(): Flow<List<Episode>> {
        return episodeDao.getDownloaded().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEpisodeCount(podcastId: String): Flow<Int> = episodeDao.countByPodcast(podcastId)

    override fun getLatestUnplayedEpisode(podcastId: String): Flow<Episode?> =
        episodeDao.getLatestUnplayed(podcastId).map { it?.toDomain() }

    override fun getInProgressEpisodes(): Flow<List<Episode>> =
        episodeDao.getInProgress(IN_PROGRESS_LIMIT).map { entities -> entities.map { it.toDomain() } }

    override fun getUnplayedEpisodes(): Flow<List<Episode>> {
        return episodeDao.getUnplayed().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateEpisodeProgress(id: String, progress: Long) {
        episodeDao.updatePlayback(id, false, progress, now())
    }

    override suspend fun markEpisodeAsPlayed(id: String) {
        episodeDao.updatePlayback(id, true, 0L, now())
    }

    override suspend fun dismissNewEpisode(id: String) = episodeDao.dismissNew(id)

    override suspend fun markEpisodeStarted(id: String, at: Long) = episodeDao.markStarted(id, at)

    override suspend fun clearHistory() = episodeDao.clearHistory()

    override suspend fun dismissAllNewEpisodes() = episodeDao.dismissAllNew()

    override suspend fun markEpisodeAsUnplayed(id: String) {
        episodeDao.updatePlayback(id, false, 0L, now())
    }

    override suspend fun saveFeed(podcast: Podcast, episodes: List<Episode>) {
        AppLogger.d(TAG, "Saving podcast ${podcast.title} with ${episodes.size} episodes")
        episodeDao.saveFeed(podcast.toEntity(), episodes.map { it.toEntity() })
    }

    /** The episodes go with the podcast through the foreign key's ON DELETE CASCADE; the removal is kept (19.9b). */
    override suspend fun deletePodcast(id: String) {
        AppLogger.i(TAG, "Deleting podcast id: $id")
        podcastDao.remove(id, now())
    }

    override suspend fun markOlderEpisodesAsPlayed(podcastId: String, publishDate: Long) {
        episodeDao.markOlderAsPlayed(podcastId, publishDate, now())
    }

    override suspend fun markOlderEpisodesAsUnplayed(podcastId: String, publishDate: Long) {
        episodeDao.markOlderAsUnplayed(podcastId, publishDate, now())
    }
}
