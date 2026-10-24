package br.com.carvalho.podcast.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import kotlinx.coroutines.flow.StateFlow

/**
 * Only the part of [this] that [pick] takes, recomposing when that part changes and not on every emission (23.8): the
 * player's state moves twice a second while playing, and a screen that shows the current episode needs none of it.
 */
@Composable
fun <T, R> StateFlow<T>.selectAsState(pick: (T) -> R): State<R> {
    val whole = collectAsState()
    return remember(whole) { derivedStateOf(structuralEqualityPolicy()) { pick(whole.value) } }
}
