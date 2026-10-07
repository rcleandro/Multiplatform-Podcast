package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import br.com.carvalho.podcast.domain.model.EpisodeListFilter
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.Podcast
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FakePodcastRepository : PodcastRepository {
    val podcasts = MutableStateFlow<List<Podcast>>(emptyList())
    val episodes = MutableStateFlow<List<Episode>>(emptyList())
    
    var deletePodcastCalledWith: String? = null
        private set

    /** When set, [getEpisodeById] throws it, as a failing database would. */
    var getEpisodeError: Exception? = null

    override fun getPodcasts(): Flow<List<Podcast>> = podcasts

    override suspend fun reorderLibrary(podcastIds: List<String>) {
        podcasts.value = podcasts.value.map { it.copy(position = podcastIds.indexOf(it.id)) }
    }

    override fun getLibrary(): Flow<List<LibraryEntry>> = combine(podcasts, episodes) { podcasts, episodes ->
        podcasts.sortedBy { it.title.lowercase() }.map { podcast ->
            val own = episodes.filter { it.podcastId == podcast.id }
            LibraryEntry(podcast, own.count { !it.isPlayed }, own.maxOfOrNull { it.publishDate })
        }
    }

    override suspend fun getPodcastById(id: String): Podcast? = podcasts.value.find { it.id == id }

    override fun getPodcastByIdFlow(id: String): Flow<Podcast?> = podcasts.map { it.find { p -> p.id == id } }

    override fun getEpisodes(podcastId: String): Flow<List<Episode>> = episodes.map { it.filter { e -> e.podcastId == podcastId } }

    override fun getEpisodesPaged(podcastId: String, filter: EpisodeFilter): Flow<PagingData<Episode>> {
        throw NotImplementedError("Paging not supported in fake")
    }

    override suspend fun getEpisodesSince(podcastId: String, publishDate: Long): List<Episode> =
        episodes.value.filter { it.podcastId == podcastId && it.publishDate >= publishDate }.sortedBy { it.publishDate }

    override fun getDownloadedEpisodes(): Flow<List<Episode>> = episodes.map { list -> list.filter { it.isDownloaded } }

    override fun getUnplayedEpisodes(): Flow<List<Episode>> = episodes.map { list -> list.filter { !it.isPlayed } }

    override fun getInProgressEpisodes(): Flow<List<Episode>> =
        episodes.map { list -> list.filter { it.playbackPosition > 0 && !it.isPlayed } }

    override suspend fun getEpisodeById(id: String): Episode? {
        getEpisodeError?.let { throw it }
        return episodes.value.find { it.id == id }
    }

    override fun searchEpisodes(query: String): Flow<List<Episode>> = episodes.map { it.filter { e -> e.title.contains(query, ignoreCase = true) } }

    override fun searchEpisodesPaged(query: String?, filter: EpisodeListFilter): Flow<PagingData<Episode>> {
        throw NotImplementedError("Paging not supported in fake")
    }

    override suspend fun updateEpisodeProgress(id: String, progress: Long) {
        episodes.value = episodes.value.map {
            if (it.id == id) it.copy(playbackPosition = progress) else it
        }
    }

    override suspend fun markEpisodeAsPlayed(id: String) {
        episodes.value = episodes.value.map {
            if (it.id == id) it.copy(isPlayed = true) else it
        }
    }

    override suspend fun markEpisodeAsUnplayed(id: String) {
        episodes.value = episodes.value.map {
            if (it.id == id) it.copy(isPlayed = false, playbackPosition = 0) else it
        }
    }

    var saveFeedCalledCount = 0
        private set

    override suspend fun saveFeed(podcast: Podcast, episodes: List<Episode>) {
        saveFeedCalledCount++
        podcasts.value = podcasts.value.filter { it.id != podcast.id } + podcast
        this.episodes.value = this.episodes.value + episodes
    }

    override suspend fun deletePodcast(id: String) {
        deletePodcastCalledWith = id
        podcasts.value = podcasts.value.filter { it.id != id }
    }

    override suspend fun markOlderEpisodesAsPlayed(podcastId: String, publishDate: Long) {
        episodes.value = episodes.value.map {
            if (it.podcastId == podcastId && it.publishDate < publishDate) it.copy(isPlayed = true) else it
        }
    }
}
