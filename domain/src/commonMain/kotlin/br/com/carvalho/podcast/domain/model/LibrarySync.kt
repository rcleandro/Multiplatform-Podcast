package br.com.carvalho.podcast.domain.model

import kotlinx.serialization.Serializable

/**
 * What one device tells another to sync the library (19.9b): the podcasts and when each came in, the ones taken out
 * and when, each episode's progress with when it changed, and the queue with when it was arranged. Downloads stay on
 * each device. Feed addresses go in whole, private ones too: the transport must be encrypted.
 */
@Serializable
data class SyncSnapshot(
    val podcasts: List<SyncPodcast> = emptyList(),
    val removed: List<SyncRemoval> = emptyList(),
    val episodes: List<SyncEpisode> = emptyList(),
    val queue: List<EpisodeRef> = emptyList(),
    val queueUpdatedAt: Long = 0,
)

@Serializable
data class SyncPodcast(val feedUrl: String, val subscribedAt: Long)

@Serializable
data class SyncRemoval(val feedUrl: String, val removedAt: Long)

@Serializable
data class SyncEpisode(val episode: EpisodeRef, val isPlayed: Boolean, val positionMs: Long, val updatedAt: Long)

/** What this device takes from another's [SyncSnapshot]; [queue] is null when the queue here stays. */
data class SyncPlan(
    val subscribe: List<String> = emptyList(),
    val unsubscribe: List<String> = emptyList(),
    val episodes: List<SyncEpisode> = emptyList(),
    val queue: List<EpisodeRef>? = null,
) {
    val isEmpty: Boolean get() = subscribe.isEmpty() && unsubscribe.isEmpty() && episodes.isEmpty() && queue == null
}

/**
 * The folder the user's own cloud or sync app carries between devices (19.12): [bookmark] is the platform's lasting
 * permission to it, [name] what Settings show, and [fileName] this device's snapshot file there.
 */
data class SyncFolder(val bookmark: String, val name: String, val fileName: String)
