package br.com.carvalho.podcast.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import br.com.carvalho.podcast.core.util.getCurrentTimestamp

/**
 * 7 → 8: `subscribedAt`, when the podcast entered the library. Nothing recorded it before, so existing podcasts get
 * the migration time plus their rowid (milliseconds), which keeps the order they were added in.
 */
object SubscribedAtMigration : Migration(FROM_VERSION, FROM_VERSION + 1) {

    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE podcasts ADD COLUMN subscribedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE podcasts SET subscribedAt = ${getCurrentTimestamp()} + rowid")
    }
}

private const val FROM_VERSION = 7
