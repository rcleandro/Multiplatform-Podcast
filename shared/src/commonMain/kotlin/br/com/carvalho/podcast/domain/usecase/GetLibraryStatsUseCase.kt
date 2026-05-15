package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetLibraryStatsUseCase(
    private val repository: PodcastRepository
) {
    operator fun invoke(): Flow<LibraryStats> {
        return combine(
            repository.getPodcasts(),
            repository.getUnplayedEpisodes(),
            repository.getDownloadedEpisodes()
        ) { podcasts, unplayed, downloaded ->
            LibraryStats(
                podcastCount = podcasts.size,
                unplayedCount = unplayed.size,
                downloadCount = downloaded.size
            )
        }
    }
}

data class LibraryStats(
    val podcastCount: Int,
    val unplayedCount: Int,
    val downloadCount: Int
)
