package br.com.carvalho.podcast.domain.repository

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePodcastRepository : PodcastRepository {
    val podcasts = MutableStateFlow<List<Podcast>>(emptyList())
    val episodes = MutableStateFlow<List<Episode>>(emptyList())
    
    var deletePodcastCalledWith: String? = null
        private set

    override fun getPodcasts(): Flow<List<Podcast>> = podcasts

    override suspend fun getPodcastById(id: String): Podcast? = podcasts.value.find { it.id == id }

    override fun getPodcastByIdFlow(id: String): Flow<Podcast?> = podcasts.map { it.find { p -> p.id == id } }

    override fun getEpisodes(podcastId: String): Flow<List<Episode>> = episodes.map { it.filter { e -> e.podcastId == podcastId } }

    override fun getEpisodesPaged(podcastId: String): Flow<PagingData<Episode>> {
        throw NotImplementedError("Paging not supported in fake")
    }

    override fun getDownloadedEpisodes(): Flow<List<Episode>> = episodes.map { list -> list.filter { it.isDownloaded } }

    override fun getUnplayedEpisodes(): Flow<List<Episode>> = episodes.map { list -> list.filter { !it.isPlayed } }

    override suspend fun getEpisodeById(id: String): Episode? = episodes.value.find { it.id == id }

    override fun searchEpisodes(query: String): Flow<List<Episode>> = episodes.map { it.filter { e -> e.title.contains(query, ignoreCase = true) } }

    override fun searchEpisodesPaged(query: String?): Flow<PagingData<Episode>> {
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

    var savePodcastCalledCount = 0
        private set
    var saveEpisodesCalledCount = 0
        private set

    override suspend fun savePodcast(podcast: Podcast) {
        savePodcastCalledCount++
        podcasts.value = podcasts.value + podcast
    }

    override suspend fun saveEpisodes(episodes: List<Episode>) {
        saveEpisodesCalledCount++
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
