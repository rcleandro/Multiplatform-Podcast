package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.core.config.RemoteConfig
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "Koin"
private var isKoinInitialized = false

actual fun initKoin(appDeclaration: KoinAppDeclaration) {
    if (isKoinInitialized) {
        AppLogger.i(TAG, "Koin already initialized for iOS, skipping.")
        return
    }
    isKoinInitialized = true
    AppLogger.i(TAG, "Initializing Koin for iOS...")

    val koinApp = startKoin {
        appDeclaration()
        modules(commonModules)
    }

    val koin = koinApp.koin
    val dispatchers = koin.get<CoroutineDispatchers>()

    CoroutineScope(dispatchers.default).launch {
        @Suppress("TooGenericExceptionCaught")
        try {
            AppLogger.i(TAG, "Initializing Firebase in background...")
            Firebase.initialize()

            AppLogger.i(TAG, "Firebase initialized, fetching Remote Config...")
            RemoteConfig.fetchAndActivate()
            AppLogger.i(TAG, "Remote Config fetched and activated")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize Firebase or Remote Config", e)
        }
    }
}
