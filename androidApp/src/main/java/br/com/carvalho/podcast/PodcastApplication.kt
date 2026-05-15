package br.com.carvalho.podcast

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import br.com.carvalho.podcast.core.di.initKoin
import br.com.carvalho.podcast.core.util.AppContext
import br.com.carvalho.podcast.domain.player.AudioPlayer
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.mp.KoinPlatform.getKoin

class PodcastApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.context = this
        initKoin {
            androidLogger()
            androidContext(this@PodcastApplication)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    getKoin().get<AudioPlayer>().releasePlayer()
                }
            }
        )
    }
}
