import Shared
import SwiftUI
import UniformTypeIdentifiers

/// "Add podcast" (25.8a): search the directory, browse a category's chart, "For you" and podcasts like one of the
/// library's, add by a feed address, import an OPML file, and the podcast of a link the app was opened with, as the
/// Compose screen does (feature/discover).
struct DiscoverScreen: View {
    let model: ShellModel
    @State private var scope: ScreenScope
    @State private var discover: Watched<DiscoverUiState>
    @State private var effects: [Watching] = []
    @State private var isImporting = false

    init(model: ShellModel) {
        self.model = model
        let scope = ScreenScope()
        _scope = State(initialValue: scope)
        _discover = State(initialValue: Watched(scope.viewModels.discover().uiState))
    }

    private var viewModel: DiscoverViewModel { scope.viewModels.discover() }

    var body: some View {
        let state = discover.value
        NavigationStack {
            List {
                if state.query.isEmpty {
                    DirectoryCategories(selected: state.category) { send(DiscoverIntentSelectCategory(category: $0)) }
                        .listRowSeparator(.hidden)
                }
                if let progress = importProgress(state) {
                    ProgressView(value: progress).listRowSeparator(.hidden)
                } else if state.isAddingUrl || state.isOpeningLink {
                    ProgressView().frame(maxWidth: .infinity).listRowSeparator(.hidden)
                }
                DirectoryResults(
                    state: state,
                    onSubscribe: { send(DiscoverIntentSubscribe(result: $0)) },
                    onRetry: { send(DiscoverIntentRetry.shared) },
                    onAddByUrl: { send(DiscoverIntentOpenUrlDialog.shared) }
                )
            }
            .listStyle(.plain)
            .navigationTitle(Res.string.shared.add_podcast.localized())
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: Binding(get: { state.query }, set: { send(DiscoverIntentChangeQuery(query: $0)) }),
                        placement: .navigationBarDrawer(displayMode: .always),
                        prompt: Res.string.shared.discover_search_placeholder.localized())
            .toolbar { toolbar }
            .alert(Res.string.shared.add_by_url.localized(), isPresented: Binding(
                get: { state.isUrlDialogOpen }, set: { if !$0 { send(DiscoverIntentDismissUrlDialog.shared) } }
            )) {
                TextField(Res.string.shared.rss_url_label.localized(),
                          text: Binding(get: { state.url }, set: { send(DiscoverIntentChangeUrl(url: $0)) }),
                          prompt: Text(Res.string.shared.rss_url_placeholder.localized()))
                    .keyboardType(.URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                Button(Res.string.shared.add.localized()) { send(DiscoverIntentConfirmUrl.shared) }
                Button(Res.string.shared.cancel.localized(), role: .cancel) { send(DiscoverIntentDismissUrlDialog.shared) }
            }
            .fileImporter(isPresented: $isImporting, allowedContentTypes: [.item]) { picked in
                send(DiscoverIntentImportOpml(text: try? OpmlFile.read(picked.get())))
            }
            .messages(from: viewModel.messages)
        }
        .onAppear(perform: start)
        .onDisappear { effects.forEach { $0.cancel() } }
        .onChange(of: model.state.podcastLink) { _, _ in takeLinkAndSimilar() }
        .onChange(of: model.state.similarTo) { _, _ in takeLinkAndSimilar() }
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button(Res.string.shared.close.localized()) { model.root.onBackClicked() }
        }
        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                Button(Res.string.shared.add_by_url.localized(), systemImage: "link") { send(DiscoverIntentOpenUrlDialog.shared) }
                Button(Res.string.shared.settings_import_opml.localized(), systemImage: "square.and.arrow.down") { isImporting = true }
            } label: {
                Label(Res.string.shared.more_options.localized(), systemImage: "ellipsis.circle")
            }
        }
    }

    /// A new opening starts clean; then the link or the podcast whose similar ones to show, if it came with one.
    private func start() {
        send(DiscoverIntentOpen(session: model.state.addPodcastSession))
        takeLinkAndSimilar()
        effects = [
            // Added by its address: back to the library, which shows it.
            SwiftBridgeKt.watch(viewModel.addedByUrl) { _ in model.root.onBackClicked() },
            SwiftBridgeKt.watch(viewModel.openPodcast) { id in
                if let id = id as? String { model.root.onPodcastSelected(podcastId: id) }
            },
        ]
    }

    private func takeLinkAndSimilar() {
        if let link = model.state.podcastLink {
            send(DiscoverIntentOpenLink(link: link))
            model.root.onPodcastLinkHandled()
        }
        if let podcastId = model.state.similarTo {
            send(DiscoverIntentShowSimilar(podcastId: podcastId))
            model.root.onSimilarHandled()
        }
    }

    private func importProgress(_ state: DiscoverUiState) -> Double? {
        guard let progress = state.importProgress, let done = progress.first, let total = progress.second else { return nil }
        return total.intValue == 0 ? 0 : done.doubleValue / total.doubleValue
    }

    private func send(_ intent: any DiscoverIntent) {
        viewModel.onIntent(intent: intent)
    }
}

/// The text of a file picked to import. Any file can be picked, since the system knows no type for `.opml`, and the
/// content says whether it fits; one too big to be an OPML counts as unreadable instead of filling the memory (22.3).
enum OpmlFile {
    static func read(_ url: URL) throws -> String? {
        let access = url.startAccessingSecurityScopedResource()
        defer { if access { url.stopAccessingSecurityScopedResource() } }
        let size = try url.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0
        guard Int64(size) <= AppConfig.shared.MAX_TEXT_BYTES else { return nil }
        return try String(contentsOf: url, encoding: .utf8)
    }
}
