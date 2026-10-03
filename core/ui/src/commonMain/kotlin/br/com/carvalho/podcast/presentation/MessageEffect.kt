package br.com.carvalho.podcast.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** Shows each one-off message a view model emits as a snackbar, one after the other. */
@Composable
fun MessageEffect(messages: Flow<StringResource>, snackbarHostState: SnackbarHostState) {
    LaunchedEffect(messages) {
        messages.collect { snackbarHostState.showSnackbar(getString(it)) }
    }
}
