package br.com.carvalho.podcast.data.repository

import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.data.local.dao.PlaybackStateDao
import br.com.carvalho.podcast.data.local.dao.PodcastDao
import br.com.carvalho.podcast.data.local.entity.BookmarkEntity
import br.com.carvalho.podcast.data.local.entity.PlaybackStateEntity
import br.com.carvalho.podcast.data.mapper.toDomain
import br.com.carvalho.podcast.domain.model.Bookmark
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.ListeningStats
import br.com.carvalho.podcast.domain.model.PodcastListening
import br.com.carvalho.podcast.domain.model.WeekListening
import br.com.carvalho.podcast.domain.repository.PlaybackState
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class PlayerRepositoryImpl(
    private val playbackStateDao: PlaybackStateDao,
    private val podcastDao: PodcastDao,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val now: () -> Long = ::getCurrentTimestamp,
) : PlayerRepository {

    /** The queue last written; progress is saved every few seconds, the queue only when it changes. */
    private var savedQueueIds: List<String>? = null

    override suspend fun savePlaybackState(episodeId: String?, position: Long, speed: Float, queue: List<Episode>) {
        playbackStateDao.save(PlaybackStateEntity(episodeId = episodeId, position = position, speed = speed))
        val queueIds = queue.map { it.id }
        if (queueIds != savedQueueIds) {
            playbackStateDao.replaceQueue(queueIds)
            savedQueueIds = queueIds
        }
    }

    override suspend fun getSavedPlaybackState(): PlaybackState? {
        val state = playbackStateDao.get() ?: return null
        return PlaybackState(
            episodeId = state.episodeId,
            position = state.position,
            speed = state.speed,
            queue = playbackStateDao.getQueue().map { it.toDomain() },
        )
    }

    override fun bookmarks(episodeId: String): Flow<List<Bookmark>> =
        playbackStateDao.bookmarks(episodeId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun allBookmarks(): List<Bookmark> = playbackStateDao.allBookmarks().map { it.toDomain() }

    private fun BookmarkEntity.toDomain() = Bookmark(id, episodeId, positionMs, note, createdAt)

    override suspend fun addBookmark(episodeId: String, positionMs: Long, note: String) {
        val bookmark = BookmarkEntity(
            episodeId = episodeId,
            positionMs = positionMs,
            note = note.trim(),
            createdAt = getCurrentTimestamp(),
        )
        playbackStateDao.insertBookmark(bookmark)
    }

    override suspend fun deleteBookmark(id: Long) = playbackStateDao.deleteBookmark(id)

    override suspend fun addListening(podcastId: String, at: Long, listenedMs: Long, savedMs: Long) =
        playbackStateDao.addListening(epochDay(at), podcastId, listenedMs, savedMs)

    override fun listeningStats(): Flow<ListeningStats> =
        combine(playbackStateDao.listening(), podcastDao.getAll()) { rows, podcasts ->
            val thisMonday = epochDay(now()).let { it - LocalDate.fromEpochDays(it).dayOfWeek.isoDayNumber + 1 }
            val weeks = (ListeningStats.WEEKS - 1 downTo 0).map { back ->
                val start = thisMonday - back * DAYS_PER_WEEK
                val days = start until start + DAYS_PER_WEEK
                WeekListening(start, rows.filter { it.day in days }.sumOf { it.listenedMs })
            }
            val byPodcast = rows.groupBy { it.podcastId }.mapValues { (_, days) -> days.sumOf { it.listenedMs } }
            ListeningStats(
                weeks = weeks,
                podcasts = podcasts.mapNotNull { podcast ->
                    byPodcast[podcast.id]?.let { PodcastListening(podcast.toDomain(), it) }
                }.sortedByDescending { it.listenedMs },
                totalListenedMs = rows.sumOf { it.listenedMs },
                totalSavedMs = rows.sumOf { it.savedMs },
            )
        }

    private fun epochDay(at: Long): Long =
        Instant.fromEpochMilliseconds(at).toLocalDateTime(timeZone()).date.toEpochDays()

    private companion object {
        const val DAYS_PER_WEEK = 7
    }
}
