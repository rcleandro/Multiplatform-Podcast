package br.com.carvalho.podcast.presentation.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RootComponentTest {

    private lateinit var lifecycle: LifecycleRegistry
    private lateinit var rootComponent: RootComponentImpl
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        lifecycle = LifecycleRegistry()
        lifecycle.resume()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createRootComponent() {
        rootComponent = RootComponentImpl(
            componentContext = DefaultComponentContext(lifecycle = lifecycle)
        )
    }

    @Test
    fun initial_state_should_be_Library() = runTest(testDispatcher) {
        createRootComponent()
        val child = rootComponent.stack.value.active.instance
        assertTrue(child is RootComponent.Child.Library)
    }

    @Test
    fun onSearchTabClicked_should_navigate_to_Search() = runTest(testDispatcher) {
        createRootComponent()
        rootComponent.onSearchTabClicked()
        val child = rootComponent.stack.value.active.instance
        assertTrue(child is RootComponent.Child.Search)
    }

    @Test
    fun onDownloadsTabClicked_should_navigate_to_DownloadedEpisodes() = runTest(testDispatcher) {
        createRootComponent()
        rootComponent.onDownloadsTabClicked()
        val child = rootComponent.stack.value.active.instance
        assertTrue(child is RootComponent.Child.DownloadedEpisodes)
    }

    @Test
    fun onPlayerTabClicked_should_navigate_to_Player() = runTest(testDispatcher) {
        createRootComponent()
        rootComponent.onPlayerTabClicked()
        val child = rootComponent.stack.value.active.instance
        assertTrue(child is RootComponent.Child.Player)
    }

    @Test
    fun onPodcastSelected_should_navigate_to_PodcastDetail() = runTest(testDispatcher) {
        createRootComponent()
        val podcastId = "test-podcast-id"
        rootComponent.onPodcastSelected(podcastId)

        val child = rootComponent.stack.value.active.instance
        assertTrue(child is RootComponent.Child.PodcastDetail)
        assertEquals(podcastId, child.podcastId)
    }

    @Test
    fun onEpisodeSelected_should_navigate_to_EpisodeDetail_and_keep_PodcastDetail_in_stack() = runTest(testDispatcher) {
        createRootComponent()
        val podcastId = "test-podcast-id"
        val episodeId = "test-episode-id"

        rootComponent.onEpisodeSelected(episodeId, podcastId)

        val stack = rootComponent.stack.value.items
        assertEquals(3, stack.size) // Library + PodcastDetail + EpisodeDetail

        val activeChild = rootComponent.stack.value.active.instance
        assertTrue(activeChild is RootComponent.Child.EpisodeDetail)
        assertEquals(episodeId, (activeChild as RootComponent.Child.EpisodeDetail).episodeId)
        assertEquals(podcastId, activeChild.podcastId)

        val podcastChild = stack[1].instance
        assertTrue(podcastChild is RootComponent.Child.PodcastDetail)
        assertEquals(podcastId, podcastChild.podcastId)
    }

    @Test
    fun onBackClicked_should_pop_the_stack() = runTest(testDispatcher) {
        createRootComponent()
        rootComponent.onSearchTabClicked()
        assertTrue(rootComponent.stack.value.active.instance is RootComponent.Child.Search)

        rootComponent.onBackClicked()
        assertTrue(rootComponent.stack.value.active.instance is RootComponent.Child.Library)
    }
}
