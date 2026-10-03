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
    private val migrations = listOf(EpisodeIdMigration(directories), QueueItemsMigration)
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
            helper.createDatabase(version).use { it.insertLibrary(episodeId = episodeId) }

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
        fileSystem.write(directories.downloadPath("e1")) { writeUtf8("audio") }
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
        assertTrue(fileSystem.exists(directories.downloadPath(E1)))
        assertFalse(fileSystem.exists(directories.downloadPath("e1")))
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

    private fun SQLiteConnection.insertLibrary(queueJson: String = "[]", episodeId: String = "e1") {
        execSQL(
            "INSERT INTO podcasts VALUES ('p1', 'Podcast', '', NULL, NULL, NULL, '', 'https://feed', NULL, 0, 1)"
        )
        execSQL(
            "INSERT INTO episodes VALUES " +
                "('$episodeId', 'p1', 'Podcast', 'Episode', NULL, 'https://audio', NULL, 60, 0, 1, 42000, 1, NULL)"
        )
        execSQL("INSERT INTO playback_state VALUES (0, '$episodeId', 42000, 1.0, '$queueJson')")
    }

    private fun SQLiteConnection.text(sql: String): String = prepare(sql).use { statement ->
        statement.step()
        statement.getText(0)
    }

    private companion object {
        const val FIRST_VERSION = 1
        const val EPISODE_ID_VERSION = 4
        const val QUEUE_TABLE_VERSION = 5
        const val CURRENT_VERSION = 5

        /** The id episode "e1" of podcast "p1" gets from version 4 on. */
        val E1 = episodeId("p1", guid = "e1", audioUrl = "https://audio")
    }
}
