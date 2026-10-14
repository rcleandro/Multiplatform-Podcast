package br.com.carvalho.podcast.feature.settings.presentation

import br.com.carvalho.podcast.core.util.AppDirectories
import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.core.util.supportsDownloads
import br.com.carvalho.podcast.domain.download.FakeEpisodeDownloader
import br.com.carvalho.podcast.domain.model.AppLanguage
import br.com.carvalho.podcast.domain.model.Episode
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.model.ThemeMode
import br.com.carvalho.podcast.domain.repository.FakeFeedSource
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import br.com.carvalho.podcast.domain.repository.FetchedFeed
import br.com.carvalho.podcast.domain.repository.OpmlFormat
import br.com.carvalho.podcast.domain.usecase.AddPodcastFromUrlUseCase
import br.com.carvalho.podcast.domain.usecase.ExportOpmlUseCase
import br.com.carvalho.podcast.domain.usecase.ImportOpmlUseCase
import br.com.carvalho.podcast.domain.usecase.OpmlImport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val preferences = FakePreferencesRepository()
    private val downloader = FakeEpisodeDownloader().apply { usedBytes = USED_BYTES }
    private val repository = FakePodcastRepository()
    private val feedSource = FakeFeedSource()

    /** One feed address per line: the view model is tested without XML. */
    private val opmlFormat = object : OpmlFormat {
        override fun read(opml: String) = opml.lines().filter { it.isNotBlank() }
        override fun write(podcasts: List<Podcast>) = podcasts.joinToString("\n") { it.feedUrl }
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        SettingsViewModel(
            preferences, downloader, AppDirectories(FakeFileSystem(), "/app".toPath()), repository,
            CoroutineDispatchers(main = dispatcher, io = dispatcher),
            ImportOpmlUseCase(opmlFormat, AddPodcastFromUrlUseCase(feedSource, repository)),
            ExportOpmlUseCase(repository, opmlFormat),
        )

    @Test
    fun theChosenThemeIsSavedAndShown() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetTheme(ThemeMode.DARK))

        assertEquals(ThemeMode.DARK, preferences.themeMode.value)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun theChosenLanguageIsSavedAndShown() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetLanguage(AppLanguage.SPANISH))

        assertEquals(AppLanguage.SPANISH, preferences.appLanguage.value)
        assertEquals(AppLanguage.SPANISH, viewModel.uiState.value.language)
    }

    @Test
    fun telemetryCanBeTurnedOff() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetTelemetry(false))

        assertFalse(preferences.telemetryEnabled.value)
        assertFalse(viewModel.uiState.value.telemetryEnabled)
    }

    @Test
    fun explicitContentCanBeHidden() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.showExplicit)

        viewModel.onIntent(SettingsIntent.SetShowExplicit(false))

        assertFalse(preferences.showExplicit.value)
        assertFalse(viewModel.uiState.value.showExplicit)
    }

    @Test
    fun wifiOnlyAndTheDownloadLimitAreSaved() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.SetWifiOnly(true))
        viewModel.onIntent(SettingsIntent.SetDownloadLimit(USED_BYTES * 2))

        assertEquals(true, viewModel.uiState.value.wifiOnlyDownloads)
        assertEquals(USED_BYTES * 2, preferences.downloadLimitBytes.value)

        viewModel.onIntent(SettingsIntent.SetDownloadNext(true))
        assertEquals(true, viewModel.uiState.value.downloadNextOnFinish)

        viewModel.onIntent(SettingsIntent.SetDeleteFinished(true))
        assertEquals(true, preferences.deleteFinishedDownloads.value)
        assertEquals(true, viewModel.uiState.value.deleteFinishedDownloads)
    }

    @Test
    fun theSpaceIsReadAgainWhenADownloadEndsOrIsDeleted() = runTest(dispatcher) {
        val viewModel = viewModel()

        downloader.usedBytes = USED_BYTES * 2
        repository.episodes.value = listOf(downloaded())

        assertEquals(if (supportsDownloads) USED_BYTES * 2 else null, viewModel.uiState.value.usedBytes)
    }

    @Test
    fun deletingAllDownloadsAsksFirstThenDeletesEach() = runTest(dispatcher) {
        repository.episodes.value = listOf(downloaded(), downloaded().copy(id = "e2"), downloaded().copy(id = "e3", isDownloaded = false))
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.RequestDeleteAll)
        assertEquals(true, viewModel.uiState.value.isConfirmingDeleteAll)
        viewModel.onIntent(SettingsIntent.DismissDeleteAll)
        assertEquals(emptyList(), downloader.deletedIds)

        viewModel.onIntent(SettingsIntent.RequestDeleteAll)
        viewModel.onIntent(SettingsIntent.ConfirmDeleteAll)

        assertEquals(false, viewModel.uiState.value.isConfirmingDeleteAll)
        assertEquals(listOf("e1", "e2"), downloader.deletedIds)
    }

    @Test
    fun theSpaceTheDownloadsTakeIsShownWhereThereAreDownloads() = runTest(dispatcher) {
        // The Web has no downloads, and no line for their space.
        assertEquals(if (supportsDownloads) USED_BYTES else null, viewModel().uiState.value.usedBytes)
    }

    private fun downloaded() = Episode(
        id = "e1", podcastId = "p", title = "E1", description = null, audioUrl = "a", imageUrl = null, duration = 60,
        publishDate = 0, isPlayed = false, playbackPosition = 0, isDownloaded = true, fileSize = null,
    )

    @Test
    fun importingAnOpmlFileSubscribesAndSaysHowItWent() = runTest(dispatcher) {
        feedSource.result = Result.success(FetchedFeed(podcast("https://new.example.com/feed"), emptyList()))
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.ImportOpml("https://new.example.com/feed\nnot a feed"))

        assertEquals(OpmlStatus.Imported(OpmlImport(added = 1, known = 0, failed = 1)), viewModel.uiState.value.opml)
        assertEquals(listOf("https://new.example.com/feed"), repository.podcasts.value.map { it.feedUrl })
    }

    @Test
    fun aFileThatCannotBeReadImportsNothing() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.ImportOpml(null))

        assertEquals(OpmlStatus.Imported(OpmlImport(0, 0, 0)), viewModel.uiState.value.opml)
    }

    @Test
    fun exportingHandsTheFileToTheScreenOnceAndSaysHowManyWereSaved() = runTest(dispatcher) {
        repository.podcasts.value = listOf(podcast("https://a.example.com/feed"), podcast("https://b.example.com/feed"))
        val viewModel = viewModel()

        viewModel.onIntent(SettingsIntent.ExportOpml)
        assertEquals(setOf("https://a.example.com/feed", "https://b.example.com/feed"), viewModel.uiState.value.opmlToSave?.lines()?.toSet())

        viewModel.onIntent(SettingsIntent.OpmlHandedOver)
        assertEquals(null, viewModel.uiState.value.opmlToSave)
        viewModel.onIntent(SettingsIntent.OpmlSaved(saved = true))
        assertEquals(OpmlStatus.Exported(2), viewModel.uiState.value.opml)
    }

    private fun podcast(feed: String) = Podcast(
        id = feed, title = feed, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = feed, siteUrl = null, lastUpdated = 0, isSubscribed = true,
    )

    private companion object {
        const val USED_BYTES = 52_428_800L
    }
}
