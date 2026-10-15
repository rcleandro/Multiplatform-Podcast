package br.com.carvalho.podcast.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("podcastId"),
        Index("publishDate")
    ]
)
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val podcastId: String,
    val podcastTitle: String?,
    val title: String,
    val description: String?,
    val audioUrl: String,
    val imageUrl: String?,
    val duration: Long,
    val publishDate: Long,
    val isPlayed: Boolean,
    val playbackPosition: Long,
    val isDownloaded: Boolean,
    val fileSize: Long?,
    /** The downloaded audio's name inside the downloads folder; the folder's absolute path can change on iOS. */
    val downloadFile: String? = null,
    /** The episode's page on the podcast's site (`<link>` in the feed), added in version 10. */
    val link: String? = null,
    /** `itunes:explicit` on the item, added in version 11 (18.5). */
    @ColumnInfo(defaultValue = "0") val explicit: Boolean = false,
    /** In "New" until heard or dismissed (18.7), added in version 12; the feed never rewrites it. */
    @ColumnInfo(defaultValue = "0") val isNew: Boolean = false,
    /** `itunes:season` and `itunes:episodeType` (`trailer`, `bonus`; null for a regular one), added in version 14. */
    val season: Int? = null,
    val episodeType: String? = null,
    /** In "Saved" (18.12), added in version 15; the feed never rewrites it. */
    @ColumnInfo(defaultValue = "0") val isSaved: Boolean = false,
    /** When it last started playing, for "History" (18.12), added in version 16; the feed never rewrites it. */
    val lastPlayedAt: Long? = null,
    /** When its download finished (18.19), added in version 20; downloads from before have none. */
    val downloadedAt: Long? = null,
)
