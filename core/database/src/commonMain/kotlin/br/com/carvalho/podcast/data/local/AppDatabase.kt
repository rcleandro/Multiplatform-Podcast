package br.com.carvalho.podcast.data.local

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import br.com.carvalho.podcast.data.local.dao.EpisodeDao
import br.com.carvalho.podcast.data.local.dao.PodcastDao
import br.com.carvalho.podcast.data.local.dao.PlaybackStateDao
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import br.com.carvalho.podcast.data.local.entity.PlaybackStateEntity

@Database(
    entities = [PodcastEntity::class, EpisodeEntity::class, PlaybackStateEntity::class],
    version = 3,
    // Every schema change ships a migration and a MigrationTest case; there is no destructive fallback.
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun playbackStateDao(): PlaybackStateDao
}

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

