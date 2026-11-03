package br.com.carvalho.podcast.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SwiftBridgeTest {
    // A SwiftUI screen follows its view model's state while it watches, and nothing reaches it after it cancels.
    @Test
    fun aWatchedFlowDeliversEachValueUntilCancelled() = runTest(UnconfinedTestDispatcher()) {
        val state = MutableStateFlow(1)
        val seen = mutableListOf<Int>()

        val handle = state.watch(backgroundScope) { seen += it }
        state.value = 2
        handle.cancel()
        state.value = 3

        assertEquals(listOf(1, 2), seen)
    }
}
