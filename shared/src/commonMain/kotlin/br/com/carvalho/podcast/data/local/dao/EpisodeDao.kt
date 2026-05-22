package br.com.carvalho.podcast.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishDate DESC")
    fun getByPodcast(podcastId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getByPodcastPaged(podcastId: String, limit: Int, offset: Int): List<EpisodeEntity>

    @Query("SELECT * FROM episodes ORDER BY publishDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllPaged(limit: Int, offset: Int): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getById(id: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE isPlayed = 0 ORDER BY publishDate DESC")
    fun getUnplayed(): Flow<List<EpisodeEntity>>

    @Query("""
        SELECT * FROM episodes
        WHERE title LIKE '%' || :query || '%'
        OR description LIKE '%' || :query || '%'
        ORDER BY publishDate DESC
    """)
    fun search(query: String): Flow<List<EpisodeEntity>>

    @Query("""
        SELECT * FROM episodes
        WHERE title LIKE '%' || :query || '%'
        OR description LIKE '%' || :query || '%'
        ORDER BY publishDate DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchPaged(query: String, limit: Int, offset: Int): List<EpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM episodes WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Query("""
        UPDATE episodes
        SET isPlayed = :played, playbackPosition = :position
        WHERE id = :id
    """)
    suspend fun updatePlayback(id: String, played: Boolean, position: Long)

    @Query("SELECT * FROM episodes WHERE isDownloaded = 1 ORDER BY publishDate DESC")
    fun getDownloaded(): Flow<List<EpisodeEntity>>

    @Query("SELECT COUNT(*) FROM episodes WHERE podcastId = :podcastId AND isPlayed = 0")
    fun getUnplayedCount(podcastId: String): Flow<Int>

    @Query("DELETE FROM episodes WHERE podcastId = :podcastId")
    suspend fun deleteByPodcast(podcastId: String)

    @Query("UPDATE episodes SET isDownloaded = :downloaded WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, downloaded: Boolean)

    @Query("""
        UPDATE episodes
        SET isPlayed = 1, playbackPosition = 0
        WHERE podcastId = :podcastId AND publishDate <= :publishDate
    """)
    suspend fun markOlderAsPlayed(podcastId: String, publishDate: Long)
}
