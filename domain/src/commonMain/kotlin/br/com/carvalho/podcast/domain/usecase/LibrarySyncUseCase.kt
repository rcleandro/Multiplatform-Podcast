package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeRef
import br.com.carvalho.podcast.domain.model.SyncEpisode
import br.com.carvalho.podcast.domain.model.SyncPlan
import br.com.carvalho.podcast.domain.model.SyncPodcast
import br.com.carvalho.podcast.domain.model.SyncRemoval
import br.com.carvalho.podcast.domain.model.SyncSnapshot
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * What [local] takes from [remote] (19.9b). Podcasts join, unless one side took a podcast out after the other added
 * it; each episode takes the newer change; the queue takes the one arranged last. Taking only what is newer, with
 * the time it had there, means two devices agree after one exchange each way and then have nothing to send.
 */
fun planSync(local: SyncSnapshot, remote: SyncSnapshot): SyncPlan {
    val here = local.podcasts.associateBy { feedUrlKey(it.feedUrl) }
    val there = remote.podcasts.associateBy { feedUrlKey(it.feedUrl) }
    val removedHere = local.removed.associate { feedUrlKey(it.feedUrl) to it.removedAt }
    val removedThere = remote.removed.associate { feedUrlKey(it.feedUrl) to it.removedAt }
    val subscribe = there.filter { (key, podcast) ->
        key !in here && podcast.subscribedAt > (removedHere[key] ?: Long.MIN_VALUE)
    }
    val unsubscribe = here.filter { (key, podcast) ->
        key !in there && (removedThere[key] ?: Long.MIN_VALUE) > podcast.subscribedAt
    }
    val byId = local.episodes.associateBy { it.episode.id }
    val byAudio = local.episodes.associateBy { it.episode.audioUrl }
    val episodes = remote.episodes.filter { theirs ->
        val ours = byId[theirs.episode.id] ?: byAudio[theirs.episode.audioUrl]
        ours == null || theirs.updatedAt > ours.updatedAt
    }
    val queue = remote.queue.takeIf {
        remote.queueUpdatedAt > local.queueUpdatedAt && it.map(EpisodeRef::id) != local.queue.map(EpisodeRef::id)
    }
    return SyncPlan(subscribe.values.map { it.feedUrl }, unsubscribe.values.map { it.feedUrl }, episodes, queue)
}

/** This device's side of the library sync (19.9b); the platform carries the snapshots between devices. */
class LibrarySyncUseCase(
    private val repository: PodcastRepository,
    private val audioPlayer: AudioPlayer,
    private val addPodcast: AddPodcastFromUrlUseCase,
    private val deletePodcast: DeletePodcastUseCase,
) {
    /** Emits whenever something a snapshot holds may have changed. */
    fun changes(): Flow<Unit> = combine(
        repository.getPodcasts(),
        repository.lastEpisodeChange(),
        audioPlayer.playerState.map { it.queueUpdatedAt },
    ) { _, _, _ -> }

    suspend fun snapshot(): SyncSnapshot {
        val podcasts = repository.getPodcasts().first()
        val feedUrls = podcasts.associate { it.id to it.feedUrl }
        fun Episode.ref() = feedUrls[podcastId]?.let { EpisodeRef(id, it, audioUrl) }
        val player = audioPlayer.playerState.value
        return SyncSnapshot(
            podcasts = podcasts.map { SyncPodcast(it.feedUrl, it.subscribedAt) },
            removed = repository.getRemovedPodcasts().map { (feedUrl, at) -> SyncRemoval(feedUrl, at) },
            episodes = repository.getSyncedEpisodes().mapNotNull { episode ->
                episode.ref()?.let { SyncEpisode(it, episode.isPlayed, episode.playbackPosition, episode.updatedAt) }
            },
            queue = player.queue.mapNotNull { it.ref() },
            queueUpdatedAt = player.queueUpdatedAt,
        )
    }

    /** Takes what [remote] knows newer; episodes of feeds not read here yet wait for the next exchange. */
    suspend fun apply(remote: SyncSnapshot): SyncPlan {
        val plan = planSync(snapshot(), remote)
        if (plan.isEmpty) return plan
        val byKey = repository.getPodcasts().first().associateBy { feedUrlKey(it.feedUrl) }
        plan.unsubscribe.forEach { feedUrl -> byKey[feedUrlKey(feedUrl)]?.let { deletePodcast(it.id) } }
        subscribeAll(plan.subscribe, addPodcast) { _, _ -> }
        val podcasts = repository.getPodcasts().first().associateBy { feedUrlKey(it.feedUrl) }
        val refs = plan.episodes.map { it.episode } + plan.queue.orEmpty()
        val podcastIds = refs.map { it.feedUrl }.distinct()
            .mapNotNull { feedUrl -> podcasts[feedUrlKey(feedUrl)]?.let { feedUrl to it.id } }.toMap()
        val episodes = EpisodeFinder(repository, podcastIds)
        plan.episodes.forEach { theirs ->
            episodes.find(theirs.episode)?.let {
                repository.applyEpisodeState(it.id, theirs.isPlayed, theirs.positionMs, theirs.updatedAt)
            }
        }
        plan.queue?.let { queue -> audioPlayer.setQueue(queue.mapNotNull { episodes.find(it) }) }
        return plan
    }
}
