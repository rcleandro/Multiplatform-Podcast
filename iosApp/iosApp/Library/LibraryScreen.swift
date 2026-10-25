import Shared
import SwiftUI

/// The library (25.4): the subscribed podcasts in a grid or a list, "Continue listening" above them, and a category
/// filter; sorting, organizing, refreshing and deleting, as the Compose screen does (feature/library).
struct LibraryScreen: View {
    let model: ShellModel
    private let viewModel: LibraryViewModel
    @State private var library: Watched<LibraryUiState>

    init(model: ShellModel) {
        self.model = model
        viewModel = model.app.viewModels.library()
        _library = State(initialValue: Watched(viewModel.uiState))
    }

    var body: some View {
        let state = library.value
        content(state)
            .navigationTitle(Res.string.shared.library_title.localized())
            .toolbar { toolbar(state) }
            .refreshable { await refresh() }
            .messages(from: viewModel.messages)
            .confirmationDialog(
                Res.string.shared.delete_podcast.localized(),
                isPresented: Binding(get: { state.podcastToDelete != nil }, set: { if !$0 { send(LibraryIntentDismissDelete.shared) } }),
                titleVisibility: .visible,
                presenting: state.podcastToDelete
            ) { _ in
                Button(Res.string.shared.delete_.localized(), role: .destructive) { send(LibraryIntentConfirmDelete.shared) }
                Button(Res.string.shared.cancel.localized(), role: .cancel) { send(LibraryIntentDismissDelete.shared) }
            } message: { podcast in
                Text(Res.string.shared.delete_podcast_confirmation.localized([podcast.title]))
            }
    }

    @ViewBuilder
    private func content(_ state: LibraryUiState) -> some View {
        if state.isLoading {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if state.podcasts.isEmpty {
            ContentUnavailableView {
                Label(Res.string.shared.no_podcasts_found.localized(), systemImage: "mic")
            } description: {
                Text(Res.string.shared.library_empty_message.localized())
            } actions: {
                Button(Res.string.shared.add_podcast.localized()) { model.root.onAddPodcast() }
                    .buttonStyle(.borderedProminent)
            }
        } else if state.layout == .list {
            List {
                header(state).listRowSeparator(.hidden)
                ForEach(state.podcasts, id: \.podcast.id) { entry in
                    Button { open(entry.podcast) } label: { PodcastRow(entry: entry) }
                        .contextMenu { deleteButton(entry.podcast) }
                }
            }
            .listStyle(.plain)
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.l) {
                    header(state)
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: Sizes.artworkM), spacing: Spacing.m)], spacing: Spacing.l) {
                        ForEach(state.podcasts, id: \.podcast.id) { entry in
                            Button { open(entry.podcast) } label: { PodcastCard(entry: entry) }
                                .buttonStyle(.plain)
                                .contextMenu { deleteButton(entry.podcast) }
                        }
                    }
                }
                .padding(Spacing.l)
            }
        }
    }

    /// "Continue listening" and the category chips, above the podcasts.
    @ViewBuilder
    private func header(_ state: LibraryUiState) -> some View {
        VStack(alignment: .leading, spacing: Spacing.s) {
            if !state.inProgress.isEmpty {
                SectionTitle(Res.string.shared.continue_listening.localized())
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Spacing.s) {
                        ForEach(state.inProgress, id: \.id) { episode in
                            ContinueCard(episode: episode) { send(LibraryIntentPlay(episode: episode)) }
                        }
                    }
                }
                SectionTitle(Res.string.shared.library_podcasts.localized()).padding(.top, Spacing.m)
            }
            // Chips only when they can tell podcasts apart: two categories or more.
            if state.categories.count > 1 {
                CategoryChips(categories: state.categories, selected: state.category) {
                    send(LibraryIntentSelectCategory(category: $0))
                }
            }
        }
    }

    @ToolbarContentBuilder
    private func toolbar(_ state: LibraryUiState) -> some ToolbarContent {
        ToolbarItemGroup(placement: .topBarTrailing) {
            // Sorting, the layout and refreshing act on podcasts: none while the library is empty (18.3).
            if !state.podcasts.isEmpty {
                Menu {
                    Picker(Res.string.shared.library_sort.localized(), selection: Binding(
                        get: { state.sort },
                        set: { send(LibraryIntentChangeSort(sort: $0)) }
                    )) {
                        ForEach(LibrarySort.entries, id: \.self) { sort in Text(sort.label).tag(sort) }
                    }
                    Divider()
                    // Arranging opens its own screen, so the library never shows drag handles.
                    Button(Res.string.shared.library_organize.localized()) { model.root.onOrganizeLibrary() }
                } label: {
                    Label(Res.string.shared.library_sort.localized(), systemImage: "arrow.up.arrow.down")
                }
                // The button shows the layout it switches to.
                Button { send(LibraryIntentToggleLayout.shared) } label: {
                    state.layout == .grid
                        ? Label(Res.string.shared.library_show_list.localized(), systemImage: "list.bullet")
                        : Label(Res.string.shared.library_show_grid.localized(), systemImage: "square.grid.2x2")
                }
            }
            Button { model.root.onAddPodcast() } label: {
                Label(Res.string.shared.add_podcast.localized(), systemImage: "plus")
            }
            Button { model.root.onOpenSettings() } label: {
                Label(Res.string.shared.settings.localized(), systemImage: "gearshape")
            }
        }
    }

    private func deleteButton(_ podcast: Podcast) -> some View {
        Button(Res.string.shared.delete_podcast.localized(), systemImage: "trash", role: .destructive) {
            send(LibraryIntentRequestDelete(podcast: podcast))
        }
    }

    private func open(_ podcast: Podcast) {
        model.root.onPodcastSelected(podcastId: podcast.id)
    }

    private func send(_ intent: any LibraryIntent) {
        viewModel.onIntent(intent: intent)
    }

    /// Pulling down refreshes every podcast; the spinner stays until the view model says it is done.
    private func refresh() async {
        send(LibraryIntentRefreshAll.shared)
        while library.value.isRefreshing || library.value.isLoading {
            try? await Task.sleep(for: .milliseconds(200))
        }
    }
}

private extension LibrarySort {
    var label: String {
        switch self {
        case .recentlyAdded: Res.string.shared.library_sort_recently_added.localized()
        case .firstAdded: Res.string.shared.library_sort_first_added.localized()
        case .latestEpisode: Res.string.shared.library_sort_latest_episode.localized()
        case .mostUnplayed: Res.string.shared.library_sort_most_unplayed.localized()
        case .custom: Res.string.shared.library_sort_custom.localized()
        default: Res.string.shared.library_sort_title.localized()
        }
    }
}
