package br.com.carvalho.podcast.benchmark

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The Episodes search on this phone's processor (23.4): `LIKE`, as the app searched before, against the full-text index
 * that replaced it, and what the index costs the writes, on a copy of a real library. Push one, then run this class:
 * `adb push podcast.db /data/local/tmp/search-bench.db`. It never opens the app's own database. Results: logcat.
 */
@RunWith(AndroidJUnit4::class)
class SearchQueryBenchmark {
    @Test
    fun likeAgainstFullText() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val copy = File(context.cacheDir, "search-bench.db")
        File(PUSHED).copyTo(copy, overwrite = true)
        val db = SQLiteDatabase.openDatabase(copy.path, null, SQLiteDatabase.OPEN_READWRITE)
        val results = Bundle()
        val longest = db.text(LONGEST_EPISODE)
        val biggest = db.text(BIGGEST_PODCAST)

        results.putString("progress_update_without_index", "${median(MICROS) { db.saveProgress(longest) }} µs")
        results.putString("feed_rewrite_without_index", "${once { db.rewriteFeed(biggest) }} ms")
        listOf(CREATE_INDEX, FILL_INDEX, BEFORE_UPDATE, AFTER_UPDATE).forEach(db::execSQL)
        results.putString("progress_update_with_index", "${median(MICROS) { db.saveProgress(longest) }} µs")
        results.putString("feed_rewrite_with_index", "${once { db.rewriteFeed(biggest) }} ms")
        TERMS.forEach { term ->
            results.putString("like_$term", "${median(MILLIS) { db.count(LIKE, "%$term%", "%$term%") }} ms")
            results.putString("fts_$term", "${median(MILLIS) { db.count(MATCH, "$term*") }} ms")
        }

        db.close()
        copy.delete()
        InstrumentationRegistry.getInstrumentation().sendStatus(0, results)
        println(results)
    }

    private fun SQLiteDatabase.text(sql: String): String = rawQuery(sql, null).use { cursor ->
        cursor.moveToFirst()
        cursor.getString(0)
    }

    private fun SQLiteDatabase.count(sql: String, vararg args: String): Long = rawQuery(sql, args).use { cursor ->
        cursor.moveToFirst()
        cursor.getLong(0)
    }

    private fun SQLiteDatabase.saveProgress(id: String) = execSQL(SAVE_PROGRESS, arrayOf(id))

    // What saveFeed does to every episode of a feed that changed: one UPDATE each, in one transaction.
    private fun SQLiteDatabase.rewriteFeed(podcastId: String) {
        beginTransaction()
        try {
            execSQL(REWRITE_FEED, arrayOf(podcastId))
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private fun median(unit: Long, block: () -> Unit): Long = List(RUNS) { once(unit, block) }.sorted()[RUNS / 2]

    private fun once(unit: Long = MILLIS, block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / unit
    }

    private companion object {
        const val PUSHED = "/data/local/tmp/search-bench.db"
        const val LIKE = "SELECT count(*) FROM episodes WHERE title LIKE ? OR description LIKE ?"
        const val MATCH = "SELECT count(*) FROM fts WHERE fts MATCH ?"
        const val LONGEST_EPISODE = "SELECT id FROM episodes ORDER BY length(description) DESC LIMIT 1"
        const val BIGGEST_PODCAST = "SELECT podcastId FROM episodes GROUP BY podcastId ORDER BY count(*) DESC LIMIT 1"
        const val SAVE_PROGRESS = "UPDATE episodes SET playbackPosition = playbackPosition + 1 WHERE id = ?"
        const val REWRITE_FEED = "UPDATE episodes SET title = title WHERE podcastId = ?"
        const val CREATE_INDEX = "CREATE VIRTUAL TABLE fts USING fts4(" +
            "content=\"episodes\", title, description, tokenize=unicode61 \"remove_diacritics=1\")"
        const val FILL_INDEX = "INSERT INTO fts(fts) VALUES('rebuild')"

        // The triggers Room makes for an external-content index: any update rewrites the row in it.
        const val BEFORE_UPDATE =
            "CREATE TRIGGER fts_bu BEFORE UPDATE ON episodes BEGIN DELETE FROM fts WHERE docid = OLD.rowid; END"
        const val AFTER_UPDATE = "CREATE TRIGGER fts_au AFTER UPDATE ON episodes BEGIN " +
            "INSERT INTO fts(docid, title, description) VALUES (NEW.rowid, NEW.title, NEW.description); END"
        val TERMS = listOf("kotlin", "intelig", "xyzzyq")
        const val RUNS = 9
        const val MILLIS = 1_000_000L
        const val MICROS = 1_000L
    }
}
