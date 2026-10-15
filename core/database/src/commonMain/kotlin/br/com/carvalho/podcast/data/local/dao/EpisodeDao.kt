package br.com.carvalho.podcast.data.local.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import br.com.carvalho.podcast.data.local.entity.PodcastFeedFields
import br.com.carvalho.podcast.data.local.entity.feedFields
import kotlinx.coroutines.flow.Flow

/** A smart playlist's episodes (19.4), newest first; a duration of 0 is unknown and fails a length limit. */
private const val SMART_PLAYLIST_QUERY = """
    SELECT * FROM episodes
    WHERE (:anyPodcast OR podcastId IN (:podcastIds))
    AND (:onlyUnplayed = 0 OR isPlayed = 0)
    AND (:onlyDownloaded = 0 OR isDownloaded = 1)
    AND (:maxSeconds = 0 OR (duration > 0 AND duration <= :maxSeconds))
    ORDER BY publishDate DESC
"""

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
@Suppress("TooManyFunctions") // one function per query
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishDate DESC")
    fun getByPodcast(podcastId: String): Flow<List<EpisodeEntity>>

    // Room's paging sources reload by themselves when the table changes (played, downloaded, refreshed).
    @Query(
        """
        SELECT * FROM episodes
        WHERE podcastId = :podcastId
        AND (:onlyUnplayed = 0 OR isPlayed = 0)
        AND (:onlyDownloaded = 0 OR isDownloaded = 1)
        ORDER BY CASE WHEN :oldestFirst THEN publishDate END ASC, publishDate DESC
    """
    )
    fun pagingSourceByPodcast(
        podcastId: String,
        onlyUnplayed: Boolean,
        onlyDownloaded: Boolean,
        oldestFirst: Boolean = false,
    ): PagingSource<Int, EpisodeEntity>

    @Query(
        """
        SELECT * FROM episodes
        WHERE (:query = '' OR title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        AND (:onlyInProgress = 0 OR (playbackPosition > 0 AND isPlayed = 0))
        AND (:onlyDownloaded = 0 OR isDownloaded = 1)
        AND (:onlyNew = 0 OR (isNew = 1 AND isPlayed = 0))
        AND (:onlySaved = 0 OR isSaved = 1)
        AND (:history = 0 OR lastPlayedAt IS NOT NULL)
        ORDER BY CASE WHEN :history THEN lastPlayedAt END DESC, publishDate DESC
    """
    )
    @Suppress("LongParameterList") // one flag per filter of the Episodes tab
    fun searchPagingSource(
        query: String,
        onlyInProgress: Boolean = false,
        onlyDownloaded: Boolean = false,
        onlyNew: Boolean = false,
        onlySaved: Boolean = false,
        history: Boolean = false,
    ): PagingSource<Int, EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getById(id: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE isPlayed = 0 ORDER BY publishDate DESC")
    fun getUnplayed(): Flow<List<EpisodeEntity>>

    // The one heard last first; episodes started before version 16 have no date and follow, newest first.
    @Query(
        """
        SELECT * FROM episodes WHERE playbackPosition > 0 AND isPlayed = 0
        ORDER BY lastPlayedAt DESC, publishDate DESC LIMIT :limit
    """
    )
    fun getInProgress(limit: Int): Flow<List<EpisodeEntity>>

    @Query(
        """
        SELECT * FROM episodes
        WHERE title LIKE '%' || :query || '%'
        OR description LIKE '%' || :query || '%'
        ORDER BY publishDate DESC
    """
    )
    fun search(query: String): Flow<List<EpisodeEntity>>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query(
        """
        UPDATE episodes
        SET podcastTitle = :podcastTitle, title = :title, description = :description, audioUrl = :audioUrl,
            imageUrl = :imageUrl, duration = :duration, publishDate = :publishDate, link = :link,
            explicit = :explicit, season = :season, episodeType = :episodeType, chaptersUrl = :chaptersUrl,
            transcriptUrl = :transcriptUrl, transcriptType = :transcriptType
        WHERE id = :id
    """
    )
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
        link: String?,
        explicit: Boolean,
        season: Int?,
        episodeType: String?,
        chaptersUrl: String?,
        transcriptUrl: String?,
        transcriptType: String?,
    )

    // The podcast row is written here so [saveFeed] can save it with its episodes in one transaction.
    // Insert-or-update instead of @Upsert: Room's upsert tells a conflict apart by the exception's message.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPodcastIfNew(podcast: PodcastEntity): Long

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM podcasts")
    suspend fun nextPodcastPosition(): Int

    @Update(entity = PodcastEntity::class)
    suspend fun updatePodcast(fields: PodcastFeedFields)

    /**
     * A feed read from the network: the podcast and its episodes, all or nothing. What a refresh brings goes to "New"
     * (18.7); a podcast just subscribed to brings only its latest episode there, not its whole archive.
     */
    @Transaction
    suspend fun saveFeed(podcast: PodcastEntity, episodes: List<EpisodeEntity>) {
        // A new podcast goes to the end of the custom order; a known one keeps its place.
        val subscribed = insertPodcastIfNew(podcast.copy(position = nextPodcastPosition())) != NOT_INSERTED
        if (!subscribed) updatePodcast(podcast.feedFields())
        deleteWithoutAudio(podcast.id)
        val known = idsOf(podcast.id).toSet()
        saveFromFeed(episodes)
        val arrived = episodes.filter { it.id !in known }
        val fresh = if (subscribed) listOfNotNull(arrived.maxByOrNull { it.publishDate }) else arrived
        // In chunks: SQLite caps the variables of one statement.
        fresh.map { it.id }.chunked(MAX_QUERY_ARGS).forEach { markNew(it) }
    }

    @Query("SELECT id FROM episodes WHERE podcastId = :podcastId")
    suspend fun idsOf(podcastId: String): List<String>

    @Query("UPDATE episodes SET isNew = 1 WHERE id IN (:ids)")
    suspend fun markNew(ids: List<String>)

    @Query("UPDATE episodes SET isNew = 0, lastPlayedAt = :at WHERE id = :id")
    suspend fun markStarted(id: String, at: Long)

    @Query("UPDATE episodes SET lastPlayedAt = NULL WHERE lastPlayedAt IS NOT NULL")
    suspend fun clearHistory()

    @Query("SELECT * FROM episodes WHERE isNew = 1 AND isPlayed = 0 ORDER BY publishDate DESC")
    suspend fun getNew(): List<EpisodeEntity>

    @Query(SMART_PLAYLIST_QUERY)
    @Suppress("LongParameterList") // one parameter per condition of a smart playlist
    fun smartPlaylistPagingSource(
        anyPodcast: Boolean,
        podcastIds: List<String>,
        onlyUnplayed: Boolean,
        onlyDownloaded: Boolean,
        maxSeconds: Long,
    ): PagingSource<Int, EpisodeEntity>

    /** The same episodes all at once, for "Play all". */
    @Query(SMART_PLAYLIST_QUERY)
    @Suppress("LongParameterList") // one parameter per condition of a smart playlist
    suspend fun smartPlaylist(
        anyPodcast: Boolean,
        podcastIds: List<String>,
        onlyUnplayed: Boolean,
        onlyDownloaded: Boolean,
        maxSeconds: Long,
    ): List<EpisodeEntity>

    @Query(
        "SELECT * FROM episodes WHERE isPlayed = 1 OR playbackPosition > 0 OR isSaved = 1 OR lastPlayedAt IS NOT NULL"
    )
    suspend fun getWithHistory(): List<EpisodeEntity>

    @Query("UPDATE episodes SET isSaved = :saved WHERE id = :id")
    suspend fun setSaved(id: String, saved: Boolean)

    /** Out of "New" (18.7): dismissed, or started. */
    @Query("UPDATE episodes SET isNew = 0 WHERE id = :id")
    suspend fun dismissNew(id: String)

    @Query("UPDATE episodes SET isNew = 0 WHERE isNew = 1")
    suspend fun dismissAllNew()

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
                it.id,
                it.podcastTitle,
                it.title,
                it.description,
                it.audioUrl,
                it.imageUrl,
                it.duration,
                it.publishDate,
                it.link,
                it.explicit,
                it.season,
                it.episodeType,
                it.chaptersUrl,
                it.transcriptUrl,
                it.transcriptType,
            )
        }
    }

    @Query("SELECT EXISTS(SELECT 1 FROM episodes WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Query(
        """
        UPDATE episodes
        SET isPlayed = :played, playbackPosition = :position,
            isNew = CASE WHEN :played THEN 0 ELSE isNew END
        WHERE id = :id
    """
    )
    suspend fun updatePlayback(id: String, played: Boolean, position: Long)

    @Query("SELECT * FROM episodes WHERE isDownloaded = 1 ORDER BY publishDate DESC")
    fun getDownloaded(): Flow<List<EpisodeEntity>>

    @Query("SELECT COUNT(*) FROM episodes WHERE podcastId = :podcastId")
    fun countByPodcast(podcastId: String): Flow<Int>

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId AND isPlayed = 0 ORDER BY publishDate DESC LIMIT 1")
    fun getLatestUnplayed(podcastId: String): Flow<EpisodeEntity?>

    @Query("SELECT COUNT(*) FROM episodes WHERE podcastId = :podcastId AND isPlayed = 0")
    fun getUnplayedCount(podcastId: String): Flow<Int>

    /** Records the downloaded file of an episode and when it finished, or that it has none ([fileName] null). */
    @Query(
        "UPDATE episodes SET downloadFile = :fileName, isDownloaded = (:fileName IS NOT NULL), " +
            "downloadedAt = :downloadedAt WHERE id = :id"
    )
    suspend fun updateDownloadFile(id: String, fileName: String?, downloadedAt: Long? = null)

    @Transaction
    @Query(
        """
        UPDATE episodes
        SET isPlayed = 1, playbackPosition = 0, isNew = 0
        WHERE podcastId = :podcastId AND publishDate <= :publishDate
    """
    )
    suspend fun markOlderAsPlayed(podcastId: String, publishDate: Long)

    @Query(
        """
        UPDATE episodes
        SET isPlayed = 0, playbackPosition = 0
        WHERE podcastId = :podcastId AND publishDate <= :publishDate
    """
    )
    suspend fun markOlderAsUnplayed(podcastId: String, publishDate: Long)

    private companion object {
        /** Row id an IGNOREd insert returns. */
        const val NOT_INSERTED = -1L
        const val MAX_QUERY_ARGS = 500
    }
}
