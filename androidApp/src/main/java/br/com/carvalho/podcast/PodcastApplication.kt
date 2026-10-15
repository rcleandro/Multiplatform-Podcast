package br.com.carvalho.podcast

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.cast.Cast
import androidx.media3.common.util.UnstableApi
import br.com.carvalho.podcast.core.di.initKoin
import br.com.carvalho.podcast.core.refresh.BackgroundRefresh
import br.com.carvalho.podcast.widget.PlayerWidget
import kotlinx.coroutines.MainScope
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

/**
 * The audio player lives as long as the process and the media service; no screen releases it. (A ProcessLifecycleOwner
 * ON_DESTROY observer used to, but Android never dispatches that event.)
 */
@OptIn(UnstableApi::class)
class PodcastApplication : Application() {
    /** Work that lasts as long as the process, like keeping the player widget current. */
    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@PodcastApplication)
        }
        PlayerWidget.keepUpdated(this, scope)
        BackgroundRefresh.keepScheduled(this, scope)
        // Before the Cast button or the media service need it (18.17); it loads in the background.
        Cast.getSingletonInstance(this).initialize()
    }
}
