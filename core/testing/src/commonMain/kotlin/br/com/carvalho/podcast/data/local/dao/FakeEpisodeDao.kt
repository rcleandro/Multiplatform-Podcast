package br.com.carvalho.podcast.data.local.dao

import androidx.paging.PagingSource
import br.com.carvalho.podcast.data.local.entity.EpisodeEntity
import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import br.com.carvalho.podcast.data.local.entity.PodcastFeedFields
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Suppress("TooManyFunctions") // mirrors the whole EpisodeDao interface
class FakeEpisodeDao : EpisodeDao {
    val episodes = MutableStateFlow<List<EpisodeEntity>>(emptyList())

    override fun getByPodcast(podcastId: String): Flow<List<EpisodeEntity>> =
        episodes.map { it.filter { e -> e.podcastId == podcastId } }

    override fun pagingSourceByPodcast(
        podcastId: String,
        onlyUnplayed: Boolean,
        onlyDownloaded: Boolean,
    ): PagingSource<Int, EpisodeEntity> = throw NotImplementedError("Paging is tested against the real database")

    override fun searchPagingSource(
        query: String,
        onlyInProgress: Boolean,
        onlyDownloaded: Boolean,
    ): PagingSource<Int, EpisodeEntity> =
        throw NotImplementedError("Paging is tested against the real database")

    override suspend fun getSince(podcastId: String, publishDate: Long): List<EpisodeEntity> =
        episodes.value.filter { it.podcastId == podcastId && it.publishDate >= publishDate }.sortedBy { it.publishDate }

    override suspend fun getById(id: String): EpisodeEntity? = episodes.value.find { it.id == id }

    override fun getUnplayed(): Flow<List<EpisodeEntity>> = episodes.map { it.filter { !it.isPlayed } }

    override fun getInProgress(limit: Int): Flow<List<EpisodeEntity>> = episodes.map { list ->
        list.filter { it.playbackPosition > 0 && !it.isPlayed }.sortedByDescending { it.publishDate }.take(limit)
    }

    override fun search(query: String): Flow<List<EpisodeEntity>> = episodes.map { it.filter { e -> e.matches(query) } }


    override suspend fun insertAll(episodes: List<EpisodeEntity>) {
        val known = this.episodes.value.map { it.id }.toSet()
        this.episodes.value = this.episodes.value + episodes.filter { it.id !in known }
    }

    override suspend fun updateFeedFields(
        id: String,
        podcastTitle: String?,
        title: String,
        description: String?,
        audioUrl: String,
        imageUrl: String?,
        duration: Long,
        publishDate: Long,
    ) {
        episodes.value = episodes.value.map {
            if (it.id != id) {
                it
            } else {
                it.copy(
                    podcastTitle = podcastTitle, title = title, description = description, audioUrl = audioUrl,
                    imageUrl = imageUrl, duration = duration, publishDate = publishDate,
                )
            }
        }
    }

    override suspend fun deleteWithoutAudio(podcastId: String) {
        episodes.value = episodes.value.filterNot { it.podcastId == podcastId && it.audioUrl.isEmpty() }
    }

    override suspend fun exists(id: String): Boolean = episodes.value.any { it.id == id }

    override suspend fun updatePlayback(id: String, played: Boolean, position: Long) {
        episodes.value = episodes.value.map {
            if (it.id == id) it.copy(isPlayed = played, playbackPosition = position) else it
        }
    }

    override fun getDownloaded(): Flow<List<EpisodeEntity>> = episodes.map { it.filter { it.isDownloaded } }

    override fun countByPodcast(podcastId: String): Flow<Int> =
        episodes.map { list -> list.count { it.podcastId == podcastId } }

    override fun getLatestUnplayed(podcastId: String): Flow<EpisodeEntity?> = episodes.map { list ->
        list.filter { it.podcastId == podcastId && !it.isPlayed }.maxByOrNull { it.publishDate }
    }

    override fun getUnplayedCount(podcastId: String): Flow<Int> =
        episodes.map { it.count { e -> e.podcastId == podcastId && !e.isPlayed } }

    val podcasts = MutableStateFlow<List<PodcastEntity>>(emptyList())

    override suspend fun insertPodcastIfNew(podcast: PodcastEntity): Long {
        if (podcasts.value.any { it.id == podcast.id }) return -1L
        podcasts.value = podcasts.value + podcast
        return podcasts.value.size.toLong()
    }

    override suspend fun nextPodcastPosition(): Int = (podcasts.value.maxOfOrNull { it.position } ?: -1) + 1

    override suspend fun updatePodcast(fields: PodcastFeedFields) {
        podcasts.value = podcasts.value.map {
            if (it.id != fields.id) {
                it
            } else {
                it.copy(
                    title = fields.title, description = fields.description, imageUrl = fields.imageUrl,
                    author = fields.author, language = fields.language, categories = fields.categories,
                    feedUrl = fields.feedUrl, siteUrl = fields.siteUrl, lastUpdated = fields.lastUpdated,
                    etag = fields.etag, lastModified = fields.lastModified,
                )
            }
        }
    }

    override suspend fun updateDownloadFile(id: String, fileName: String?) {
        episodes.value = episodes.value.map {
            if (it.id == id) it.copy(downloadFile = fileName, isDownloaded = fileName != null) else it
        }
    }

    override suspend fun markOlderAsPlayed(podcastId: String, publishDate: Long) {
        episodes.value = episodes.value.map {
            if (it.podcastId == podcastId && it.publishDate <= publishDate) it.copy(isPlayed = true) else it
        }
    }

    override suspend fun markOlderAsUnplayed(podcastId: String, publishDate: Long) {
        episodes.value = episodes.value.map {
            if (it.podcastId == podcastId && it.publishDate <= publishDate) {
                it.copy(isPlayed = false, playbackPosition = 0)
            } else {
                it
            }
        }
    }
}

private fun EpisodeEntity.matches(query: String) = title.contains(query) || description?.contains(query) == true
