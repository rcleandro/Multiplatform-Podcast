package br.com.carvalho.podcast.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Delete
import androidx.room3.Upsert
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcasts ORDER BY title ASC")
    fun getAll(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE id = :id")
    suspend fun getById(id: String): PodcastEntity?

    @Query("SELECT * FROM podcasts WHERE id = :id")
    fun getByIdFlow(id: String): Flow<PodcastEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM podcasts WHERE feedUrl = :feedUrl)")
    suspend fun existsByFeedUrl(feedUrl: String): Boolean

    @Upsert
    suspend fun insert(podcast: PodcastEntity)

    @Delete
    suspend fun delete(podcast: PodcastEntity)

    @Query("DELETE FROM podcasts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE podcasts SET lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateLastUpdated(id: String, timestamp: Long)
}
