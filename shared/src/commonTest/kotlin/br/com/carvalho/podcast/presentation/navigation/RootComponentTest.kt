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
import kotlinx.coroutines.withContext
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
        withContext(Dispatchers.Main.immediate) {
            createRootComponent()
            val child = rootComponent.stack.value.active.instance
            assertTrue(child is RootComponent.Child.Library)
        }
    }

    @Test
    fun onSearchTabClicked_should_navigate_to_Search() = runTest(testDispatcher) {
        withContext(Dispatchers.Main.immediate) {
            createRootComponent()
            rootComponent.onSearchTabClicked()
            val child = rootComponent.stack.value.active.instance
            assertTrue(child is RootComponent.Child.Search)
        }
    }

    @Test
    fun onDownloadsTabClicked_should_navigate_to_DownloadedEpisodes() = runTest(testDispatcher) {
        withContext(Dispatchers.Main.immediate) {
            createRootComponent()
            rootComponent.onDownloadsTabClicked()
            val child = rootComponent.stack.value.active.instance
            assertTrue(child is RootComponent.Child.DownloadedEpisodes)
        }
    }

    @Test
    fun onPlayerTabClicked_should_navigate_to_Player() = runTest(testDispatcher) {
        withContext(Dispatchers.Main.immediate) {
            createRootComponent()
            rootComponent.onPlayerTabClicked()
            val child = rootComponent.stack.value.active.instance
            assertTrue(child is RootComponent.Child.Player)
        }
    }

    @Test
    fun onPodcastSelected_should_navigate_to_PodcastDetail() = runTest(testDispatcher) {
        withContext(Dispatchers.Main.immediate) {
            createRootComponent()
            val podcastId = "test-podcast-id"
            rootComponent.onPodcastSelected(podcastId)

            val child = rootComponent.stack.value.active.instance
            assertTrue(child is RootComponent.Child.PodcastDetail)
            assertEquals(podcastId, child.podcastId)
        }
    }

    @Test
    fun onEpisodeSelected_should_navigate_to_EpisodeDetail_and_keep_PodcastDetail_in_stack() = runTest(testDispatcher) {
        withContext(Dispatchers.Main.immediate) {
            createRootComponent()
            val podcastId = "test-podcast-id"
            val episodeId = "test-episode-id"

            rootComponent.onEpisodeSelected(episodeId, podcastId)

            val stack = rootComponent.stack.value.items
            assertEquals(3, stack.size)

            val activeChild = rootComponent.stack.value.active.instance
            assertTrue(activeChild is RootComponent.Child.EpisodeDetail)
            assertEquals(episodeId, (activeChild).episodeId)
            assertEquals(podcastId, activeChild.podcastId)

            val podcastChild = stack[1].instance
            assertTrue(podcastChild is RootComponent.Child.PodcastDetail)
            assertEquals(podcastId, podcastChild.podcastId)
        }
    }

    @Test
    fun onBackClicked_should_pop_the_stack() = runTest(testDispatcher) {
        withContext(Dispatchers.Main.immediate) {
            createRootComponent()
            rootComponent.onSearchTabClicked()
            assertTrue(rootComponent.stack.value.active.instance is RootComponent.Child.Search)

            rootComponent.onBackClicked()
            assertTrue(rootComponent.stack.value.active.instance is RootComponent.Child.Library)
        }
    }

    @Test
    fun switching_tabs_should_clear_details() = runTest(testDispatcher) {
        withContext(Dispatchers.Main.immediate) {
            createRootComponent()
            rootComponent.onEpisodeSelected("e1", "p1")
            assertEquals(3, rootComponent.stack.value.items.size)

            rootComponent.onSearchTabClicked()
            val stack = rootComponent.stack.value.items
            assertEquals(2, stack.size)
            assertTrue(stack[0].instance is RootComponent.Child.Library)
            assertTrue(stack[1].instance is RootComponent.Child.Search)
        }
    }

    @Test
    fun selecting_podcast_should_clear_previous_episodes() = runTest(testDispatcher) {
        withContext(Dispatchers.Main.immediate) {
            createRootComponent()
            rootComponent.onEpisodeSelected("e1", "p1")
            assertEquals(3, rootComponent.stack.value.items.size)

            rootComponent.onPodcastSelected("p2")
            val stack = rootComponent.stack.value.items
            assertEquals(2, stack.size)
            assertTrue(stack[0].instance is RootComponent.Child.Library)
            assertTrue(stack[1].instance is RootComponent.Child.PodcastDetail)
            assertEquals("p2", (stack[1].instance as RootComponent.Child.PodcastDetail).podcastId)
        }
    }
}
