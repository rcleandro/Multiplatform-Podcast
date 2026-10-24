package br.com.carvalho.podcast

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import androidx.annotation.OptIn
import androidx.media3.cast.Cast
import androidx.media3.common.util.UnstableApi
import br.com.carvalho.podcast.core.di.initKoin
import br.com.carvalho.podcast.core.refresh.BackgroundRefresh
import br.com.carvalho.podcast.core.sync.FolderSync
import br.com.carvalho.podcast.core.sync.WearableSync
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
        if (isDebuggable()) watchTheMainThread()
        initKoin {
            androidLogger()
            androidContext(this@PodcastApplication)
        }
        PlayerWidget.keepUpdated(this, scope)
        BackgroundRefresh.keepScheduled(this, scope)
        WearableSync.keepInSync(this, scope)
        FolderSync.keepInSync(scope)
        // Before the Cast button or the media service need it (18.17); it loads in the background.
        Cast.getSingletonInstance(this).initialize()
    }

    private fun isDebuggable() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    /**
     * Debug builds log every disk and network access made on the main thread, with its stack (23.5): the kind of work
     * that slows the start and drops frames. Only logged, so a violation never crashes the app being tested.
     */
    private fun watchTheMainThread() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().detectLeakedSqlLiteObjects().penaltyLog().build())
    }
}
