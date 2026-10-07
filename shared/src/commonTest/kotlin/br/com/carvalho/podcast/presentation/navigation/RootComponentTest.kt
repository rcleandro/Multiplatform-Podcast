package br.com.carvalho.podcast.presentation.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RootComponentTest {
    private val backDispatcher = BackDispatcher()

    private fun createRoot(stateKeeper: StateKeeperDispatcher = StateKeeperDispatcher()) = RootComponent(
        DefaultComponentContext(
            lifecycle = LifecycleRegistry().apply { resume() },
            stateKeeper = stateKeeper,
            backHandler = backDispatcher,
        )
    )

    private val RootComponent.current get() = state.value

    @Test
    fun startsOnTheLibraryWithNothingToGoBackTo() {
        val root = createRoot()

        assertEquals(Tab.Library, root.current.selectedTab)
        assertFalse(backDispatcher.isEnabled)
    }

    @Test
    fun organizingTheLibraryOpensItsScreenAndBackClosesIt() {
        val root = createRoot()

        root.onOrganizeLibrary()

        assertTrue(root.current.isOrganizingLibrary)
        assertTrue(backDispatcher.isEnabled)
        backDispatcher.back()
        assertFalse(root.current.isOrganizingLibrary)
        assertEquals(Tab.Library, root.current.selectedTab)
    }

    @Test
    fun selectingAnEpisodeOpensItOverItsPodcast() {
        val root = createRoot()

        root.onEpisodeSelected(episodeId = "e1", podcastId = "p1")

        assertEquals(Detail.Podcast("p1"), root.current.podcast)
        assertEquals(Detail.Episode("e1"), root.current.episode)
    }

    @Test
    fun eachTabKeepsItsOwnStack() {
        val root = createRoot()
        root.onPodcastSelected("p1")

        root.onTabClicked(Tab.Search)
        assertNull(root.current.podcast)
        root.onEpisodeSelected("e2", "p2")

        root.onTabClicked(Tab.Library)
        assertEquals(Detail.Podcast("p1"), root.current.podcast)
        root.onTabClicked(Tab.Search)
        assertEquals(Detail.Episode("e2"), root.current.episode)
    }

    @Test
    fun selectingTheShownTabAgainReturnsToItsRoot() {
        val root = createRoot()
        root.onPodcastSelected("p1")

        root.onTabClicked(Tab.Library)

        assertTrue(root.current.currentStack.isEmpty())
    }

    @Test
    fun backClosesThePlayerThenPopsTheTabThenReturnsToTheLibrary() {
        val root = createRoot()
        root.onTabClicked(Tab.Downloads)
        root.onEpisodeSelected("e1", "p1")
        root.onPlayerClicked()

        assertTrue(backDispatcher.back())
        assertFalse(root.current.isPlayerOpen)
        assertTrue(backDispatcher.back())
        assertEquals(Detail.Podcast("p1"), root.current.podcast)
        assertNull(root.current.episode)
        assertTrue(backDispatcher.back())
        assertTrue(root.current.currentStack.isEmpty())
        assertTrue(backDispatcher.back())
        assertEquals(Tab.Library, root.current.selectedTab)
        assertFalse(backDispatcher.isEnabled)
    }

    @Test
    fun theStateSurvivesRecreation() {
        val savedState = StateKeeperDispatcher().let { keeper ->
            createRoot(keeper).apply {
                onTabClicked(Tab.Search)
                onEpisodeSelected("e1", "p1")
            }
            keeper.save()
        }

        val restored = createRoot(StateKeeperDispatcher(savedState))

        assertEquals(Tab.Search, restored.current.selectedTab)
        assertEquals(Detail.Episode("e1"), restored.current.episode)
    }
}
