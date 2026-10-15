package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.model.BookmarkBackup
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.EpisodeBackup
import br.com.carvalho.podcast.domain.model.EpisodeRef
import br.com.carvalho.podcast.domain.model.LibraryBackup
import br.com.carvalho.podcast.domain.model.PodcastBackup
import br.com.carvalho.podcast.domain.model.PreferencesBackup
import br.com.carvalho.podcast.domain.player.AudioPlayer
import br.com.carvalho.podcast.domain.repository.PlayerRepository
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import br.com.carvalho.podcast.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val backupJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

/** A backup file and how many podcasts it holds. */
data class BackupExport(val text: String, val podcasts: Int)

/** The whole library as one file (19.11), for this app on another device or after reinstalling it. */
class ExportBackupUseCase(
    private val repository: PodcastRepository,
    private val playerRepository: PlayerRepository,
    private val preferences: PreferencesRepository,
    private val audioPlayer: AudioPlayer,
) {
    suspend operator fun invoke(): BackupExport {
        val podcasts = repository.getLibrary().first().map { it.podcast }.sortedBy { it.position }
        val feedUrls = podcasts.associate { it.id to it.feedUrl }
        fun Episode.ref() = feedUrls[podcastId]?.let { EpisodeRef(id, it, audioUrl) }
        val backup = LibraryBackup(
            podcasts = podcasts.map {
                PodcastBackup(
                    feedUrl = it.feedUrl,
                    title = it.title,
                    oldestFirst = it.oldestFirst,
                    playback = it.playback,
                    notifyNewEpisodes = it.notifyNewEpisodes,
                    autoDownloadCount = it.autoDownloadCount,
                )
            },
            episodes = repository.getEpisodesWithHistory().mapNotNull { episode ->
                episode.ref()?.let {
                    EpisodeBackup(it, episode.isPlayed, episode.playbackPosition, episode.isSaved, episode.lastPlayedAt)
                }
            },
            queue = audioPlayer.playerState.value.queue.mapNotNull { it.ref() },
            bookmarks = playerRepository.allBookmarks().mapNotNull { bookmark ->
                repository.getEpisodeById(bookmark.episodeId)?.ref()?.let {
                    BookmarkBackup(it, bookmark.positionMs, bookmark.note, bookmark.createdAt)
                }
            },
            playlists = repository.getSmartPlaylists().first(),
            preferences = PreferencesBackup.of(preferences),
        )
        return BackupExport(backupJson.encodeToString(LibraryBackup.serializer(), backup), podcasts.size)
    }
}

/**
 * Brings a backup back (19.11): subscribes to its podcasts, then adds what it knows to what this install knows, so
 * restoring an old backup never loses newer listening. Heard on either side stays heard; the backup's position only
 * goes to an episode not begun here; saved and the latest play time join. Each podcast's settings and the preferences
 * come from the backup, and its queue only when the queue here is empty. Returns null for a file that is not a backup.
 */
class RestoreBackupUseCase(
    private val repository: PodcastRepository,
    private val playerRepository: PlayerRepository,
    private val preferences: PreferencesRepository,
    private val audioPlayer: AudioPlayer,
    private val addPodcast: AddPodcastFromUrlUseCase,
) {
    suspend operator fun invoke(text: String, onProgress: (Int, Int) -> Unit = { _, _ -> }): OpmlImport? {
        val backup = read(text) ?: return null
        val results = subscribeAll(backup.podcasts.map { it.feedUrl }, addPodcast, onProgress)
        val byKey = repository.getPodcasts().first().associateBy { feedUrlKey(it.feedUrl) }
        // A podcast added now may live under the address its feed moved to; the result says which.
        val podcastIds = backup.podcasts.zip(results).mapNotNull { (podcast, result) ->
            (result.getOrNull() ?: byKey[feedUrlKey(podcast.feedUrl)])?.let { podcast.feedUrl to it.id }
        }.toMap()
        restorePodcasts(backup.podcasts, podcastIds)
        val episodes = EpisodeFinder(repository, podcastIds)
        backup.episodes.forEach { saved -> episodes.find(saved.episode)?.let { restoreEpisode(it, saved) } }
        restoreBookmarks(backup.bookmarks, episodes)
        if (audioPlayer.playerState.value.queue.isEmpty()) {
            audioPlayer.setQueue(backup.queue.mapNotNull { episodes.find(it) })
        }
        // A playlist with the same name is the same one: restoring twice adds nothing.
        val names = repository.getSmartPlaylists().first().map { it.name }.toSet()
        backup.playlists.filter { it.name !in names }.forEach { repository.saveSmartPlaylist(it.copy(id = 0)) }
        backup.preferences.applyTo(preferences)
        return OpmlImport.of(results)
    }

    private fun read(text: String): LibraryBackup? = try {
        backupJson.decodeFromString(LibraryBackup.serializer(), text)
            .takeIf { it.format == LibraryBackup.FORMAT && it.version <= LibraryBackup.VERSION }
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    /** Each podcast's own settings, and the backup's library order ahead of podcasts it does not know. */
    private suspend fun restorePodcasts(podcasts: List<PodcastBackup>, podcastIds: Map<String, String>) {
        podcasts.forEach { podcast ->
            val id = podcastIds[podcast.feedUrl] ?: return@forEach
            repository.setPodcastPlayback(id, podcast.playback)
            podcast.oldestFirst?.let { repository.setEpisodeOrder(id, it) }
            repository.setPodcastNotifications(id, podcast.notifyNewEpisodes)
            repository.setPodcastAutoDownload(id, podcast.autoDownloadCount)
        }
        val ordered = podcasts.mapNotNull { podcastIds[it.feedUrl] }.distinct()
        val others = repository.getPodcasts().first().sortedBy { it.position }.map { it.id } - ordered.toSet()
        repository.reorderLibrary(ordered + others)
    }

    private suspend fun restoreEpisode(here: Episode, saved: EpisodeBackup) {
        if (saved.isPlayed && !here.isPlayed) {
            repository.markEpisodeAsPlayed(here.id)
        } else if (!here.isPlayed && here.playbackPosition == 0L && saved.positionMs > 0) {
            repository.updateEpisodeProgress(here.id, saved.positionMs)
        }
        if (saved.isSaved && !here.isSaved) repository.setEpisodeSaved(here.id, true)
        val playedAt = saved.lastPlayedAt
        if (playedAt != null && playedAt > (here.lastPlayedAt ?: 0)) repository.markEpisodeStarted(here.id, playedAt)
    }

    /** Only bookmarks not already here: restoring the same backup twice adds nothing. */
    private suspend fun restoreBookmarks(bookmarks: List<BookmarkBackup>, episodes: EpisodeFinder) {
        bookmarks.groupBy { it.episode }.forEach { (ref, saved) ->
            val episode = episodes.find(ref) ?: return@forEach
            val here = playerRepository.bookmarks(episode.id).first().map { it.positionMs to it.note }.toSet()
            saved.filter { it.positionMs to it.note !in here }
                .forEach { playerRepository.addBookmark(episode.id, it.positionMs, it.note) }
        }
    }
}

/** The episodes of the restored podcasts, read once per podcast, by id first and by audio address otherwise. */
private class EpisodeFinder(private val repository: PodcastRepository, private val podcastIds: Map<String, String>) {
    private val loaded = mutableMapOf<String, List<Episode>>()

    suspend fun find(ref: EpisodeRef): Episode? {
        val podcastId = podcastIds[ref.feedUrl] ?: return null
        val episodes = loaded[podcastId] ?: repository.getEpisodes(podcastId).first().also { loaded[podcastId] = it }
        return episodes.firstOrNull { it.id == ref.id } ?: episodes.firstOrNull { it.audioUrl == ref.audioUrl }
    }
}
