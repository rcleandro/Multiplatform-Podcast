package br.com.carvalho.podcast.data.local

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import br.com.carvalho.podcast.core.util.AppLogger
import org.w3c.dom.Worker

private const val TAG = "AppDatabase"

actual fun createAppDatabase(): AppDatabase {
    AppLogger.d(TAG, "Initializing database for Wasm with WebWorkerSQLiteDriver...")
    val driver = WebWorkerSQLiteDriver(createSqliteWorker())
    return Room.databaseBuilder<AppDatabase>("podcast.db")
        .setDriver(driver = driver)
        .fallbackToDestructiveMigration(true)
        .build()
}

private fun createSqliteWorker(): Worker {
    val url = getWorkerUrl()
    AppLogger.d(TAG, "Worker URL: $url")
    return Worker(url)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun getWorkerUrl(): String =
    js("""(typeof __webpack_public_path__ !== 'undefined' ? __webpack_public_path__ : './') + 'sqlite-worker.js'""")
