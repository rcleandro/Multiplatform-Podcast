package br.com.carvalho.podcast.data.local

import br.com.carvalho.podcast.core.util.AppDirectories
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSHomeDirectory

fun createAppDatabase(directories: AppDirectories): AppDatabase {
    val dbPath = NSHomeDirectory() + "/Documents/podcast.db"
    return Room.databaseBuilder<AppDatabase>(dbPath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .addAppMigrations(directories)
        .build()
}
