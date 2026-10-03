package br.com.carvalho.podcast.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import android.content.Context
import kotlinx.coroutines.Dispatchers

fun createAppDatabase(context: Context): AppDatabase {
    return Room.databaseBuilder<AppDatabase>(context, "podcast.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()
}
