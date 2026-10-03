package br.com.carvalho.podcast.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.core.util.episodeId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 3 → 4: episode ids were the feed's guid (or the hash code of the title when there was none), so equal guids in
 * two feeds collided. They become [episodeId]; the player state, the saved queue and the downloaded files follow.
 */
class EpisodeIdMigration(private val directories: AppDirectories) : Migration(FROM_VERSION, FROM_VERSION + 1) {

    override suspend fun migrate(connection: SQLiteConnection) {
        val newIds = connection.readNewIds()
        newIds.forEach { (old, new) ->
            connection.prepare("UPDATE episodes SET id = ? WHERE id = ?").use {
                it.bindText(1, new)
                it.bindText(2, old)
                it.step()
            }
        }
        connection.remapPlaybackState(newIds)
        renameDownloads(newIds)
    }

    private suspend fun SQLiteConnection.readNewIds(): Map<String, String> =
        prepare("SELECT id, podcastId, title, audioUrl FROM episodes").use { statement ->
            buildMap {
                while (statement.step()) {
                    val oldId = statement.getText(0)
                    val title = statement.getText(2)
                    val audioUrl = statement.getText(3)
                    // Version 3 used this hash when the item had no guid; the new id falls back to the audio URL.
                    val hadNoGuid = oldId == title.hashCode().toString() || oldId == audioUrl.hashCode().toString()
                    put(oldId, episodeId(statement.getText(1), oldId.takeUnless { hadNoGuid }, audioUrl))
                }
            }
        }

    private suspend fun SQLiteConnection.remapPlaybackState(newIds: Map<String, String>) {
        val rows = prepare("SELECT id, episodeId, queueJson FROM playback_state").use { statement ->
            buildList {
                while (statement.step()) {
                    val episodeId = if (statement.isNull(1)) null else statement.getText(1)
                    add(Triple(statement.getLong(0), episodeId, statement.getText(2)))
                }
            }
        }
        rows.forEach { (rowId, episodeId, queueJson) ->
            prepare("UPDATE playback_state SET episodeId = ?, queueJson = ? WHERE id = ?").use {
                val newEpisodeId = episodeId?.let { id -> newIds[id] ?: id }
                if (newEpisodeId == null) it.bindNull(EPISODE_ID) else it.bindText(EPISODE_ID, newEpisodeId)
                it.bindText(QUEUE_JSON, remapQueue(queueJson, newIds))
                it.bindLong(ROW_ID, rowId)
                it.step()
            }
        }
    }

    /** The queue holds whole episodes as JSON; only their `id` changes. Unreadable JSON is kept as it was. */
    private fun remapQueue(queueJson: String, newIds: Map<String, String>): String = runCatching {
        JsonArray(
            Json.parseToJsonElement(queueJson).jsonArray.map { item ->
                val episode = item.jsonObject
                val id = episode["id"]?.jsonPrimitive?.content
                JsonObject(episode + ("id" to JsonPrimitive(newIds[id] ?: id)))
            }
        ).toString()
    }.getOrDefault(queueJson)

    private fun renameDownloads(newIds: Map<String, String>) {
        val fileSystem = directories.fileSystem
        newIds.forEach { (old, new) ->
            // Version 3 named every download "<id>.mp3".
            val file = directories.downloadPath("$old.mp3")
            if (fileSystem.exists(file)) fileSystem.atomicMove(file, directories.downloadPath("$new.mp3"))
        }
    }

    private companion object {
        const val FROM_VERSION = 3

        // Parameter positions of the playback_state UPDATE.
        const val EPISODE_ID = 1
        const val QUEUE_JSON = 2
        const val ROW_ID = 3
    }
}
