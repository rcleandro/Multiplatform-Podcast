package br.com.carvalho.podcast.data.local.dao

import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeEpisodeDao : EpisodeDao {
    val episodes = MutableStateFlow<List<EpisodeEntity>>(emptyList())

    override fun getByPodcast(podcastId: String): Flow<List<EpisodeEntity>> = episodes.map { it.filter { e -> e.podcastId == podcastId } }

    override suspend fun getByPodcastPaged(podcastId: String, limit: Int, offset: Int): List<EpisodeEntity> =
        episodes.value.filter { it.podcastId == podcastId }.drop(offset).take(limit)

    override suspend fun getAllPaged(limit: Int, offset: Int): List<EpisodeEntity> =
        episodes.value.drop(offset).take(limit)

    override suspend fun getById(id: String): EpisodeEntity? = episodes.value.find { it.id == id }

    override fun getUnplayed(): Flow<List<EpisodeEntity>> = episodes.map { it.filter { !it.isPlayed } }

    override fun search(query: String): Flow<List<EpisodeEntity>> = episodes.map { it.filter { it.title.contains(query) || it.description?.contains(query) == true } }

    override suspend fun searchPaged(query: String, limit: Int, offset: Int): List<EpisodeEntity> =
        episodes.value.filter { it.title.contains(query) || it.description?.contains(query) == true }.drop(offset).take(limit)

    override suspend fun insertAll(episodes: List<EpisodeEntity>) {
        this.episodes.value = this.episodes.value + episodes
    }

    override suspend fun exists(id: String): Boolean = episodes.value.any { it.id == id }

    override suspend fun updatePlayback(id: String, played: Boolean, position: Long) {
        episodes.value = episodes.value.map {
            if (it.id == id) it.copy(isPlayed = played, playbackPosition = position) else it
        }
    }

    override fun getDownloaded(): Flow<List<EpisodeEntity>> = episodes.map { it.filter { it.isDownloaded } }

    override fun getUnplayedCount(podcastId: String): Flow<Int> = episodes.map { it.count { e -> e.podcastId == podcastId && !e.isPlayed } }

    override suspend fun deleteByPodcast(podcastId: String) {
        episodes.value = episodes.value.filter { it.podcastId != podcastId }
    }

    override suspend fun updateDownloadStatus(id: String, downloaded: Boolean) {
        episodes.value = episodes.value.map {
            if (it.id == id) it.copy(isDownloaded = downloaded) else it
        }
    }

    override suspend fun markOlderAsPlayed(podcastId: String, publishDate: Long) {
        episodes.value = episodes.value.map {
            if (it.podcastId == podcastId && it.publishDate <= publishDate) it.copy(isPlayed = true) else it
        }
    }
}
