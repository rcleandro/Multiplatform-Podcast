package br.com.carvalho.podcast.wear

import android.app.Application
import android.content.ComponentName
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import br.com.carvalho.podcast.core.di.initKoin
import br.com.carvalho.podcast.core.refresh.BackgroundRefresh
import br.com.carvalho.podcast.core.sync.WearableSync
import br.com.carvalho.podcast.domain.player.AudioPlayer
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.get
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
        WearableSync.keepInSync(this, scope)
        keepNowPlayingFresh()
    }

    /** Draws the tile and the face's complication again when the episode, play or pause, or the minutes left change. */
    private fun keepNowPlayingFresh() {
        val tiles = TileService.getUpdater(this)
        val complications = ComplicationDataSourceUpdateRequester.create(
            this,
            ComponentName(this, NowPlayingComplicationService::class.java),
        )
        get<AudioPlayer>().playerState
            .map { Triple(it.currentEpisode?.id, it.isPlaying, timeLeft(it)) }
            .distinctUntilChanged()
            .onEach {
                tiles.requestUpdate(NowPlayingTileService::class.java)
                complications.requestUpdateAll()
            }
            .launchIn(scope)
    }
}
