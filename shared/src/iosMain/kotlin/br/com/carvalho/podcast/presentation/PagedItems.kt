package br.com.carvalho.podcast.presentation

import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * A paged list for SwiftUI (ADR 0015, decision 1), what `LazyPagingItems` is to Compose: [items] holds what has loaded,
 * and the list calls [loadAround] as each row appears, so the next page comes as the end gets close.
 */
class PagedItems<T : Any> internal constructor(
    pages: Flow<PagingData<T>>,
    scope: CoroutineScope,
    mainContext: CoroutineContext = Dispatchers.Main,
) {
    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items

    private val presenter = object : PagingDataPresenter<T>(mainContext) {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) {
            _items.value = snapshot().items
        }
    }

    init {
        scope.launch { pages.collectLatest(presenter::collectFrom) }
    }

    /** The row at [index] of [items] is on screen. */
    fun loadAround(index: Int) {
        val snapshot = presenter.snapshot()
        val position = snapshot.placeholdersBefore + index
        if (position in 0 until presenter.size) presenter[position]
    }
}
