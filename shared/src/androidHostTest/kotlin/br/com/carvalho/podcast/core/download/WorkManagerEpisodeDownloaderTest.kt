package br.com.carvalho.podcast.core.download

import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.data.download.KtorEpisodeDownloader
import br.com.carvalho.podcast.data.local.dao.FakeEpisodeDao
import br.com.carvalho.podcast.data.mapper.toEntity
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.PodcastRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkManagerEpisodeDownloaderTest {
    private val context = RuntimeEnvironment.getApplication()
    private val workManager = WorkManagerTestInitHelper.initializeTestWorkManager(context)
        .let { WorkManager.getInstance(context) }
    private val episodeDao = FakeEpisodeDao()
    private val repository = FakePodcastRepository()
    private val requested = mutableListOf<String>()
    private val transfers = KtorEpisodeDownloader(
        HttpClient(
            MockEngine { request ->
                requested += request.url.toString()
                respondOk("audio")
            }
        ),
        episodeDao,
        AppDirectories(FakeFileSystem(), "/test".toPath()),
        // The fake file system has no disk behind it for Android to measure.
        freeDiskBytes = { null },
    )
    private val downloader = WorkManagerEpisodeDownloader(transfers, workManager)

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun anEpisodeWithMoreTextThanAWorkRequestHoldsIsStillQueued() = runTest {
        // The feed writes the title and the address; WorkManager refuses a request with more than 10 KB of data.
        val episode = episode(title = "t".repeat(TOO_LONG), audioUrl = "https://cdn.example.com/" + "a".repeat(TOO_LONG))

        downloader.download(episode)

        assertEquals(1, workManager.getWorkInfosForUniqueWork("download-${episode.id}").get().size)
    }

    @Test
    fun theWorkerDownloadsTheEpisodeItFindsByItsId() = runTest {
        val episode = episode(title = "Pilot", audioUrl = "https://cdn.example.com/pilot.mp3")
        repository.episodes.value = listOf(episode)
        episodeDao.insertAll(listOf(episode.toEntity()))
        startKoin { modules(module { single { transfers }; single<PodcastRepository> { repository } }) }
        val work = TestListenableWorkerBuilder<DownloadWorker>(context)
            .setInputData(workDataOf("episodeId" to episode.id))
            .build()

        assertEquals(ListenableWorker.Result.success(), work.doWork())

        assertEquals(listOf(episode.audioUrl), requested)
        assertEquals("e1.mp3", episodeDao.episodes.value.single().downloadFile)
    }

    @Test
    fun anEpisodeRemovedWhileItsDownloadWaitedIsGivenUp() = runTest {
        startKoin { modules(module { single { transfers }; single<PodcastRepository> { repository } }) }
        val work = TestListenableWorkerBuilder<DownloadWorker>(context)
            .setInputData(workDataOf("episodeId" to "gone"))
            .build()

        assertEquals(ListenableWorker.Result.failure(), work.doWork())
        assertEquals(emptyList<String>(), requested)
    }

    private fun episode(title: String, audioUrl: String) = Episode(
        id = "e1", podcastId = "p1", title = title, description = null, audioUrl = audioUrl, imageUrl = null,
        duration = 0, publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = false, fileSize = null,
    )

    private companion object {
        const val TOO_LONG = 20_000
    }
}
