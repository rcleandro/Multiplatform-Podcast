package br.com.carvalho.podcast.data.local

import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * 27 → 28 (23.4): Room creates the search index and its triggers, which only follow changes from then on; this fills
 * it with the episodes already saved, or the search would find nothing until every feed was read again.
 */
class SearchIndexMigration : AutoMigrationSpec {
    override suspend fun onPostMigrate(connection: SQLiteConnection) {
        connection.execSQL("INSERT INTO episodes_fts(episodes_fts) VALUES('rebuild')")
    }
}
