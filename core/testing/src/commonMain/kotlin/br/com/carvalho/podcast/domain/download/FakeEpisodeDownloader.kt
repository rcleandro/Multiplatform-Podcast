package br.com.carvalho.podcast.domain.download

import br.com.carvalho.podcast.domain.model.Episode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeEpisodeDownloader : EpisodeDownloader {
    override val activeDownloads = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())

    var usedBytes = 0L

    var downloadCalledWith: Episode? = null
    var deleteCalledWith: String? = null
    var cancelCalledWith: String? = null

    /** Downloaded files by episode id. */
    val localPaths = mutableMapOf<String, String>()

    override suspend fun download(episode: Episode) {
        downloadCalledWith = episode
    }

    override suspend fun cancel(episodeId: String) {
        cancelCalledWith = episodeId
    }

    override suspend fun delete(episodeId: String) {
        deleteCalledWith = episodeId
    }

    override fun getDownloadStatus(episodeId: String): StateFlow<DownloadStatus> {
        return MutableStateFlow(DownloadStatus.Idle).asStateFlow()
    }

    override suspend fun getLocalPath(episodeId: String): String? = localPaths[episodeId]

    override suspend fun usedBytes(): Long = usedBytes
}
