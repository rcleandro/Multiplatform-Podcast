package br.com.carvalho.podcast.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSHomeDirectory

actual fun createAppDatabase(): AppDatabase {
    val dbPath = NSHomeDirectory() + "/Documents/podcast.db"
    return Room.databaseBuilder<AppDatabase>(dbPath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .fallbackToDestructiveMigration(true)
        .build()
}
