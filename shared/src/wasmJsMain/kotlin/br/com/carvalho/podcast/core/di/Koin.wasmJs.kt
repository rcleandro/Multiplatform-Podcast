package br.com.carvalho.podcast.core.di

import br.com.carvalho.podcast.core.util.AppLogger
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

private const val TAG = "Koin"

actual fun initKoin(appDeclaration: KoinAppDeclaration) {
    AppLogger.i(TAG, "Initializing Koin for Wasm...")
    startKoin {
        appDeclaration()
        modules(commonModules)
    }
}
