package br.com.carvalho.podcast.data.local

import androidx.room3.Room

actual fun createInMemoryDatabase(): AppDatabase {
    return Room.inMemoryDatabaseBuilder<AppDatabase>()
        .build()
}

actual val isDatabaseSupported: Boolean = false
