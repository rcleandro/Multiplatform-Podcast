package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.first

/** The choices a podcast offers for downloading by itself (18.19); 0 is off. */
object AutoDownload {
    @Suppress("MagicNumber") // the choices themselves
    val CHOICES = listOf(0, 1, 3, 5)
}

/**
 * Keeps a podcast's newest unheard episodes downloaded, as many as it asks for (18.19): those missing are handed to
 * the downloader, which still waits for Wi-Fi and stays under the space limit when Settings say so.
 */
class AutoDownloadUseCase(
    private val repository: PodcastRepository,
    private val downloader: EpisodeDownloader,
) {
    suspend operator fun invoke(podcastId: String) {
        val count = repository.getPodcastById(podcastId)?.autoDownloadCount ?: return
        if (count <= 0) return
        val downloading = downloader.activeDownloads.value.keys
        repository.getEpisodes(podcastId).first()
            .filterNot { it.isPlayed }
            .sortedByDescending { it.publishDate }
            .take(count)
            .filterNot { it.isDownloaded || it.id in downloading }
            .forEach { downloader.download(it) }
    }
}
