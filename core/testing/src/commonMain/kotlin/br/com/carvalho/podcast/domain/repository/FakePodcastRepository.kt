package br.com.carvalho.podcast.domain.repository

import androidx.paging.PagingData
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeFilter
import br.com.carvalho.podcast.domain.model.EpisodeListFilter
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastPlayback
import br.com.carvalho.podcast.domain.model.SmartPlaylist
import br.com.carvalho.podcast.domain.model.topCategories
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

    override fun getEpisodes(podcastId: String): Flow<List<Episode>> = episodes.map {
        it.filter { e -> e.podcastId == podcastId }
    }

    /** One page with every match, in the asked order, like the database query. */
    override fun getEpisodesPaged(
        podcastId: String,
        filter: EpisodeFilter,
        oldestFirst: Boolean,
    ): Flow<PagingData<Episode>> =
        episodes.map { list ->
            val matching = list.filter { it.podcastId == podcastId }.filter {
                when (filter) {
                    EpisodeFilter.ALL -> true
                    EpisodeFilter.UNPLAYED -> !it.isPlayed
                    EpisodeFilter.DOWNLOADED -> it.isDownloaded
                }
            }
            val newestFirst = matching.sortedByDescending { it.publishDate }
            PagingData.from(if (oldestFirst) newestFirst.reversed() else newestFirst)
        }

    override suspend fun setPodcastAutoDownload(podcastId: String, count: Int) {
        podcasts.value = podcasts.value.map { if (it.id == podcastId) it.copy(autoDownloadCount = count) else it }
    }

    override suspend fun setPodcastNotifications(podcastId: String, enabled: Boolean) {
        podcasts.value = podcasts.value.map { if (it.id == podcastId) it.copy(notifyNewEpisodes = enabled) else it }
    }

    override suspend fun getNewEpisodes(): List<Episode> =
        episodes.value.filter { it.isNew && !it.isPlayed }.sortedByDescending { it.publishDate }

    override suspend fun setPodcastPlayback(podcastId: String, playback: PodcastPlayback) {
        podcasts.value = podcasts.value.map { if (it.id == podcastId) it.copy(playback = playback) else it }
    }

    override suspend fun setEpisodeOrder(podcastId: String, oldestFirst: Boolean) {
        podcasts.value = podcasts.value.map { if (it.id == podcastId) it.copy(oldestFirst = oldestFirst) else it }
    }

    override suspend fun dismissNewEpisode(id: String) {
        episodes.value = episodes.value.map { if (it.id == id) it.copy(isNew = false) else it }
    }

    override suspend fun markEpisodeStarted(id: String, at: Long) {
        episodes.value = episodes.value.map { if (it.id == id) it.copy(isNew = false, lastPlayedAt = at) else it }
    }

    override suspend fun clearHistory() {
        episodes.value = episodes.value.map { it.copy(lastPlayedAt = null) }
    }

    override suspend fun dismissAllNewEpisodes() {
        episodes.value = episodes.value.map { it.copy(isNew = false) }
    }

    override fun getDownloadedEpisodes(): Flow<List<Episode>> = episodes.map { list -> list.filter { it.isDownloaded } }

    override fun getUnplayedEpisodes(): Flow<List<Episode>> = episodes.map { list -> list.filter { !it.isPlayed } }

    override fun getEpisodeCount(podcastId: String): Flow<Int> =
        episodes.map { list -> list.count { it.podcastId == podcastId } }

    override fun getLatestUnplayedEpisode(podcastId: String): Flow<Episode?> = episodes.map { list ->
        list.filter { it.podcastId == podcastId && !it.isPlayed }.maxByOrNull { it.publishDate }
    }

    override fun getInProgressEpisodes(): Flow<List<Episode>> =
        episodes.map { list -> list.filter { it.playbackPosition > 0 && !it.isPlayed } }

    override suspend fun getEpisodeById(id: String): Episode? {
        getEpisodeError?.let { throw it }
        return episodes.value.find { it.id == id }
    }

    val smartPlaylists = MutableStateFlow<List<SmartPlaylist>>(emptyList())

    override fun getSmartPlaylists(): Flow<List<SmartPlaylist>> = smartPlaylists

    override suspend fun saveSmartPlaylist(playlist: SmartPlaylist) {
        val nextId = (smartPlaylists.value.maxOfOrNull { it.id } ?: 0) + 1
        val saved = if (playlist.id == 0L) playlist.copy(id = nextId) else playlist
        smartPlaylists.value = smartPlaylists.value.filter { it.id != saved.id } + saved
    }

    override suspend fun deleteSmartPlaylist(id: Long) {
        smartPlaylists.value = smartPlaylists.value.filter { it.id != id }
    }

    override fun getSmartPlaylistEpisodesPaged(playlist: SmartPlaylist): Flow<PagingData<Episode>> =
        episodes.map { PagingData.from(getSmartPlaylistEpisodes(playlist)) }

    /** The same conditions as the database query. */
    override suspend fun getSmartPlaylistEpisodes(playlist: SmartPlaylist): List<Episode> {
        val inCategory = podcasts.value.filter { playlist.category == null || playlist.category in it.topCategories }
            .map { it.id }.toSet()
        val maxSeconds = playlist.maxMinutes?.let { it * 60L }
        return episodes.value.filter {
            it.podcastId in inCategory && (!playlist.unplayedOnly || !it.isPlayed) &&
                (!playlist.downloadedOnly || it.isDownloaded) &&
                (maxSeconds == null || it.duration in 1..maxSeconds)
        }.sortedByDescending { it.publishDate }
    }

    override suspend fun getEpisodesWithHistory(): List<Episode> = episodes.value.filter {
        it.isPlayed || it.playbackPosition > 0 || it.isSaved || it.lastPlayedAt != null
    }
    override fun searchEpisodes(query: String): Flow<List<Episode>> = episodes.map {
        it.filter { e ->
            e.title.contains(
                query,
                ignoreCase = true
            )
        }
    }

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

    override suspend fun setEpisodeSaved(id: String, saved: Boolean) {
        episodes.value = episodes.value.map { if (it.id == id) it.copy(isSaved = saved) else it }
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

    /** Feed address and when, for each podcast [deletePodcast] took out; [clock] stamps it. */
    val removed = mutableMapOf<String, Long>()
    var clock = 0L

    override suspend fun deletePodcast(id: String) {
        deletePodcastCalledWith = id
        podcasts.value.find { it.id == id }?.let { removed[it.feedUrl] = clock }
        podcasts.value = podcasts.value.filter { it.id != id }
    }

    override suspend fun getRemovedPodcasts(): Map<String, Long> = removed.toMap()

    override suspend fun getSyncedEpisodes(): List<Episode> = episodes.value.filter { it.updatedAt > 0 }

    override fun lastEpisodeChange(): Flow<Long> = episodes.map { list -> list.maxOfOrNull { it.updatedAt } ?: 0 }

    override suspend fun applyEpisodeState(id: String, played: Boolean, position: Long, updatedAt: Long) {
        episodes.value = episodes.value.map {
            if (it.id == id) it.copy(isPlayed = played, playbackPosition = position, updatedAt = updatedAt) else it
        }
    }

    override suspend fun markOlderEpisodesAsPlayed(podcastId: String, publishDate: Long) {
        episodes.value = episodes.value.map {
            if (it.podcastId == podcastId && it.publishDate <= publishDate) it.copy(isPlayed = true) else it
        }
    }

    override suspend fun markOlderEpisodesAsUnplayed(podcastId: String, publishDate: Long) {
        episodes.value = episodes.value.map {
            if (it.podcastId == podcastId && it.publishDate <= publishDate) {
                it.copy(isPlayed = false, playbackPosition = 0)
            } else {
                it
            }
        }
    }
}
