package br.com.carvalho.podcast.domain.download

import br.com.carvalho.podcast.domain.model.Episode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeEpisodeDownloader : EpisodeDownloader {
    private val _activeDownloads = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    override val activeDownloads: StateFlow<Map<String, DownloadStatus>> = _activeDownloads.asStateFlow()

    var downloadCalledWith: Episode? = null
    var deleteCalledWith: String? = null

    override suspend fun download(episode: Episode) {
        downloadCalledWith = episode
    }

    override suspend fun pause(episodeId: String) {}

    override suspend fun resume(episodeId: String) {}

    override suspend fun cancel(episodeId: String) {}

    override suspend fun delete(episodeId: String) {
        deleteCalledWith = episodeId
    }

    override fun getDownloadStatus(episodeId: String): StateFlow<DownloadStatus> {
        return MutableStateFlow(DownloadStatus.Idle).asStateFlow()
    }

    override fun getLocalPath(episodeId: String): String? = null
}
