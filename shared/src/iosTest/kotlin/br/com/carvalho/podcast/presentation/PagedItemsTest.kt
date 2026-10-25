package br.com.carvalho.podcast.presentation

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PagedItemsTest {
    private val dispatcher = UnconfinedTestDispatcher()

    // A long podcast loads a page at a time: the SwiftUI list asks for more as its last rows appear.
    @Test
    fun theNextPageLoadsAsTheEndOfTheListAppears() = runTest(dispatcher) {
        val pager = Pager(PagingConfig(pageSize = PAGE, initialLoadSize = PAGE, prefetchDistance = 2, enablePlaceholders = false)) {
            Numbers()
        }
        val paged = PagedItems(pager.flow, backgroundScope, dispatcher)
        advanceUntilIdle()
        assertEquals((0 until PAGE).toList(), paged.items.value)

        paged.loadAround(1)
        advanceUntilIdle()
        assertEquals(PAGE, paged.items.value.size)

        paged.loadAround(PAGE - 1)
        advanceUntilIdle()
        assertEquals((0 until 2 * PAGE).toList(), paged.items.value)
    }

    private class Numbers : PagingSource<Int, Int>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
            val start = params.key ?: 0
            val end = minOf(start + params.loadSize, TOTAL)
            return LoadResult.Page((start until end).toList(), prevKey = null, nextKey = end.takeIf { it < TOTAL })
        }

        override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null
    }

    private companion object {
        const val PAGE = 10
        const val TOTAL = 35
    }
}
