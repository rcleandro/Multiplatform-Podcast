package br.com.carvalho.podcast.data.local

import br.com.carvalho.podcast.core.util.AppDirectories
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

fun createAppDatabase(directories: AppDirectories): AppDatabase {
    val dbFile = File(System.getProperty("user.home"), ".podcast/podcast.db")
        .also { it.parentFile?.mkdirs() }
    return Room.databaseBuilder<AppDatabase>(dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addAppMigrations(directories)
        .build()
}
