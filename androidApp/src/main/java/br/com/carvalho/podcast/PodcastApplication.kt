package br.com.carvalho.podcast

import android.app.Application
import br.com.carvalho.podcast.core.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

/**
 * The audio player lives as long as the process and the media service; no screen releases it. (A ProcessLifecycleOwner
 * ON_DESTROY observer used to, but Android never dispatches that event.)
 */
class PodcastApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@PodcastApplication)
        }
    }
}
