package br.com.carvalho.podcast.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import br.com.carvalho.podcast.data.local.entity.BookmarkEntity
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.ListeningEntity
import br.com.carvalho.podcast.data.local.entity.PlaybackStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions") // one function per query
interface PlaybackStateDao {
    @Query("SELECT * FROM playback_state WHERE id = 1")
    suspend fun get(): PlaybackStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(state: PlaybackStateEntity)

    /** The queue's episodes as they are now, in queue order. */
    @Query("SELECT episodes.* FROM queue_items JOIN episodes ON episodes.id = queue_items.episodeId ORDER BY position")
    suspend fun getQueue(): List<EpisodeEntity>

    @Query("DELETE FROM queue_items")
    suspend fun clearQueue()

    /** Adds the episode only if it is in the library, where the foreign key would otherwise fail the write. */
    @Query("INSERT INTO queue_items (position, episodeId) SELECT :position, id FROM episodes WHERE id = :episodeId")
    suspend fun insertQueueItem(position: Int, episodeId: String)

    @Transaction
    suspend fun replaceQueue(episodeIds: List<String>) {
        clearQueue()
        episodeIds.forEachIndexed { position, id -> insertQueueItem(position, id) }
    }

    /** An episode's bookmarks (19.3), in the order they come in the episode. */
    @Query("SELECT * FROM bookmarks WHERE episodeId = :episodeId ORDER BY positionMs, id")
    fun bookmarks(episodeId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY episodeId, positionMs, id")
    suspend fun allBookmarks(): List<BookmarkEntity>

    @Insert
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertListening(row: ListeningEntity)

    @Query(
        """
        UPDATE listening SET listenedMs = listenedMs + :listenedMs, savedMs = savedMs + :savedMs
        WHERE day = :day AND podcastId = :podcastId
        """
    )
    suspend fun addToListening(day: Long, podcastId: String, listenedMs: Long, savedMs: Long)

    /** Adds to the day's totals for the podcast (19.5), starting them at zero. */
    @Transaction
    suspend fun addListening(day: Long, podcastId: String, listenedMs: Long, savedMs: Long) {
        insertListening(ListeningEntity(day, podcastId, 0, 0))
        addToListening(day, podcastId, listenedMs, savedMs)
    }

    @Query("SELECT * FROM listening")
    fun listening(): Flow<List<ListeningEntity>>
}
