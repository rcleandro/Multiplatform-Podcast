package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.observability.Metrics
import br.com.carvalho.podcast.core.util.AppLogger
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

private const val TAG = "Koin"
private var isKoinInitialized = false

actual fun initKoin(appDeclaration: KoinAppDeclaration) {
    Metrics.markProcessStart()
    if (isKoinInitialized) {
        AppLogger.i(TAG, "Koin already initialized for Wasm, skipping.")
        return
    }
    isKoinInitialized = true
    // No build types on the Web: the development server runs on this machine, a published bundle does not (22.9).
    AppLogger.isDebugBuild = pageHost() in LOCAL_HOSTS
    AppLogger.i(TAG, "Initializing Koin for Wasm...")
    startKoin {
        appDeclaration()
        modules(commonModules + platformModule)
    }
}

private val LOCAL_HOSTS = setOf("localhost", "127.0.0.1", "[::1]")

private fun pageHost(): String = js("window.location.hostname")
