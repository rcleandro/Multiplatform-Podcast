import SwiftUI
import WatchShared

/// A podcast's episodes, newest first unless the podcast says otherwise; a tap plays one (19.9a). Each row says where
/// the episode stands, as the Wear OS rows do (19.9g), and a heard one is drawn dimmer.
struct PodcastView: View {
    private let page: WatchPodcast
    @State private var state: WatchedFlow<WatchPodcastState?>

    init(podcastId: String) {
        let page = WatchBridgeKt.watchPodcast(podcastId: podcastId)
        self.page = page
        _state = State(initialValue: WatchedFlow(page.state, initial: nil))
    }

    var body: some View {
        Group {
            if let state = state.value {
                List(state.episodes, id: \.episode.id) { row in
                    Button { Task { try? await page.play(episode: row.episode) } } label: { EpisodeRow(row: row) }
                }
                .navigationTitle(state.title)
            } else {
                ProgressView()
            }
        }
    }
}

struct EpisodeRow: View {
    let row: WatchEpisode
    private static let progressSize: CGFloat = 32

    var body: some View {
        HStack(spacing: Spacing.s) {
            icon.frame(width: Self.progressSize, height: Self.progressSize)
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text(row.episode.title).textStyle(Typography.bodyMedium).lineLimit(2)
                if let detail { Text(detail).textStyle(Typography.labelMedium).foregroundStyle(Color(.Theme.onSurfaceVariant)) }
            }
        }
        .opacity(row.episode.isPlayed ? Opacity.muted : 1)
    }

    @ViewBuilder private var icon: some View {
        switch row.status {
        case is EpisodeStatusPlaying: Image(systemName: "waveform").foregroundStyle(Color(.Theme.primary))
        case let downloading as EpisodeStatusDownloading:
            if let progress = downloading.progress { ProgressView(value: progress.floatValue).progressViewStyle(.circular) } else { ProgressView() }
        default: Thumbnail(url: row.episode.imageUrl)
        }
    }

    /// "Playing", "Downloading", "Heard", "12 min left" or the length, then whether it is on the watch.
    private var detail: String? {
        let status: String? = switch row.status {
        case is EpisodeStatusPlaying: text("episode_playing")
        case is EpisodeStatusDownloading: text("episode_downloading")
        case is EpisodeStatusPlayed: text("episode_played")
        case let remaining as EpisodeStatusRemaining: text("remaining_time", remaining.time)
        case let length as EpisodeStatusLength: length.time
        default: nil
        }
        let parts = [status, row.isOnWatch ? text("filter_downloaded") : nil].compactMap { $0 }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }
}
