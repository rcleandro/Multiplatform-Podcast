package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.player.FakeAudioPlayer
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class DeleteOldDownloadsUseCaseTest {
    private val repository = FakePodcastRepository()
    private val downloader = FakeEpisodeDownloader()
    private val preferences = FakePreferencesRepository()
    private val player = FakeAudioPlayer()
    private val now = 100.days.inWholeMilliseconds
    private val deleteOld = DeleteOldDownloadsUseCase(repository, downloader, preferences, player, now = { now })

    @Test
    fun downloadsOlderThanSettingsAllowGoExceptSavedPlayingAndUndated() = runTest {
        preferences.setDeleteDownloadsAfterDays(7)
        val old = (now - 8.days.inWholeMilliseconds)
        repository.episodes.value = listOf(
            episode("old", downloadedAt = old),
            episode("recent", downloadedAt = now - 1.days.inWholeMilliseconds),
            episode("saved", downloadedAt = old).copy(isSaved = true),
            episode("playing", downloadedAt = old),
            episode("undated", downloadedAt = null),
        )
        player.play(episode("playing", downloadedAt = old))

        deleteOld()

        assertEquals(listOf("old"), downloader.deletedIds)
    }

    @Test
    fun keptWhenSettingsSaySo() = runTest {
        repository.episodes.value = listOf(episode("old", downloadedAt = 0))

        deleteOld()

        assertEquals(null, downloader.deleteCalledWith)
    }

    private fun episode(id: String, downloadedAt: Long?) = Episode(
        id = id, podcastId = "p", title = id, description = null, audioUrl = "a", imageUrl = null, duration = 60,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = true, fileSize = null,
        downloadedAt = downloadedAt,
    )
}
