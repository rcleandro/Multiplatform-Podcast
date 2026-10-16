package br.com.carvalho.podcast.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import br.com.carvalho.podcast.data.local.entity.LibraryRow
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import br.com.carvalho.podcast.data.local.entity.RemovedPodcastEntity
import br.com.carvalho.podcast.data.local.entity.SmartPlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions") // one function per query
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

    @Query(
        "INSERT OR REPLACE INTO removed_podcasts (feedUrl, removedAt) SELECT feedUrl, :at FROM podcasts WHERE id = :id"
    )
    suspend fun rememberRemoved(id: String, at: Long)

    /** Takes the podcast out and keeps when (19.9b), so a synced device does not bring it back. */
    @Transaction
    suspend fun remove(id: String, at: Long) {
        rememberRemoved(id, at)
        deleteById(id)
    }

    @Query("SELECT * FROM removed_podcasts")
    suspend fun removed(): List<RemovedPodcastEntity>

    @Query(
        "UPDATE podcasts SET playbackSpeed = :speed, skipIntroSeconds = :introSeconds, " +
            "skipOutroSeconds = :outroSeconds WHERE id = :id"
    )
    suspend fun setPlayback(id: String, speed: Float?, introSeconds: Int, outroSeconds: Int)

    @Query("UPDATE podcasts SET autoDownloadCount = :count WHERE id = :id")
    suspend fun setAutoDownload(id: String, count: Int)

    @Query("UPDATE podcasts SET notifyNewEpisodes = :enabled WHERE id = :id")
    suspend fun setNotifications(id: String, enabled: Boolean)

    @Query("UPDATE podcasts SET oldestFirst = :oldestFirst WHERE id = :id")
    suspend fun setOldestFirst(id: String, oldestFirst: Boolean)

    @Query("UPDATE podcasts SET position = :position WHERE id = :id")
    suspend fun setPosition(id: String, position: Int)

    /** The custom order, all or nothing: [ids] from first to last. */
    @Transaction
    suspend fun reorder(ids: List<String>) {
        ids.forEachIndexed { position, id -> setPosition(id, position) }
    }

    /** The saved filters of the Episodes tab (19.4), in the order they were made. */
    @Query("SELECT * FROM smart_playlists ORDER BY id")
    fun smartPlaylists(): Flow<List<SmartPlaylistEntity>>

    @Upsert
    suspend fun saveSmartPlaylist(playlist: SmartPlaylistEntity)

    @Query("DELETE FROM smart_playlists WHERE id = :id")
    suspend fun deleteSmartPlaylist(id: Long)
}
