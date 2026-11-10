package br.com.carvalho.podcast.core.player

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaConstants
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.PodcastPlayback
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.usecase.ExplicitRule
import kotlinx.coroutines.flow.first

internal const val ROOT_ID = "root"
internal const val LIBRARY_ID = "library_node"
internal const val PODCAST_PREFIX = "podcast_"

/**
 * The tree Android Auto and other media browsers walk: root → library → one folder per podcast → its episodes. The
 * library opens as the app's does: "Continue listening" with what was left halfway, then the podcasts.
 * Episodes are addressed by their own id; the root item itself comes from [PodcastMediaService].
 */
internal class MediaTree(
    private val repository: PodcastRepository,
    /** What may show and play (26.3): the car lists what the app's own screens list. */
    private val rule: () -> ExplicitRule = { ExplicitRule() },
    /** An episode's downloaded file, when it is still there. */
    private val localPath: suspend (episodeId: String) -> String? = { null },
    /** A text from this module's Android resources (`R.string`), in the phone's language. */
    private val text: (resId: Int) -> String,
) {
    suspend fun children(parentId: String): List<MediaItem> = when {
        parentId == ROOT_ID -> listOf(libraryNode(MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM))
        parentId == LIBRARY_ID -> {
            val started = repository.getInProgressEpisodes().first().filter { rule().allows(it) }
            // The podcasts get their title only under the other group: alone, the screen's own title says it.
            val podcastsTitle = if (started.isEmpty()) null else text(R.string.library_podcasts)
            started.map { episodeItem(it, podcastOf(it), group = text(R.string.continue_listening)) } +
                podcasts().map { podcastItem(it, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM, podcastsTitle) }
        }
        parentId.startsWith(PODCAST_PREFIX) -> {
            val podcastId = parentId.removePrefix(PODCAST_PREFIX)
            val podcast = repository.getPodcastById(podcastId)
            repository.getEpisodes(podcastId).first().filter { rule().allows(it) }.map { episodeItem(it, podcast) }
        }
        else -> emptyList()
    }

    suspend fun item(mediaId: String): MediaItem? = when {
        mediaId == LIBRARY_ID -> libraryNode(contentStyle = null)
        mediaId.startsWith(PODCAST_PREFIX) ->
            repository.getPodcastById(mediaId.removePrefix(PODCAST_PREFIX))
                ?.takeIf { rule().allows(it) }
                ?.let { podcastItem(it, contentStyle = null) }
        else -> repository.getEpisodeById(mediaId)?.takeIf { rule().allows(it) }?.let { episodeItem(it, podcastOf(it)) }
    }

    /**
     * What a voice search ("play X on Podcast", 20.7) plays, first item first: with no words, what was left halfway;
     * a podcast's name, its newest unheard episode; anything else, the saved episodes whose title has the words.
     */
    suspend fun search(query: String): List<MediaItem> {
        val words = query.trim()
        val podcast = podcasts().firstOrNull { words.isNotEmpty() && it.title.contains(words, ignoreCase = true) }
        val newest = podcast?.let { repository.getLatestUnplayedEpisode(it.id).first() }
        val episodes = when {
            words.isEmpty() -> repository.getInProgressEpisodes().first()
            newest != null -> listOf(newest)
            else -> repository.searchEpisodes(words).first()
        }
        return episodes.filter { rule().allows(it) }.map { episodeItem(it, podcastOf(it)) }
    }

    /**
     * Where an episode the car or a voice search asks for starts, as it would from the app. Handed over with the
     * item: Media3 refuses a seek while a stream is still loading, and the episode would start over.
     */
    suspend fun startPositionMs(mediaId: String): Long {
        val episode = repository.getEpisodeById(mediaId) ?: return 0
        return episode.startMs(podcastOf(episode)?.playback ?: PodcastPlayback())
    }

    private suspend fun podcasts(): List<Podcast> = repository.getPodcasts().first().filter { rule().allows(it) }

    private suspend fun podcastOf(episode: Episode): Podcast? = repository.getPodcastById(episode.podcastId)

    private suspend fun episodeItem(episode: Episode, podcast: Podcast?, group: String? = null): MediaItem {
        // A downloaded episode plays from its file, as in the app; a Chromecast needs the address it came from.
        val file = if (episode.isDownloaded) localPath(episode.id) else null
        return MediaItem.Builder()
            .setMediaId(episode.id)
            .setUri(episode.copy(localPath = file).playableUri())
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(episode.audioUrl.toUri()).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setArtist(podcast?.title ?: "")
                    .setArtworkUri(episode.imageUrl?.toUri() ?: podcast?.imageUrl?.toUri())
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
                    .withCarExtras(group = group)
                    .build()
            )
            .build()
    }

    private fun libraryNode(contentStyle: Int?): MediaItem = MediaItem.Builder()
        .setMediaId(LIBRARY_ID)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(text(R.string.library_title))
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS)
                .withCarExtras(contentStyle)
                .build()
        )
        .build()
}

private fun podcastItem(podcast: Podcast, contentStyle: Int?, group: String? = null): MediaItem = MediaItem.Builder()
    .setMediaId("$PODCAST_PREFIX${podcast.id}")
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(podcast.title)
            .setArtist(podcast.author)
            .setArtworkUri(podcast.imageUrl?.toUri())
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS)
            .withCarExtras(contentStyle, group)
            .build()
    )
    .build()

/** How Android Auto lays out a folder's children (grid or list), and the titled group an item is listed under. */
private fun MediaMetadata.Builder.withCarExtras(
    contentStyle: Int? = null,
    group: String? = null,
): MediaMetadata.Builder {
    if (contentStyle == null && group == null) return this
    return setExtras(
        Bundle().apply {
            contentStyle?.let { putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, it) }
            group?.let { putString(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE, it) }
        }
    )
}
