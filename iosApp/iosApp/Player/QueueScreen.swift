import Shared
import SwiftUI

/// The queue (25.7), over the player: what plays now and what comes next, moved by the system's handles, removed by a
/// swipe, played by a tap, and cleared after asking (18.6), as the Compose screen does.
struct QueueScreen: View {
    let model: ShellModel
    @State private var scope: ScreenScope
    @State private var queue: Watched<QueueUiState>

    init(model: ShellModel) {
        self.model = model
        let scope = ScreenScope()
        _scope = State(initialValue: scope)
        _queue = State(initialValue: Watched(scope.viewModels.queue().uiState))
    }

    var body: some View {
        let state = queue.value
        NavigationStack {
            List {
                if let current = state.current {
                    Section(titled: Res.string.shared.now_playing.localized()) { row(current, state) }
                }
                if !state.upNext.isEmpty {
                    Section(titled: Res.string.shared.up_next.localized()) {
                        ForEach(state.upNext, id: \.id) { episode in
                            Button { send(QueueIntentPlay(episode: episode)) } label: { row(episode, state) }
                                .buttonStyle(.plain)
                        }
                        .onMove { from, to in
                            var ids = state.upNext.map(\.id)
                            ids.move(fromOffsets: from, toOffset: to)
                            send(QueueIntentReorder(ids: ids))
                        }
                        .onDelete { offsets in
                            offsets.map { state.upNext[$0] }.forEach { send(QueueIntentRemove(episode: $0)) }
                        }
                    }
                } else {
                    // A row under "Now playing", not over the list: large text would draw it on top of the episode.
                    ContentUnavailableView(Res.string.shared.queue_empty_title.localized(), systemImage: "list.bullet",
                                           description: Text(Res.string.shared.queue_empty_message.localized()))
                        .listRowBackground(Color.clear)
                }
            }
            .navigationTitle(Res.string.shared.queue.localized())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(Res.string.shared.close.localized()) { model.root.onBackClicked() }
                }
                if !state.upNext.isEmpty {
                    ToolbarItem(placement: .topBarLeading) { EditButton() }
                    ToolbarItem(placement: .bottomBar) {
                        Button(Res.string.shared.clear_queue.localized(), role: .destructive) { send(QueueIntentRequestClear.shared) }
                    }
                }
            }
            .confirmationDialog(Res.string.shared.clear_queue.localized(), isPresented: Binding(
                get: { state.isConfirmingClear }, set: { if !$0 { send(QueueIntentDismissClear.shared) } }
            ), titleVisibility: .visible) {
                Button(Res.string.shared.clear.localized(), role: .destructive) { send(QueueIntentConfirmClear.shared) }
                Button(Res.string.shared.cancel.localized(), role: .cancel) { send(QueueIntentDismissClear.shared) }
            } message: {
                Text(Res.string.shared.clear_queue_message.localized())
            }
        }
    }

    private func row(_ episode: Episode, _ state: QueueUiState) -> some View {
        let isCurrent = state.current?.id == episode.id
        return EpisodeRow(
            episode: episode,
            podcastTitle: episode.podcastTitle,
            isPlaying: isCurrent && state.isPlaying,
            isLoading: isCurrent && state.isBuffering,
            download: EpisodeDownload((state.downloads as? [String: DownloadStatus])?[episode.id], isDownloaded: episode.isDownloaded),
            actions: EpisodeActions(
                play: { send(QueueIntentPlay(episode: episode)) },
                download: { send(QueueIntentDownload(episode: episode)) },
                cancelDownload: { send(QueueIntentCancelDownload(episode: episode)) },
                deleteDownload: { send(QueueIntentDeleteDownload(episode: episode)) }
            )
        )
    }

    private func send(_ intent: any QueueIntent) {
        scope.viewModels.queue().onIntent(intent: intent)
    }
}
