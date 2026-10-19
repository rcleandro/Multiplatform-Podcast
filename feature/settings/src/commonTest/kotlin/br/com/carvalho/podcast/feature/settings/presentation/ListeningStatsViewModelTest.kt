package br.com.carvalho.podcast.feature.settings.presentation

import br.com.carvalho.podcast.domain.model.ListeningStats
import br.com.carvalho.podcast.domain.repository.FakePlayerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ListeningStatsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theStatsGrowAsSomethingPlays() = runTest(dispatcher) {
        val repository = FakePlayerRepository()
        val viewModel = ListeningStatsViewModel(repository)
        backgroundScope.launch { viewModel.stats.collect {} }
        assertEquals(ListeningStats(), viewModel.stats.value)

        repository.stats.value = ListeningStats(totalListenedMs = 60_000, totalSavedMs = 5_000)

        assertEquals(60_000, viewModel.stats.value.totalListenedMs)
        assertEquals(5_000, viewModel.stats.value.totalSavedMs)
    }
}
