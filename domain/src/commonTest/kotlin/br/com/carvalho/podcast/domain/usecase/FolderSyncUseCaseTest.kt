package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FolderSyncUseCaseTest {

    @Test
    fun twoDevicesSyncThroughTheirOwnFilesInTheSameFolder() = runTest {
        val folder = FakeSyncFiles()
        val phone = Device("podcast-sync-phone.json")
        phone.repository.podcasts.value = listOf(podcast(FEED, subscribedAt = 1))
        phone.repository.episodes.value = listOf(episode("e1"))
        val laptop = Device("podcast-sync-laptop.json")
        laptop.feeds.resultsByUrl[FEED] = Result.success(FetchedFeed(podcast(FEED), listOf(episode("e1"))))

        phone.exchange(folder)
        laptop.exchange(folder)
        laptop.repository.applyEpisodeState("e1", played = true, position = 0, updatedAt = 10)
        laptop.exchange(folder)
        phone.exchange(folder)

        assertEquals(setOf(phone.fileName, laptop.fileName), folder.files.keys)
        assertEquals(listOf(FEED), laptop.repository.podcasts.value.map { it.feedUrl })
        assertTrue(phone.repository.episodes.value.single().isPlayed)
        assertTrue(phone.folderSync.status.value is FolderSyncStatus.Synced)
    }

    @Test
    fun anUnchangedSnapshotIsNotWrittenAgain() = runTest {
        val folder = FakeSyncFiles()
        val phone = Device("podcast-sync-phone.json")

        phone.exchange(folder)
        phone.exchange(folder)

        assertEquals(1, folder.writes)
    }

    @Test
    fun otherFilesAndOnesStillComingInAreLeftAlone() = runTest {
        val folder = FakeSyncFiles()
        folder.files["notes.json"] = """{"podcasts": [{"feedUrl": "$FEED", "subscribedAt": 1}]}"""
        folder.files["podcast-sync-cut.json"] = """{"podcasts": [{"feedUrl": "https:"""
        val phone = Device("podcast-sync-phone.json")

        phone.exchange(folder)

        assertEquals(emptyList(), phone.repository.podcasts.value)
        assertTrue(phone.folderSync.status.value is FolderSyncStatus.Synced)
    }

    @Test
    fun aFolderThatCannotBeReadSaysSo() = runTest {
        val folder = FakeSyncFiles().apply { fails = true }
        val phone = Device("podcast-sync-phone.json")

        phone.exchange(folder)

        assertEquals(FolderSyncStatus.Failed, phone.folderSync.status.value)
    }

    @Test
    fun eachNewDeviceGetsItsOwnFile() {
        assertNotEquals(FolderSyncUseCase.newFileName(), FolderSyncUseCase.newFileName())
        assertTrue(FolderSyncUseCase.newFileName().matches(Regex("podcast-sync-[0-9a-f]+\\.json")))
    }

    private class FakeSyncFiles : SyncFiles {
        val files = mutableMapOf<String, String>()
        var writes = 0
        var fails = false

        override suspend fun read(): Map<String, String> {
            if (fails) error("folder gone")
            return files.toMap()
        }

        override suspend fun write(name: String, text: String) {
            writes++
            files[name] = text
        }
    }

    private class Device(val fileName: String) {
        val repository = FakePodcastRepository()
        val feeds = FakeFeedSource()
        private val sync = LibrarySyncUseCase(
            repository,
            FakeAudioPlayer(),
            AddPodcastFromUrlUseCase(feeds, repository),
            DeletePodcastUseCase(repository, FakeEpisodeDownloader()),
        )
        val folderSync = FolderSyncUseCase(sync, now = { 1 })

        suspend fun exchange(folder: SyncFiles) = folderSync.exchange(folder, fileName)
    }

    private fun podcast(feedUrl: String, subscribedAt: Long = 0) = Podcast(
        id = feedUrl, title = feedUrl, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = feedUrl, siteUrl = null, lastUpdated = 0, isSubscribed = true,
        subscribedAt = subscribedAt,
    )

    private fun episode(id: String) = Episode(
        id = id, podcastId = FEED, title = id, description = null, audioUrl = "https://cdn.example.com/$id.mp3",
        imageUrl = null, duration = 0, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false,
        fileSize = null,
    )

    private companion object {
        const val FEED = "https://a.example.com/feed"
    }
}
