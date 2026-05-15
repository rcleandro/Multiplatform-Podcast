package br.com.carvalho.podcast.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import br.com.carvalho.podcast.core.util.AppContext
import br.com.carvalho.podcast.core.util.androidContext
import kotlinx.coroutines.Dispatchers

actual fun createAppDatabase(): AppDatabase {
    val context = AppContext.androidContext
    return Room.databaseBuilder<AppDatabase>(context, "podcast.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()
}
