package br.com.carvalho.podcast.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.PlaybackStateEntity

@Dao
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
}
