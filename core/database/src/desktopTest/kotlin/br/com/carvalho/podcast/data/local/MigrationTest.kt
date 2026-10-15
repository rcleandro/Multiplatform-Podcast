package br.com.carvalho.podcast.data.local

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.core.util.episodeId
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Opens a database written by every exported schema version and migrates it to the current one. */
class MigrationTest {
    private val databaseFile: Path = Files.createTempFile("migration", ".db")
    private val directories = AppDirectories(FakeFileSystem(), "/app".toPath())
    private val migrations = listOf(EpisodeIdMigration(directories), QueueItemsMigration, DownloadFileMigration, SubscribedAtMigration, PositionMigration)
    private val helper = MigrationTestHelper(
        schemaDirectoryPath = Paths.get("schemas"),
        databasePath = databaseFile,
        driver = BundledSQLiteDriver(),
        databaseClass = AppDatabase::class,
        databaseFactory = { AppDatabaseConstructor.initialize() },
    )

    @AfterTest
    fun deleteDatabase() {
        databaseFile.deleteIfExists()
    }

    @Test
    fun everyVersionMigratesToTheCurrentOneKeepingTheLibrary() = runTest {
        for (version in FIRST_VERSION until CURRENT_VERSION) {
            databaseFile.deleteIfExists()
            // From version 4 on, rows already carry the feed-scoped id.
            val episodeId = if (version >= EPISODE_ID_VERSION) E1 else "e1"
            val queueJson = if (version < QUEUE_TABLE_VERSION) "[]" else null
            helper.createDatabase(version).use { it.insertLibrary(queueJson, episodeId) }

            helper.runMigrationsAndValidate(CURRENT_VERSION, migrations).use { connection ->
                assertEquals("Podcast", connection.text("SELECT title FROM podcasts WHERE id = 'p1'"))
                assertEquals("1|42000|1", connection.text(
                    "SELECT isPlayed || '|' || playbackPosition || '|' || isDownloaded FROM episodes WHERE id = '$E1'"
                ))
                assertEquals(E1, connection.text("SELECT episodeId FROM playback_state"))
            }
        }
    }

    @Test
    fun version4GivesEpisodesFeedScopedIdsAndMovesTheirReferencesAndFiles() = runTest {
        val noGuidId = "Trailer".hashCode().toString()
        val fileSystem = directories.fileSystem
        fileSystem.createDirectories(directories.downloadsDir)
        fileSystem.write(directories.downloadPath("e1.mp3")) { writeUtf8("audio") }
        helper.createDatabase(EPISODE_ID_VERSION - 1).use { connection ->
            connection.insertLibrary(queueJson = """[{"id":"e1","title":"Episode"},{"id":"$noGuidId"}]""")
            connection.execSQL(
                "INSERT INTO episodes VALUES ('$noGuidId', 'p1', 'Podcast', 'Trailer', NULL, 'https://trailer', " +
                    "NULL, 60, 0, 0, 0, 0, NULL)"
            )
        }

        helper.runMigrationsAndValidate(EPISODE_ID_VERSION, migrations).use { connection ->
            val trailer = episodeId("p1", guid = null, audioUrl = "https://trailer")
            assertEquals("1", connection.text("SELECT COUNT(*) FROM episodes WHERE id = '$trailer'"))
            assertEquals(E1, connection.text("SELECT episodeId FROM playback_state"))
            assertEquals(
                """[{"id":"$E1","title":"Episode"},{"id":"$trailer"}]""",
                connection.text("SELECT queueJson FROM playback_state")
            )
        }
        assertTrue(fileSystem.exists(directories.downloadPath("$E1.mp3")))
        assertFalse(fileSystem.exists(directories.downloadPath("e1.mp3")))
    }

    @Test
    fun version5KeepsTheQueueAsReferencesToEpisodesInTheLibrary() = runTest {
        helper.createDatabase(QUEUE_TABLE_VERSION - 1).use { connection ->
            connection.insertLibrary(queueJson = """[{"id":"e1","title":"Stale copy"},{"id":"gone"}]""")
        }

        helper.runMigrationsAndValidate(QUEUE_TABLE_VERSION, migrations).use { connection ->
            assertEquals("1", connection.text("SELECT COUNT(*) FROM queue_items"))
            assertEquals("0|e1", connection.text("SELECT position || '|' || episodeId FROM queue_items"))
            assertEquals("e1", connection.text("SELECT episodeId FROM playback_state"))
        }
    }

    @Test
    fun version6RecordsTheFileOfEpisodesAlreadyDownloaded() = runTest {
        helper.createDatabase(DOWNLOAD_FILE_VERSION - 1).use { connection ->
            connection.insertLibrary(queueJson = null, episodeId = E1)
            connection.execSQL(
                "INSERT INTO episodes VALUES ('e2', 'p1', 'Podcast', 'Other', NULL, 'https://other', " +
                    "NULL, 60, 0, 0, 0, 0, NULL)"
            )
        }

        helper.runMigrationsAndValidate(DOWNLOAD_FILE_VERSION, migrations).use { connection ->
            assertEquals("$E1.mp3", connection.text("SELECT downloadFile FROM episodes WHERE id = '$E1'"))
            assertEquals("1", connection.text("SELECT COUNT(*) FROM episodes WHERE downloadFile IS NULL"))
        }
    }

    @Test
    fun version7StartsEveryPodcastWithoutAFeedVersion() = runTest {
        helper.createDatabase(FEED_VERSION_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(FEED_VERSION_VERSION, migrations).use { connection ->
            assertEquals(
                "1", connection.text("SELECT COUNT(*) FROM podcasts WHERE etag IS NULL AND lastModified IS NULL")
            )
        }
    }

    @Test
    fun version8DatesThePodcastsInTheOrderTheyWereAdded() = runTest {
        helper.createDatabase(SUBSCRIBED_AT_VERSION - 1).use { connection ->
            connection.insertLibrary(queueJson = null, episodeId = E1)
            connection.execSQL(
                "INSERT INTO podcasts (id, title, description, categories, feedUrl, lastUpdated, isSubscribed) " +
                    "VALUES ('p0', 'Added later', '', '[]', 'https://later', 0, 1)"
            )
        }

        helper.runMigrationsAndValidate(SUBSCRIBED_AT_VERSION, migrations).use { connection ->
            assertEquals(
                "p1,p0", connection.text("SELECT group_concat(id) FROM (SELECT id FROM podcasts ORDER BY subscribedAt)")
            )
            assertEquals("0", connection.text("SELECT COUNT(*) FROM podcasts WHERE subscribedAt <= 0"))
        }
    }

    @Test
    fun version9PlacesThePodcastsInTheOrderTheyWereAdded() = runTest {
        helper.createDatabase(POSITION_VERSION - 1).use { connection ->
            connection.insertLibrary(queueJson = null, episodeId = E1)
            connection.execSQL(
                "INSERT INTO podcasts (id, title, description, categories, feedUrl, lastUpdated, isSubscribed) " +
                    "VALUES ('p0', 'Added later', '', '[]', 'https://later', 0, 1)"
            )
        }

        helper.runMigrationsAndValidate(POSITION_VERSION, migrations).use { connection ->
            assertEquals(
                "p1,p0", connection.text("SELECT group_concat(id) FROM (SELECT id FROM podcasts ORDER BY position)")
            )
        }
    }

    @Test
    fun version10StartsEveryEpisodeWithoutAPageLink() = runTest {
        helper.createDatabase(LINK_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(LINK_VERSION, migrations).use { connection ->
            assertEquals("1", connection.text("SELECT COUNT(*) FROM episodes WHERE link IS NULL"))
        }
    }

    @Test
    fun version11StartsEveryPodcastAndEpisodeAsNotExplicit() = runTest {
        helper.createDatabase(EXPLICIT_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(EXPLICIT_VERSION, migrations).use { connection ->
            assertEquals("0", connection.text("SELECT explicit FROM podcasts WHERE id = 'p1'"))
            assertEquals("0", connection.text("SELECT explicit FROM episodes WHERE id = '$E1'"))
        }
    }

    @Test
    fun version12StartsWithNothingInNew() = runTest {
        helper.createDatabase(NEW_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(NEW_VERSION, migrations).use { connection ->
            assertEquals("0", connection.text("SELECT isNew FROM episodes WHERE id = '$E1'"))
        }
    }

    @Test
    fun version13ListsEveryPodcastAsTheFeedSays() = runTest {
        helper.createDatabase(ORDER_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(ORDER_VERSION, migrations).use { connection ->
            assertEquals("0|1", connection.text("SELECT serial || '|' || (oldestFirst IS NULL) FROM podcasts"))
        }
    }

    @Test
    fun version14StartsEveryEpisodeAsARegularOneWithoutSeason() = runTest {
        helper.createDatabase(SEASON_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(SEASON_VERSION, migrations).use { connection ->
            assertEquals("1", connection.text("SELECT COUNT(*) FROM episodes WHERE season IS NULL AND episodeType IS NULL"))
        }
    }

    @Test
    fun version15StartsWithNothingSaved() = runTest {
        helper.createDatabase(SAVED_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(SAVED_VERSION, migrations).use { connection ->
            assertEquals("0", connection.text("SELECT isSaved FROM episodes WHERE id = '$E1'"))
        }
    }

    @Test
    fun version16StartsWithAnEmptyHistory() = runTest {
        helper.createDatabase(HISTORY_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(HISTORY_VERSION, migrations).use { connection ->
            assertEquals("1", connection.text("SELECT COUNT(*) FROM episodes WHERE lastPlayedAt IS NULL"))
        }
    }

    @Test
    fun version17PlaysEveryPodcastTheGeneralWay() = runTest {
        helper.createDatabase(PODCAST_PLAYBACK_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(PODCAST_PLAYBACK_VERSION, migrations).use { connection ->
            assertEquals(
                "1|0|0",
                connection.text("SELECT (playbackSpeed IS NULL) || '|' || skipIntroSeconds || '|' || skipOutroSeconds FROM podcasts"),
            )
        }
    }

    @Test
    fun version18LetsEveryPodcastNotify() = runTest {
        helper.createDatabase(NOTIFY_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(NOTIFY_VERSION, migrations).use { connection ->
            assertEquals("1", connection.text("SELECT notifyNewEpisodes FROM podcasts WHERE id = 'p1'"))
        }
    }

    @Test
    fun version19DownloadsNothingByItself() = runTest {
        helper.createDatabase(AUTO_DOWNLOAD_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(AUTO_DOWNLOAD_VERSION, migrations).use { connection ->
            assertEquals("0", connection.text("SELECT autoDownloadCount FROM podcasts WHERE id = 'p1'"))
        }
    }

    @Test
    fun version20KeepsDownloadsFromBeforeWithoutADate() = runTest {
        helper.createDatabase(DOWNLOADED_AT_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(DOWNLOADED_AT_VERSION, migrations).use { connection ->
            assertEquals("1|1", connection.text("SELECT isDownloaded || '|' || (downloadedAt IS NULL) FROM episodes"))
        }
    }

    private fun SQLiteConnection.insertLibrary(queueJson: String? = "[]", episodeId: String = "e1") {
        execSQL(
            "INSERT INTO podcasts (id, title, description, imageUrl, author, language, categories, feedUrl, siteUrl, " +
                "lastUpdated, isSubscribed) VALUES ('p1', 'Podcast', '', NULL, NULL, NULL, '', 'https://feed', NULL, 0, 1)"
        )
        execSQL(
            // Named columns: later versions add more (downloadFile in 6; etag, lastModified, subscribedAt in podcasts).
            "INSERT INTO episodes (id, podcastId, podcastTitle, title, description, audioUrl, imageUrl, duration, " +
                "publishDate, isPlayed, playbackPosition, isDownloaded, fileSize) VALUES " +
                "('$episodeId', 'p1', 'Podcast', 'Episode', NULL, 'https://audio', NULL, 60, 0, 1, 42000, 1, NULL)"
        )
        // Before version 5 the queue was a JSON column.
        val queueValue = queueJson?.let { ", '$it'" }.orEmpty()
        execSQL("INSERT INTO playback_state VALUES (0, '$episodeId', 42000, 1.0$queueValue)")
    }

    @Test
    fun version21KnowsNoChaptersUntilTheFeedIsReadAgain() = runTest {
        helper.createDatabase(CHAPTERS_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(CHAPTERS_VERSION, migrations).use { connection ->
            assertEquals("1", connection.text("SELECT COUNT(*) FROM episodes WHERE chaptersUrl IS NULL"))
        }
    }

    @Test
    fun version22StartsWithoutBookmarks() = runTest {
        helper.createDatabase(BOOKMARKS_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(BOOKMARKS_VERSION, migrations).use { connection ->
            assertEquals("0", connection.text("SELECT COUNT(*) FROM bookmarks"))
        }
    }

    @Test
    fun version23StartsWithoutSmartPlaylists() = runTest {
        helper.createDatabase(SMART_PLAYLISTS_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(SMART_PLAYLISTS_VERSION, migrations).use { connection ->
            assertEquals("0", connection.text("SELECT COUNT(*) FROM smart_playlists"))
        }
    }

    @Test
    fun version24StartsWithNothingHeard() = runTest {
        helper.createDatabase(LISTENING_VERSION - 1).use { it.insertLibrary(queueJson = null, episodeId = E1) }

        helper.runMigrationsAndValidate(LISTENING_VERSION, migrations).use { connection ->
            assertEquals("0", connection.text("SELECT COUNT(*) FROM listening"))
        }
    }

    private fun SQLiteConnection.text(sql: String): String = prepare(sql).use { statement ->
        statement.step()
        statement.getText(0)
    }

    private companion object {
        const val FIRST_VERSION = 1
        const val EPISODE_ID_VERSION = 4
        const val QUEUE_TABLE_VERSION = 5
        const val DOWNLOAD_FILE_VERSION = 6
        const val FEED_VERSION_VERSION = 7
        const val SUBSCRIBED_AT_VERSION = 8
        const val POSITION_VERSION = 9
        const val LINK_VERSION = 10
        const val EXPLICIT_VERSION = 11
        const val NEW_VERSION = 12
        const val ORDER_VERSION = 13
        const val SEASON_VERSION = 14
        const val SAVED_VERSION = 15
        const val HISTORY_VERSION = 16
        const val PODCAST_PLAYBACK_VERSION = 17
        const val NOTIFY_VERSION = 18
        const val AUTO_DOWNLOAD_VERSION = 19
        const val DOWNLOADED_AT_VERSION = 20
        const val CHAPTERS_VERSION = 21
        const val BOOKMARKS_VERSION = 22
        const val SMART_PLAYLISTS_VERSION = 23
        const val LISTENING_VERSION = 24
        const val CURRENT_VERSION = 24

        /** The id episode "e1" of podcast "p1" gets from version 4 on. */
        val E1 = episodeId("p1", guid = "e1", audioUrl = "https://audio")
    }
}
