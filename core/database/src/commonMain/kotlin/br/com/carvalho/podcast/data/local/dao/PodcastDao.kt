package br.com.carvalho.podcast.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import br.com.carvalho.podcast.data.local.entity.LibraryRow
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcasts ORDER BY title ASC")
    fun getAll(): Flow<List<PodcastEntity>>

    // Correlated subqueries use the index on episodes.podcastId; one query instead of one per podcast.
    @Query(
        """
        SELECT p.*,
            (SELECT COUNT(*) FROM episodes e WHERE e.podcastId = p.id AND e.isPlayed = 0) AS unplayedCount,
            (SELECT MAX(e.publishDate) FROM episodes e WHERE e.podcastId = p.id) AS latestEpisodeDate
        FROM podcasts p
        ORDER BY p.title COLLATE NOCASE
        """
    )
    fun getLibrary(): Flow<List<LibraryRow>>

    @Query("SELECT * FROM podcasts WHERE id = :id")
    suspend fun getById(id: String): PodcastEntity?

    @Query("SELECT * FROM podcasts WHERE id = :id")
    fun getByIdFlow(id: String): Flow<PodcastEntity?>

    @Upsert
    suspend fun insert(podcast: PodcastEntity)

    @Query("DELETE FROM podcasts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE podcasts SET position = :position WHERE id = :id")
    suspend fun setPosition(id: String, position: Int)

    /** The custom order, all or nothing: [ids] from first to last. */
    @Transaction
    suspend fun reorder(ids: List<String>) {
        ids.forEachIndexed { position, id -> setPosition(id, position) }
    }
}
