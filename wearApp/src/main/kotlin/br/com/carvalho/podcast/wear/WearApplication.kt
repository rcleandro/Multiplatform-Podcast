package br.com.carvalho.podcast.wear

import android.app.Application
import br.com.carvalho.podcast.core.di.initKoin
import br.com.carvalho.podcast.core.refresh.BackgroundRefresh
import kotlinx.coroutines.MainScope
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The watch app's process (19.9a): the same Koin graph as the phone's, so the library, player and downloads work the
 * same way. No Firebase here: the watch sends no telemetry.
 */
class WearApplication : Application() {
    /** Work that lasts as long as the process, like keeping the background refresh scheduled. */
    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@WearApplication)
            modules(module { viewModelOf(::WatchEffectsViewModel) })
        }
        BackgroundRefresh.keepScheduled(this, scope)
    }
}
