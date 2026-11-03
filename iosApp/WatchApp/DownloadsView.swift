import SwiftUI
import WatchShared

/// What is on the watch (19.9f), to listen to away from the phone: a tap plays, a long press opens the actions.
struct DownloadsView: View {
    private let library = WatchBridgeKt.watchLibrary()
    @State private var rows: WatchedFlow<[WatchEpisode]>

    init() {
        _rows = State(initialValue: WatchedFlow(library.downloads, initial: []))
    }

    var body: some View {
        Group {
            if rows.value.isEmpty {
                Text(text("downloads_empty_message")).textStyle(Typography.bodyMedium).multilineTextAlignment(.center)
                    .foregroundStyle(Color(.Theme.onSurfaceVariant))
            } else {
                List(rows.value, id: \.episode.id) { row in
                    EpisodeButton(row: row) { WatchPlayerHolder.shared.playNow(row.episode) }
                }
            }
        }
        .navigationTitle(text("filter_downloaded"))
    }
}

/**
 * What can be done with an episode (19.9f): play or pause, play next or add to the queue, download (or cancel or
 * delete it) and mark it heard or not. Each choice goes back to the list, as on Wear OS.
 */
struct EpisodeActionsView: View {
    private let actions: WatchEpisodeActions
    let done: () -> Void
    @State private var state: WatchEpisodeActionsState?

    init(episodeId: String, done: @escaping () -> Void) {
        actions = WatchBridgeKt.watchEpisodeActions(episodeId: episodeId)
        self.done = done
    }

    var body: some View {
        List {
            if let state {
                Text(state.episode.title).textStyle(Typography.titleSmall).multilineTextAlignment(.center)
                    .listRowBackground(Color.clear)
                action(state.isPlaying ? "pause" : "play", state.isPlaying ? "pause.fill" : "play.fill") {
                    try await actions.playPause()
                }
                action("play_next", "text.line.first.and.arrowtriangle.forward") { try await actions.enqueue(next: true) }
                action("add_to_queue", "text.badge.plus") { try await actions.enqueue(next: false) }
                download(state.download)
                if state.episode.isPlayed {
                    action("mark_as_unplayed", "circle") { try await actions.markPlayed(played: false) }
                } else {
                    action("mark_as_played", "checkmark.circle") { try await actions.markPlayed(played: true) }
                }
            }
        }
        .task { state = try? await actions.load() }
    }

    private func download(_ download: DownloadAction) -> some View {
        let (key, symbol) = switch download {
        case .cancel: ("cancel_download", "xmark.circle")
        case .delete_: ("delete_download", "trash")
        default: ("download_cd", "arrow.down.circle")
        }
        return action(key, symbol) { try await actions.download(action: download) }
    }

    private func action(_ key: String, _ symbol: String, run: @escaping () async throws -> Void) -> some View {
        Button {
            Task {
                try? await run()
                done()
            }
        } label: {
            Label(text(key), systemImage: symbol).textStyle(Typography.bodyMedium)
        }
    }
}
