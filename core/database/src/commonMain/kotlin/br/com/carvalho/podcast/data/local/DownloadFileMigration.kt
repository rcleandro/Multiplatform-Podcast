package br.com.carvalho.podcast.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * 5 → 6: the downloaded file's name was deduced as "<id>.mp3"; it becomes the `downloadFile` column, so the
 * extension can follow the audio type. Episodes already downloaded keep their file under the old name.
 */
object DownloadFileMigration : Migration(FROM_VERSION, FROM_VERSION + 1) {

    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE episodes ADD COLUMN downloadFile TEXT")
        connection.execSQL("UPDATE episodes SET downloadFile = id || '.mp3' WHERE isDownloaded = 1")
    }
}

private const val FROM_VERSION = 5
