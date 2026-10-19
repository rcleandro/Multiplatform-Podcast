package br.com.carvalho.podcast.core.player

import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.MediaQueueItem

/**
 * What a Chromecast plays (18.17): the episode's address on the web, never its downloaded file, and as audio, so the
 * receiver shows the cover and the title instead of waiting for video; a video episode keeps its type (19.10).
 */
@UnstableApi
internal class EpisodeCastConverter : MediaItemConverter {
    private val default = DefaultMediaItemConverter()

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val remote = mediaItem.requestMetadata.mediaUri ?: mediaItem.localConfiguration?.uri
        val mimeType = mediaItem.localConfiguration?.mimeType ?: audioMimeType(remote.toString())
        val item = mediaItem.buildUpon().setUri(remote).setMimeType(mimeType).build()
        return default.toMediaQueueItem(item)
    }

    override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem = default.toMediaItem(mediaQueueItem)
}
