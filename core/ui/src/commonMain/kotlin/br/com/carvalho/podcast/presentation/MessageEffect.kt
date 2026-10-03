package br.com.carvalho.podcast.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** A one-off message for the user: a text from `composeResources` and the values for its placeholders. */
data class UiMessage(val text: StringResource, val args: List<Any> = emptyList())

/** Shows each one-off message a view model emits as a snackbar, one after the other. */
@Suppress("SpreadOperator") // getString takes varargs; the copy happens once per snackbar
@Composable
fun MessageEffect(messages: Flow<UiMessage>, snackbarHostState: SnackbarHostState) {
    LaunchedEffect(messages) {
        messages.collect { snackbarHostState.showSnackbar(getString(it.text, *it.args.toTypedArray())) }
    }
}
