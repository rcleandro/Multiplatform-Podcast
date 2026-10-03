package br.com.carvalho.podcast.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 4 → 5: the queue was a JSON copy of whole episodes in `playback_state.queueJson`, which went stale (played,
 * downloaded, new audio URL). It becomes `queue_items` rows that point to the episodes; ids no longer in the library
 * are dropped, as the foreign key would drop them from now on.
 */
object QueueItemsMigration : Migration(FROM_VERSION, FROM_VERSION + 1) {

    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `queue_items` (`position` INTEGER NOT NULL, `episodeId` TEXT NOT NULL, " +
                "PRIMARY KEY(`position`), FOREIGN KEY(`episodeId`) REFERENCES `episodes`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_queue_items_episodeId` ON `queue_items` (`episodeId`)")
        connection.savedQueueIds().forEachIndexed { position, episodeId ->
            connection.prepare(
                "INSERT INTO queue_items (position, episodeId) SELECT ?, id FROM episodes WHERE id = ?"
            ).use {
                it.bindLong(1, position.toLong())
                it.bindText(2, episodeId)
                it.step()
            }
        }
        connection.execSQL("ALTER TABLE playback_state DROP COLUMN queueJson")
    }

    /** The ids in the saved queue; unreadable JSON means an empty queue, as before. */
    private suspend fun SQLiteConnection.savedQueueIds(): List<String> {
        val queueJson = prepare("SELECT queueJson FROM playback_state LIMIT 1").use { statement ->
            if (statement.step()) statement.getText(0) else null
        } ?: return emptyList()
        return runCatching {
            Json.parseToJsonElement(queueJson).jsonArray.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
        }.getOrDefault(emptyList())
    }
}

private const val FROM_VERSION = 4
