package br.com.carvalho.podcast.feature.discover.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalFocusManager
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
import br.com.carvalho.podcast.core.designsystem.component.SectionTitle
import br.com.carvalho.podcast.core.designsystem.readableWidth
import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.add
import br.com.carvalho.podcast.core.ui.generated.resources.add_by_url
import br.com.carvalho.podcast.core.ui.generated.resources.add_podcast
import br.com.carvalho.podcast.core.ui.generated.resources.back
import br.com.carvalho.podcast.core.ui.generated.resources.cancel
import br.com.carvalho.podcast.core.ui.generated.resources.discover_search_placeholder
import br.com.carvalho.podcast.core.ui.generated.resources.error_search_directory
import br.com.carvalho.podcast.core.ui.generated.resources.more_options
import br.com.carvalho.podcast.core.ui.generated.resources.no_podcasts_found
import br.com.carvalho.podcast.core.ui.generated.resources.opened_link
import br.com.carvalho.podcast.core.ui.generated.resources.paste
import br.com.carvalho.podcast.core.ui.generated.resources.podcast_episode_count
import br.com.carvalho.podcast.core.ui.generated.resources.popular
import br.com.carvalho.podcast.core.ui.generated.resources.rss_url_label
import br.com.carvalho.podcast.core.ui.generated.resources.rss_url_placeholder
import br.com.carvalho.podcast.core.ui.generated.resources.settings_import_opml
import br.com.carvalho.podcast.core.ui.generated.resources.subscribe
import br.com.carvalho.podcast.core.ui.generated.resources.subscribe_to
import br.com.carvalho.podcast.core.ui.generated.resources.subscribed
import br.com.carvalho.podcast.core.ui.generated.resources.suggestions_message
import br.com.carvalho.podcast.core.ui.generated.resources.suggestions_title
import br.com.carvalho.podcast.core.ui.generated.resources.try_again
import br.com.carvalho.podcast.domain.model.PodcastCategory
import br.com.carvalho.podcast.domain.usecase.DirectoryResult
import br.com.carvalho.podcast.presentation.MessageEffect
import br.com.carvalho.podcast.presentation.component.SearchField
import br.com.carvalho.podcast.presentation.file.rememberTextFilePicker
import br.com.carvalho.podcast.presentation.format.explicitLabel
import br.com.carvalho.podcast.presentation.format.label
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Add podcast". Each [session] (one per opening) starts with nothing typed and no link; the same session, after a
 * rotation or a fold, keeps it. A [link] opened in the app (18.4) is shown here, then [onLinkHandled]; one to a
 * podcast the library has goes to [onOpenPodcast] instead.
 */
@Composable
fun DiscoverScreen(
    onBack: () -> Unit,
    session: Int = 0,
    link: String? = null,
    onLinkHandled: () -> Unit = {},
    onOpenPodcast: (String) -> Unit = {},
    /** A podcast of the library whose similar ones to show (19.6); handed back once taken, like [link]. */
    similarTo: String? = null,
    onSimilarHandled: () -> Unit = {},
    viewModel: DiscoverViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    MessageEffect(viewModel.messages, snackbarHostState)
    // Added by its address: back to the library, which shows it.
    LaunchedEffect(viewModel) { viewModel.addedByUrl.collectLatest { onBack() } }
    LaunchedEffect(viewModel) { viewModel.openPodcast.collectLatest(onOpenPodcast) }
    // Before the link: a new opening clears the screen first, then shows the link it came with.
    LaunchedEffect(session) { viewModel.onIntent(DiscoverIntent.Open(session)) }
    LaunchedEffect(link) {
        if (link != null) {
            viewModel.onIntent(DiscoverIntent.OpenLink(link))
            onLinkHandled()
        }
    }
    LaunchedEffect(similarTo) {
        if (similarTo != null) {
            viewModel.onIntent(DiscoverIntent.ShowSimilar(similarTo))
            onSimilarHandled()
        }
    }

    // Opened for a link, the search never takes focus: asking for it before the link reaches the view model raced
    // the clearing below, and the keyboard stayed open over the linked podcast.
    val openedWithLink = remember { link != null }
    DiscoverContent(
        state = state,
        snackbarHostState = snackbarHostState,
        focusSearch = !openedWithLink,
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
            onImportOpml = rememberTextFilePicker { viewModel.onIntent(DiscoverIntent.ImportOpml(it)) },
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
    /** Opens the file picker for an OPML file (18.13). */
    val onImportOpml: () -> Unit = {},
)

@Composable
fun DiscoverContent(
    state: DiscoverUiState,
    actions: DiscoverActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    focusSearch: Boolean = true,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                DiscoverTopBar(actions)
                state.importProgress?.let { (done, total) ->
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else done.toFloat() / total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        snackbarHost = { PodcastSnackbarHost(snackbarHostState, clearMiniPlayer = false) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        // Ready to type as soon as the screen opens. Requested in here: the Scaffold builds its content after the
        // composition around it, so a request from outside would find no field yet.
        val searchFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { if (focusSearch && !state.isOpeningLink) searchFocus.requestFocus() }
        // A link's podcast is what to see, not the keyboard over it (18.4).
        val focusManager = LocalFocusManager.current
        LaunchedEffect(state.isOpeningLink) { if (state.isOpeningLink) focusManager.clearFocus() }
        Column(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            SearchField(
                query = state.query,
                onQueryChange = actions.onQueryChange,
                placeholder = stringResource(Res.string.discover_search_placeholder),
                modifier = Modifier.readableWidth().focusRequester(searchFocus),
            )
            if (state.query.isBlank()) CategoryChips(state.category, actions.onCategoryChange)
            if (state.isAddingUrl || state.isOpeningLink) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            // Above the keyboard, which opens with the screen, the results stay in sight. Only they make room for it:
            // in a low window (a phone on its side) the keyboard takes nearly all the height, and padding the whole
            // column squeezed the search field until its text no longer showed. The navigation bar inset is already
            // in [padding], so it is not counted twice.
            Box(modifier = Modifier.weight(1f).imePadding()) { DiscoverBody(state, actions) }
        }
    }

    if (state.isUrlDialogOpen) {
        AddByUrlDialog(state.url, actions.onUrlChange, actions.onDismissUrlDialog, actions.onConfirmUrl)
    }
}

@Composable
private fun DiscoverBody(state: DiscoverUiState, actions: DiscoverActions) {
    when {
        state.isSearching && state.results.isEmpty() -> LoadingState()
        state.searchFailed -> ErrorState(
            icon = Icons.Rounded.CloudOff,
            title = stringResource(Res.string.error_search_directory),
            message = null,
            actionLabel = stringResource(Res.string.try_again),
            onAction = actions.onRetry,
        )
        state.results.isEmpty() -> EmptyState(
            icon = Icons.Rounded.Search,
            title = stringResource(Res.string.no_podcasts_found),
            message = null,
            actionLabel = stringResource(Res.string.add_by_url),
            onAction = actions.onOpenUrlDialog,
        )
        else -> LazyColumn(
            state = rememberKeyboardClosingListState(),
            contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.fillMaxSize(),
        ) {
            // One header, always the first item: a linked podcast that arrives after the chart would otherwise open
            // above the top, since the list keeps its first visible item in place.
            if (state.query.isBlank()) {
                item(key = HEADER_KEY) {
                    Column {
                        state.linked?.let { linked ->
                            ListTitle(stringResource(Res.string.opened_link))
                            ResultRow(linked, linked.podcast.feedUrl in state.subscribing, actions.onSubscribe)
                        }
                        Recommendations(state, actions)
                        // A chart says which one: the overall (18.3) or a category's (18.2).
                        ListTitle(stringResource(state.category?.label ?: Res.string.popular))
                    }
                }
            }
            items(state.results, key = { it.podcast.feedUrl }) { result ->
                ResultRow(result, isSubscribing = result.podcast.feedUrl in state.subscribing, actions.onSubscribe)
            }
        }
    }
}

@Composable
internal fun ListTitle(text: String) = SectionTitle(text, Modifier.readableWidth().padding(vertical = Spacing.s))

private const val HEADER_KEY = "header"

/**
 * What an empty library shows instead of a blank screen (18.3): pick a category and subscribe to its most popular
 * podcasts, or look for one. The library goes back to its podcasts as soon as it has one.
 */
// ponytail: a failed subscription is not announced here (the library owns the snackbar); pass its host if it matters.
@Composable
fun PodcastSuggestions(
    onAddPodcast: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = koinViewModel(key = SUGGESTIONS_KEY),
) {
    val state by viewModel.uiState.collectAsState()
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
            modifier = Modifier.readableWidth().padding(horizontal = Spacing.l, vertical = Spacing.s),
        ) {
            Text(stringResource(Res.string.suggestions_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(Res.string.suggestions_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onAddPodcast) { Text(stringResource(Res.string.add_podcast)) }
        }
        CategoryChips(state.category) { viewModel.onIntent(DiscoverIntent.SelectCategory(it)) }
        DiscoverBody(
            state,
            DiscoverActions(
                onRetry = { viewModel.onIntent(DiscoverIntent.Retry) },
                onSubscribe = { viewModel.onIntent(DiscoverIntent.Subscribe(it)) },
                onOpenUrlDialog = onAddPodcast,
            ),
        )
    }
}

private const val SUGGESTIONS_KEY = "library-suggestions"

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
        modifier = Modifier.readableWidth().padding(bottom = Spacing.s),
        contentPadding = PaddingValues(horizontal = Spacing.l),
    )
}

@Composable
internal fun ResultRow(result: DirectoryResult, isSubscribing: Boolean, onSubscribe: (DirectoryResult) -> Unit) {
    val podcast = result.podcast
    val episodes = podcast.episodeCount?.let { pluralStringResource(Res.plurals.podcast_episode_count, it, it) }
    PodcastListItem(
        title = podcast.title,
        author = podcast.author,
        imageUrl = podcast.artworkUrl,
        supportingText = listOfNotNull(explicitLabel(podcast.explicit), podcast.genre, episodes)
            .joinToString(" · ")
            .ifEmpty { null },
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
    val focusManager = LocalFocusManager.current
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
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.settings_import_opml)) },
                    onClick = {
                        menuOpen = false
                        // Back from the picker the search field would take the keyboard again, over the result.
                        focusManager.clearFocus()
                        actions.onImportOpml()
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
