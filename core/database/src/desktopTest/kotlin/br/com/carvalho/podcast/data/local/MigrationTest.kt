package br.com.carvalho.podcast.data.local

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Opens a database written by every exported schema version and migrates it to the current one. */
class MigrationTest {
    private val databaseFile: Path = Files.createTempFile("migration", ".db")
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
            helper.createDatabase(version).use { it.insertLibrary() }

            helper.runMigrationsAndValidate(CURRENT_VERSION).use { connection ->
                assertEquals("Podcast", connection.text("SELECT title FROM podcasts WHERE id = 'p1'"))
                assertEquals("1|42000|1", connection.text(
                    "SELECT isPlayed || '|' || playbackPosition || '|' || isDownloaded FROM episodes WHERE id = 'e1'"
                ))
                assertEquals("e1", connection.text("SELECT episodeId FROM playback_state"))
            }
        }
    }

    private fun SQLiteConnection.insertLibrary() {
        execSQL(
            "INSERT INTO podcasts VALUES ('p1', 'Podcast', '', NULL, NULL, NULL, '', 'https://feed', NULL, 0, 1)"
        )
        execSQL(
            "INSERT INTO episodes VALUES " +
                "('e1', 'p1', 'Podcast', 'Episode', NULL, 'https://audio', NULL, 60, 0, 1, 42000, 1, NULL)"
        )
        execSQL("INSERT INTO playback_state VALUES (0, 'e1', 42000, 1.0, '[]')")
    }

    private fun SQLiteConnection.text(sql: String): String = prepare(sql).use { statement ->
        statement.step()
        statement.getText(0)
    }

    private companion object {
        const val FIRST_VERSION = 1
        const val CURRENT_VERSION = 3
    }
}
