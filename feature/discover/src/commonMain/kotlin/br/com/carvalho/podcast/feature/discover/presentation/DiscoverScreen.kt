package br.com.carvalho.podcast.feature.discover.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import br.com.carvalho.podcast.core.designsystem.Sizes
import br.com.carvalho.podcast.core.designsystem.Spacing
import br.com.carvalho.podcast.core.designsystem.component.EmptyState
import br.com.carvalho.podcast.core.designsystem.component.ErrorState
import br.com.carvalho.podcast.core.designsystem.component.FilterChipRow
import br.com.carvalho.podcast.core.designsystem.component.FilterOption
import br.com.carvalho.podcast.core.designsystem.component.LoadingState
import br.com.carvalho.podcast.core.designsystem.component.PodcastListItem
import br.com.carvalho.podcast.core.designsystem.component.PodcastSnackbarHost
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.add
import br.com.carvalho.podcast.core.ui.generated.resources.add_by_url
import br.com.carvalho.podcast.core.ui.generated.resources.add_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.discover_hint_message
import br.com.carvalho.podcast.core.ui.generated.resources.discover_hint_title
import br.com.carvalho.podcast.core.ui.generated.resources.discover_search_placeholder
import br.com.carvalho.podcast.core.ui.generated.resources.error_search_directory
import br.com.carvalho.podcast.core.ui.generated.resources.more_options
import br.com.carvalho.podcast.core.ui.generated.resources.no_podcasts_found
import br.com.carvalho.podcast.core.ui.generated.resources.paste
import br.com.carvalho.podcast.core.ui.generated.resources.podcast_episode_count
import br.com.carvalho.podcast.core.ui.generated.resources.rss_url_label
import br.com.carvalho.podcast.core.ui.generated.resources.rss_url_placeholder
import br.com.carvalho.podcast.core.ui.generated.resources.subscribe
import br.com.carvalho.podcast.core.ui.generated.resources.subscribe_to
import br.com.carvalho.podcast.core.ui.generated.resources.subscribed
import br.com.carvalho.podcast.core.ui.generated.resources.try_again
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.usecase.DirectoryResult
import br.com.carvalho.podcast.presentation.MessageEffect
import br.com.carvalho.podcast.presentation.component.SearchField
import br.com.carvalho.podcast.presentation.format.label
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DiscoverScreen(onBack: () -> Unit, viewModel: DiscoverViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    MessageEffect(viewModel.messages, snackbarHostState)
    // Added by its address: back to the library, which shows it.
    LaunchedEffect(viewModel) { viewModel.addedByUrl.collectLatest { onBack() } }

    DiscoverContent(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = DiscoverActions(
            onBack = onBack,
            onQueryChange = { viewModel.onIntent(DiscoverIntent.ChangeQuery(it)) },
            onRetry = { viewModel.onIntent(DiscoverIntent.Retry) },
            onCategoryChange = { viewModel.onIntent(DiscoverIntent.SelectCategory(it)) },
            onSubscribe = { viewModel.onIntent(DiscoverIntent.Subscribe(it)) },
            onOpenUrlDialog = { viewModel.onIntent(DiscoverIntent.OpenUrlDialog) },
            onUrlChange = { viewModel.onIntent(DiscoverIntent.ChangeUrl(it)) },
            onConfirmUrl = { viewModel.onIntent(DiscoverIntent.ConfirmUrl) },
            onDismissUrlDialog = { viewModel.onIntent(DiscoverIntent.DismissUrlDialog) },
        ),
    )
}

data class DiscoverActions(
    val onBack: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onCategoryChange: (PodcastCategory?) -> Unit = {},
    val onSubscribe: (DirectoryResult) -> Unit = {},
    val onOpenUrlDialog: () -> Unit = {},
    val onUrlChange: (String) -> Unit = {},
    val onConfirmUrl: () -> Unit = {},
    val onDismissUrlDialog: () -> Unit = {},
)

@Composable
fun DiscoverContent(
    state: DiscoverUiState,
    actions: DiscoverActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = { DiscoverTopBar(actions) },
        snackbarHost = { PodcastSnackbarHost(snackbarHostState, clearMiniPlayer = false) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        // Ready to type as soon as the screen opens. Requested in here: the Scaffold builds its content after the
        // composition around it, so a request from outside would find no field yet.
        val searchFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { searchFocus.requestFocus() }
        // Above the keyboard, which opens with the screen: the hint and its button, and the last results, stay in
        // sight. The navigation bar inset is already in [padding], so it is not counted twice.
        Column(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            SearchField(
                query = state.query,
                onQueryChange = actions.onQueryChange,
                placeholder = stringResource(Res.string.discover_search_placeholder),
                modifier = Modifier.readableWidth().focusRequester(searchFocus),
            )
            if (state.query.isBlank()) CategoryChips(state.category, actions.onCategoryChange)
            if (state.isAddingUrl) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            DiscoverBody(state, actions)
        }
    }

    if (state.isUrlDialogOpen) {
        AddByUrlDialog(state.url, actions.onUrlChange, actions.onDismissUrlDialog, actions.onConfirmUrl)
    }
}

@Composable
private fun DiscoverBody(state: DiscoverUiState, actions: DiscoverActions) {
    when {
        state.query.isBlank() && state.category == null -> EmptyState(
            icon = Icons.Rounded.Search,
            title = stringResource(Res.string.discover_hint_title),
            message = stringResource(Res.string.discover_hint_message),
            actionLabel = stringResource(Res.string.add_by_url),
            onAction = actions.onOpenUrlDialog,
        )
        state.isSearching && state.results.isEmpty() -> LoadingState()
        state.searchFailed -> ErrorState(
            icon = Icons.Rounded.CloudOff,
            title = stringResource(Res.string.error_search_directory),
            message = null,
            actionLabel = stringResource(Res.string.try_again),
            onAction = actions.onRetry,
        )
        state.results.isEmpty() && !state.isSearching -> EmptyState(
            icon = Icons.Rounded.Search,
            title = stringResource(Res.string.no_podcasts_found),
            message = null,
            actionLabel = stringResource(Res.string.add_by_url),
            onAction = actions.onOpenUrlDialog,
        )
        else -> LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.results, key = { it.podcast.feedUrl }) { result ->
                ResultRow(result, isSubscribing = result.podcast.feedUrl in state.subscribing, actions.onSubscribe)
            }
        }
    }
}

/** The categories under the empty field: a tap shows the most popular podcasts of one, another tap closes it (18.2). */
@Composable
private fun CategoryChips(selected: PodcastCategory?, onCategoryChange: (PodcastCategory?) -> Unit) {
    // In the user's alphabetical order: Apple's is the English one.
    val labeled = PodcastCategory.entries.map { it to stringResource(it.label) }.sortedBy { it.second.lowercase() }
    val categories = labeled.map { it.first }
    FilterChipRow(
        options = labeled.map { FilterOption(it.second) },
        selectedIndex = categories.indexOf(selected),
        onSelected = { index -> onCategoryChange(categories[index].takeIf { it != selected }) },
        modifier = Modifier.readableWidth().padding(start = Spacing.l, end = Spacing.l, bottom = Spacing.s),
    )
}

@Composable
private fun ResultRow(result: DirectoryResult, isSubscribing: Boolean, onSubscribe: (DirectoryResult) -> Unit) {
    val podcast = result.podcast
    val episodes = podcast.episodeCount?.let { pluralStringResource(Res.plurals.podcast_episode_count, it, it) }
    PodcastListItem(
        title = podcast.title,
        author = podcast.author,
        imageUrl = podcast.artworkUrl,
        supportingText = listOfNotNull(podcast.genre, episodes).joinToString(" · ").ifEmpty { null },
        onClick = { onSubscribe(result) },
        modifier = Modifier.readableWidth(),
        trailing = { SubscribeButton(result, isSubscribing, onClick = { onSubscribe(result) }) },
    )
}

@Composable
private fun SubscribeButton(result: DirectoryResult, isSubscribing: Boolean, onClick: () -> Unit) {
    val label = stringResource(Res.string.subscribe_to, result.podcast.title)
    when {
        // Just the check: a "Subscribed" button would squeeze the title and author on a phone.
        result.isSubscribed -> Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = stringResource(Res.string.subscribed),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Sizes.touchTarget).padding(Spacing.m),
        )
        isSubscribing -> CircularProgressIndicator(
            strokeWidth = Sizes.progressStroke,
            modifier = Modifier.padding(horizontal = Spacing.l).size(Sizes.iconM),
        )
        else -> FilledTonalButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = label }) {
            Text(stringResource(Res.string.subscribe))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverTopBar(actions: DiscoverActions) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(Res.string.add_podcast), modifier = Modifier.semantics { heading() }) },
        navigationIcon = {
            IconButton(onClick = actions.onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back))
            }
        },
        actions = {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(Res.string.more_options))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.add_by_url)) },
                    onClick = {
                        menuOpen = false
                        actions.onOpenUrlDialog()
                    },
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun AddByUrlDialog(url: String, onUrlChange: (String) -> Unit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    // Ready to type (or paste) as soon as it opens; the keyboard's done key, Enter on Desktop, sends it.
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    // ponytail: the deprecated ClipboardManager reads plain text on every platform; its replacement (LocalClipboard)
    // returns a platform ClipEntry that needs an expect/actual per platform. Switch when text gets a common accessor.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_by_url)) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text(stringResource(Res.string.rss_url_label)) },
                placeholder = { Text(stringResource(Res.string.rss_url_placeholder)) },
                trailingIcon = {
                    IconButton(onClick = { clipboard.getText()?.text?.let(onUrlChange) }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = stringResource(Res.string.paste))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (url.isNotBlank()) onConfirm() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = url.isNotBlank()) { Text(stringResource(Res.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
