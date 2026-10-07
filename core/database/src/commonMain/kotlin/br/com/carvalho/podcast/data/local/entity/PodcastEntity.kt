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
)

/** The columns a feed owns: refreshing it updates these and keeps what the library set (subscribedAt). */
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
)

fun PodcastEntity.feedFields() = PodcastFeedFields(
    id, title, description, imageUrl, author, language, categories, feedUrl, siteUrl, lastUpdated, etag, lastModified
)
