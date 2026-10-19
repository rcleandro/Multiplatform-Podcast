package br.com.carvalho.podcast.data.local.entity

import androidx.room3.Entity

/**
 * Time heard on one day for one podcast (19.5), added in version 24. No foreign key: the totals outlive a podcast
 * removed from the library. [day] is days since 1970-01-01 in the device's time zone.
 */
@Entity(tableName = "listening", primaryKeys = ["day", "podcastId"])
data class ListeningEntity(
    val day: Long,
    val podcastId: String,
    val listenedMs: Long,
    val savedMs: Long,
)
