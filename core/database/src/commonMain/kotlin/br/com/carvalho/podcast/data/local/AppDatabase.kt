package br.com.carvalho.podcast.data.local

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.data.local.dao.EpisodeDao
import br.com.carvalho.podcast.data.local.dao.PlaybackStateDao
import br.com.carvalho.podcast.data.local.dao.PodcastDao
import br.com.carvalho.podcast.data.local.entity.BookmarkEntity
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.ListeningEntity
import br.com.carvalho.podcast.data.local.entity.PlaybackStateEntity
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import br.com.carvalho.podcast.data.local.entity.QueueItemEntity
import br.com.carvalho.podcast.data.local.entity.RemovedPodcastEntity
import br.com.carvalho.podcast.data.local.entity.SmartPlaylistEntity

@Database(
    entities = [
        PodcastEntity::class,
        EpisodeEntity::class,
        PlaybackStateEntity::class,
        QueueItemEntity::class,
        BookmarkEntity::class,
        SmartPlaylistEntity::class,
        ListeningEntity::class,
        RemovedPodcastEntity::class,
    ],
    version = 27,
    // Every schema change ships a migration and a MigrationTest case; there is no destructive fallback.
    // 3 → 4, 4 → 5, 5 → 6, 7 → 8 and 8 → 9 are manual (EpisodeIdMigration, QueueItemsMigration,
    // DownloadFileMigration, SubscribedAtMigration, PositionMigration), added by addAppMigrations.
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21),
        AutoMigration(from = 21, to = 22),
        AutoMigration(from = 22, to = 23),
        AutoMigration(from = 23, to = 24),
        AutoMigration(from = 24, to = 25),
        AutoMigration(from = 25, to = 26),
        AutoMigration(from = 26, to = 27),
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

/** The manual migrations every platform's builder needs; the automatic ones come from `@Database`. */
fun RoomDatabase.Builder<AppDatabase>.addAppMigrations(directories: AppDirectories): RoomDatabase.Builder<AppDatabase> =
    addMigrations(
        EpisodeIdMigration(directories),
        QueueItemsMigration,
        DownloadFileMigration,
        SubscribedAtMigration,
        PositionMigration,
    )
