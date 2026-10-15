package br.com.carvalho.podcast.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** A saved filter of episodes (19.4), added in version 23; [category] is the category's enum name. */
@Entity(tableName = "smart_playlists")
data class SmartPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String?,
    val unplayedOnly: Boolean,
    val downloadedOnly: Boolean,
    val maxMinutes: Int?,
)
