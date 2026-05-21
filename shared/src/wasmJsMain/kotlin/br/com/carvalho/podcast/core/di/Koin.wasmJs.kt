package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.util.AppLogger
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

private const val TAG = "Koin"
private var isKoinInitialized = false

actual fun initKoin(appDeclaration: KoinAppDeclaration) {
    if (isKoinInitialized) {
        AppLogger.i(TAG, "Koin already initialized for Wasm, skipping.")
        return
    }
    isKoinInitialized = true
    AppLogger.i(TAG, "Initializing Koin for Wasm...")
    startKoin {
        appDeclaration()
        modules(commonModules)
    }
}
