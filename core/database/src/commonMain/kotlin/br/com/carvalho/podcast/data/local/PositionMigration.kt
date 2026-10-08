package br.com.carvalho.podcast.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** 8 → 9: `position`, the custom order of the library. It starts as the order the podcasts were added in (rowid). */
object PositionMigration : Migration(FROM_VERSION, FROM_VERSION + 1) {

    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE podcasts ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE podcasts SET position = rowid")
    }
}

private const val FROM_VERSION = 8
