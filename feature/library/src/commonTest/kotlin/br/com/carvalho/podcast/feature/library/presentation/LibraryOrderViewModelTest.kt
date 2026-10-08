package br.com.carvalho.podcast.feature.library.presentation

import br.com.carvalho.podcast.core.util.CoroutineDispatchers
import br.com.carvalho.podcast.domain.model.LibraryEntry
import br.com.carvalho.podcast.domain.model.LibrarySort
import br.com.carvalho.podcast.domain.model.Podcast
import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import br.com.carvalho.podcast.domain.repository.FakePreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryOrderViewModelTest {
    private val repository = FakePodcastRepository()
    private val preferences = FakePreferencesRepository()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(main = testDispatcher, io = testDispatcher)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun podcast(title: String, position: Int) = Podcast(
        id = title, title = title, description = "", imageUrl = null, author = null, language = null,
        categories = emptyList(), feedUrl = title, siteUrl = null, lastUpdated = 0, isSubscribed = true,
        position = position,
    )

    @Test
    fun organizingSwitchesTheLibraryToTheCustomOrder() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(podcast("A", 1), podcast("B", 0))

        val viewModel = LibraryOrderViewModel(repository, preferences, dispatchers)

        assertEquals(LibrarySort.CUSTOM, preferences.librarySort.value)
        assertEquals(listOf("B", "A"), viewModel.podcasts.value.map { it.podcast.id })
    }

    @Test
    fun aDraggedPodcastIsFoundByItsKeyNotByItsPlaceInTheList() {
        val entries = listOf("A", "B", "C").map { LibraryEntry(podcast(it, 0), 0, null) }

        assertEquals(listOf("C", "A", "B"), entries.movedByKey("C", "A").map { it.podcast.id })
        assertEquals(listOf("A", "B", "C"), entries.movedByKey("hint", "A").map { it.podcast.id })
    }

    @Test
    fun aNewOrderIsSaved() = runTest(testDispatcher) {
        repository.podcasts.value = listOf(podcast("A", 0), podcast("B", 1), podcast("C", 2))
        val viewModel = LibraryOrderViewModel(repository, preferences, dispatchers)

        viewModel.reorder(listOf("C", "A", "B"))

        assertEquals(listOf("C", "A", "B"), viewModel.podcasts.value.map { it.podcast.id })
    }
}
