package br.com.carvalho.podcast.data.local.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import br.com.carvalho.podcast.data.local.entity.PodcastFeedFields
import br.com.carvalho.podcast.data.local.entity.feedFields
import kotlinx.coroutines.flow.Flow

import androidx.room3.Transaction

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishDate DESC")
    fun getByPodcast(podcastId: String): Flow<List<EpisodeEntity>>

    // Room's paging sources reload by themselves when the table changes (played, downloaded, refreshed).
    @Query("""
        SELECT * FROM episodes
        WHERE podcastId = :podcastId
        AND (:onlyUnplayed = 0 OR isPlayed = 0)
        AND (:onlyDownloaded = 0 OR isDownloaded = 1)
        ORDER BY publishDate DESC
    """)
    fun pagingSourceByPodcast(
        podcastId: String,
        onlyUnplayed: Boolean,
        onlyDownloaded: Boolean,
    ): PagingSource<Int, EpisodeEntity>

    @Query("""
        SELECT * FROM episodes
        WHERE :query = '' OR title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'
        ORDER BY publishDate DESC
    """)
    fun searchPagingSource(query: String): PagingSource<Int, EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId AND publishDate >= :publishDate ORDER BY publishDate")
    suspend fun getSince(podcastId: String, publishDate: Long): List<EpisodeEntity>

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

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query("""
        UPDATE episodes
        SET podcastTitle = :podcastTitle, title = :title, description = :description, audioUrl = :audioUrl,
            imageUrl = :imageUrl, duration = :duration, publishDate = :publishDate
        WHERE id = :id
    """)
    @Suppress("LongParameterList") // one parameter per column the feed owns
    suspend fun updateFeedFields(
        id: String,
        podcastTitle: String?,
        title: String,
        description: String?,
        audioUrl: String,
        imageUrl: String?,
        duration: Long,
        publishDate: Long,
    )

    // The podcast row is written here so [saveFeed] can save it with its episodes in one transaction.
    // Insert-or-update instead of @Upsert: Room's upsert tells a conflict apart by the exception's message.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPodcastIfNew(podcast: PodcastEntity): Long

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM podcasts")
    suspend fun nextPodcastPosition(): Int

    @Update(entity = PodcastEntity::class)
    suspend fun updatePodcast(fields: PodcastFeedFields)

    /** A feed read from the network: the podcast and its episodes, all or nothing. */
    @Transaction
    suspend fun saveFeed(podcast: PodcastEntity, episodes: List<EpisodeEntity>) {
        // A new podcast goes to the end of the custom order; a known one keeps its place.
        if (insertPodcastIfNew(podcast.copy(position = nextPodcastPosition())) == NOT_INSERTED) {
            updatePodcast(podcast.feedFields())
        }
        deleteWithoutAudio(podcast.id)
        saveFromFeed(episodes)
    }

    // Before 15.4 the parser saved items without audio (blog posts) as episodes.
    @Query("DELETE FROM episodes WHERE podcastId = :podcastId AND audioUrl = ''")
    suspend fun deleteWithoutAudio(podcastId: String)

    /**
     * Saves episodes read from a feed: new ones are inserted, known ones get the feed's text, audio and dates while
     * keeping what the user did (played, position, downloaded).
     */
    @Transaction
    suspend fun saveFromFeed(episodes: List<EpisodeEntity>) {
        insertAll(episodes)
        episodes.forEach {
            updateFeedFields(
                it.id, it.podcastTitle, it.title, it.description, it.audioUrl, it.imageUrl, it.duration, it.publishDate
            )
        }
    }

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

    /** Records the downloaded file of an episode, or that it has none ([fileName] null). */
    @Query("UPDATE episodes SET downloadFile = :fileName, isDownloaded = (:fileName IS NOT NULL) WHERE id = :id")
    suspend fun updateDownloadFile(id: String, fileName: String?)

    @Transaction
    @Query("""
        UPDATE episodes
        SET isPlayed = 1, playbackPosition = 0
        WHERE podcastId = :podcastId AND publishDate <= :publishDate
    """)
    suspend fun markOlderAsPlayed(podcastId: String, publishDate: Long)

    private companion object {
        /** Row id an IGNOREd insert returns. */
        const val NOT_INSERTED = -1L
    }
}
