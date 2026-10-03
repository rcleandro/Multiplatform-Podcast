package br.com.carvalho.podcast.core.download

import br.com.carvalho.podcast.core.di.initKoin
import br.com.carvalho.podcast.data.download.UrlSessionEpisodeDownloader
import org.koin.mp.KoinPlatform

/**
 * Called by the app delegate when iOS relaunches the app to deliver finished background downloads; the app may not
 * have built its screen yet, so Koin is started here too.
 */
fun handleBackgroundDownloadEvents(completionHandler: () -> Unit) {
    initKoin()
    KoinPlatform.getKoin().get<UrlSessionEpisodeDownloader>().onBackgroundEvents(completionHandler)
}
