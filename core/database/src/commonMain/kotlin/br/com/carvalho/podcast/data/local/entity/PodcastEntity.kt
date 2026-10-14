package br.com.carvalho.podcast.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val author: String?,
    val language: String?,
    val categories: String,
    val feedUrl: String,
    val siteUrl: String?,
    val lastUpdated: Long,
    val isSubscribed: Boolean,
    /** `ETag` and `Last-Modified` of the copy saved last (FeedVersion). */
    val etag: String? = null,
    val lastModified: String? = null,
    /** When the podcast entered the library; the feed never rewrites it (see PodcastFeedFields). */
    @ColumnInfo(defaultValue = "0") val subscribedAt: Long = 0,
    /** Place in the custom order; the feed never rewrites it either. */
    @ColumnInfo(defaultValue = "0") val position: Int = 0,
    /** `itunes:explicit` on the channel, added in version 11 (18.5). */
    @ColumnInfo(defaultValue = "0") val explicit: Boolean = false,
    /** `itunes:type` serial on the channel, added in version 13 (18.10). */
    @ColumnInfo(defaultValue = "0") val serial: Boolean = false,
    /** The episode order the user chose, added in version 13; `null` follows [serial]. The feed never rewrites it. */
    val oldestFirst: Boolean? = null,
    /** How this podcast plays (18.14), added in version 17; null speed follows the general one. */
    val playbackSpeed: Float? = null,
    @ColumnInfo(defaultValue = "0") val skipIntroSeconds: Int = 0,
    @ColumnInfo(defaultValue = "0") val skipOutroSeconds: Int = 0,
    /** New episodes found in the background notify (18.18), added in version 18; on by default. */
    @ColumnInfo(defaultValue = "1") val notifyNewEpisodes: Boolean = true,
)

/** The columns a feed owns: refreshing it updates these and keeps what the library set (subscribedAt, position). */
data class PodcastFeedFields(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val author: String?,
    val language: String?,
    val categories: String,
    val feedUrl: String,
    val siteUrl: String?,
    val lastUpdated: Long,
    val etag: String?,
    val lastModified: String?,
    val explicit: Boolean,
    val serial: Boolean,
)

fun PodcastEntity.feedFields() = PodcastFeedFields(
    id, title, description, imageUrl, author, language, categories, feedUrl, siteUrl, lastUpdated, etag, lastModified,
    explicit, serial,
)
