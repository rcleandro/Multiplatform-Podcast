package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.observability.CrashReporter
import br.com.carvalho.podcast.core.observability.followTelemetryConsent
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import br.com.carvalho.podcast.core.util.AppLogger
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.content.Context
import android.content.pm.ApplicationInfo

private const val TAG = "Koin"
private var isKoinInitialized = false

actual fun initKoin(appDeclaration: KoinAppDeclaration) {
    if (isKoinInitialized) {
        AppLogger.i(TAG, "Koin already initialized for Android, skipping.")
        return
    }
    isKoinInitialized = true
    AppLogger.i(TAG, "Initializing Koin for Android...")

    val koinApp = startKoin {
        appDeclaration()
        modules(commonModules + platformModule)
    }

    val koin = koinApp.koin
    AppLogger.crashReporter = koin.getOrNull<CrashReporter>()
    val context = koin.get<Context>()
    AppLogger.isDebugBuild = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val dispatchers = koin.get<CoroutineDispatchers>()

    CoroutineScope(dispatchers.default).launch {
        @Suppress("TooGenericExceptionCaught")
        try {
            AppLogger.i(TAG, "Initializing Firebase in background...")
            Firebase.initialize(context)
            AppLogger.i(TAG, "Firebase initialized")
            followTelemetryConsent(koin.get<PreferencesRepository>().telemetryEnabled)
        } catch (e: Exception) {
            // No unit tests, Firebase might not be available
            AppLogger.e(TAG, "Failed to initialize Firebase", e)
        }
    }
}
