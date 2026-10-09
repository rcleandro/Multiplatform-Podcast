package br.com.carvalho.podcast.core.player

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaConstants
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.first

internal const val ROOT_ID = "root"
internal const val LIBRARY_ID = "library_node"
internal const val PODCAST_PREFIX = "podcast_"

/**
 * The tree Android Auto and other media browsers walk: root → library → one folder per podcast → its episodes.
 * Episodes are addressed by their own id; the root item itself comes from [PodcastMediaService].
 */
internal class MediaTree(private val repository: PodcastRepository, private val libraryTitle: () -> String) {
    suspend fun children(parentId: String): List<MediaItem> = when {
        parentId == ROOT_ID -> listOf(libraryNode(MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM))
        parentId == LIBRARY_ID -> repository.getPodcasts().first().map {
            podcastItem(it, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM)
        }
        parentId.startsWith(PODCAST_PREFIX) -> {
            val podcastId = parentId.removePrefix(PODCAST_PREFIX)
            val podcast = repository.getPodcastById(podcastId)
            repository.getEpisodes(podcastId).first().map { episodeItem(it, podcast) }
        }
        else -> emptyList()
    }

    suspend fun item(mediaId: String): MediaItem? = when {
        mediaId == LIBRARY_ID -> libraryNode(contentStyle = null)
        mediaId.startsWith(PODCAST_PREFIX) ->
            repository.getPodcastById(mediaId.removePrefix(PODCAST_PREFIX))
                ?.let { podcastItem(it, contentStyle = null) }
        else -> repository.getEpisodeById(mediaId)?.let { episodeItem(it, repository.getPodcastById(it.podcastId)) }
    }

    private fun libraryNode(contentStyle: Int?): MediaItem = MediaItem.Builder()
        .setMediaId(LIBRARY_ID)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(libraryTitle())
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS)
                .withContentStyle(contentStyle)
                .build()
        )
        .build()
}

private fun podcastItem(podcast: Podcast, contentStyle: Int?): MediaItem = MediaItem.Builder()
    .setMediaId("$PODCAST_PREFIX${podcast.id}")
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(podcast.title)
            .setArtist(podcast.author)
            .setArtworkUri(podcast.imageUrl?.toUri())
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS)
            .withContentStyle(contentStyle)
            .build()
    )
    .build()

private fun episodeItem(episode: Episode, podcast: Podcast?): MediaItem = MediaItem.Builder()
    .setMediaId(episode.id)
    .setUri(episode.audioUrl)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(episode.title)
            .setArtist(podcast?.title ?: "")
            .setArtworkUri(episode.imageUrl?.toUri() ?: podcast?.imageUrl?.toUri())
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
            .build()
    )
    .build()

/** How Android Auto lays out a folder's children (grid or list); null keeps its default. */
private fun MediaMetadata.Builder.withContentStyle(contentStyle: Int?): MediaMetadata.Builder =
    if (contentStyle == null) {
        this
    } else {
        setExtras(Bundle().apply { putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, contentStyle) })
    }
