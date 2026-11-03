package br.com.carvalho.podcast.feature.discover.presentation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.flow.filter

/** Scrolling the results means reading them: the search field lets go of the focus, and the keyboard closes. */
@Composable
internal fun rememberKeyboardClosingListState(): LazyListState {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.filter { it }.collect { focusManager.clearFocus() }
    }
    return listState
}
