package br.com.carvalho.podcast.data.local

import br.com.carvalho.podcast.core.util.AppDirectories
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import android.content.Context
import kotlinx.coroutines.Dispatchers

fun createAppDatabase(context: Context, directories: AppDirectories): AppDatabase {
    return Room.databaseBuilder<AppDatabase>(context, "podcast.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(EpisodeIdMigration(directories))
        .build()
}
