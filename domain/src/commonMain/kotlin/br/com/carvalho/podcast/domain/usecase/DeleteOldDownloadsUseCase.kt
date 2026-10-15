package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.core.util.getCurrentTimestamp
import br.com.carvalho.podcast.domain.download.EpisodeDownloader
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.days

/** The days Settings offer before a download is deleted (18.19); 0 keeps them. */
object DeleteOldDownloads {
    @Suppress("MagicNumber") // the choices themselves
    val CHOICES = listOf(0, 7, 14, 30)
}

/**
 * Deletes downloads older than Settings allow (18.19), each through the downloader as "Delete download" does. Saved
 * episodes and the one playing stay; downloads from before their date was kept have none and stay too.
 */
class DeleteOldDownloadsUseCase(
    private val repository: PodcastRepository,
    private val downloader: EpisodeDownloader,
    private val preferences: PreferencesRepository,
    private val audioPlayer: AudioPlayer,
    private val now: () -> Long = ::getCurrentTimestamp,
) {
    suspend operator fun invoke() {
        val days = preferences.deleteDownloadsAfterDays.value
        if (days <= 0) return
        val oldest = now() - days.days.inWholeMilliseconds
        val playing = audioPlayer.playerState.value.currentEpisode?.id
        repository.getDownloadedEpisodes().first()
            .filter { episode -> episode.downloadedAt?.let { it < oldest } == true }
            .filterNot { it.isSaved || it.id == playing }
            .forEach { downloader.delete(it.id) }
    }
}
