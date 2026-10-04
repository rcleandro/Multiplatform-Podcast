package br.com.carvalho.podcast.presentation

import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.error_download
import br.com.carvalho.podcast.domain.download.DownloadStatus
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * One message for each download that fails while this is collected, naming the cause ("storage full", "no
 * connection"); the row alone only shows that it failed. Failures from before collection are not news.
 */
fun EpisodeDownloader.failureMessages(): Flow<UiMessage> = flow {
    var failed = activeDownloads.value.failedIds()
    activeDownloads.collect { downloads ->
        downloads.forEach { (episodeId, status) ->
            if (status is DownloadStatus.Failed && episodeId !in failed) {
                emit(UiMessage(status.error.toMessage(fallback = Res.string.error_download)))
            }
        }
        failed = downloads.failedIds()
    }
}

private fun Map<String, DownloadStatus>.failedIds() = filterValues { it is DownloadStatus.Failed }.keys
