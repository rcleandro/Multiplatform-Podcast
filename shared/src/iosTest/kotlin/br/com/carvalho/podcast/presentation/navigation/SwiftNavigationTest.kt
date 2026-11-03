package br.com.carvalho.podcast.presentation.navigation

import br.com.carvalho.podcast.core.util.NetworkMonitor
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SwiftNavigationTest {
    @BeforeTest
    fun setUp() {
        startKoin { modules(module { single<NetworkMonitor> { Online } }) }
    }

    @AfterTest
    fun tearDown() = stopKoin()

    // The system ends the process in the background; the scene keeps what save() wrote, and the app comes back there.
    @Test
    fun theNavigationComesBackAsItWasSaved() {
        val navigation = SwiftNavigation(saved = null)
        navigation.root.onTabClicked(Tab.Episodes)
        navigation.root.onEpisodeSelected(episodeId = "e1", podcastId = "p1")
        navigation.root.onPlayerClicked()

        val restored = SwiftNavigation(saved = navigation.save())

        assertEquals(navigation.root.state.value, restored.root.state.value)
    }

    @Test
    fun aStateThatNoLongerReadsStartsOnTheLibrary() {
        assertEquals(NavigationState(), SwiftNavigation(saved = "{not a state").root.state.value)
    }

    private object Online : NetworkMonitor {
        override suspend fun isOnline() = true
    }
}
