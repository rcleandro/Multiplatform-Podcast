import WatchShared
import WidgetKit

/**
 * Keeps the watch's complications and Smart Stack widget (25.14f) showing what plays: it saves a snapshot in the App
 * Group and has WidgetKit redraw only when what they show changes (another episode, play or pause, the speed) or the
 * position jumps from where the last snapshot said it would be. Between those, the progress moves by itself.
 */
@MainActor
enum WatchWidgetSync {
    /// A position further than this from the expected one is a seek, not playback going on.
    private static let seekToleranceMs: Double = 5_000
    private static let millisPerSecond: Int64 = 1000
    private static var watching: Watching?
    private static var last: WidgetSnapshot?

    static func start() {
        guard WidgetSnapshot.folder != nil, watching == nil else { return }
        watching = WatchBridgeKt.watch(WatchPlayerHolder.shared.state) { value in
            if let state = value as? PlayerState { save(state) }
        }
    }

    private static func save(_ state: PlayerState) {
        let snapshot = WidgetSnapshot(
            episode: state.currentEpisode.map {
                WidgetSnapshot.Item(id: $0.id, title: $0.title, podcastTitle: $0.podcastTitle, cover: nil)
            },
            isPlaying: state.isPlaying,
            continueListening: [],
            texts: WidgetSnapshot.Texts(nothingPlaying: text("widget_nothing_playing"), play: text("play"),
                                        pause: text("pause"), skipBackward: "", skipForward: ""),
            progress: state.currentEpisode.map { episode in
                WidgetSnapshot.Progress(positionMs: state.position,
                                        durationMs: state.duration?.int64Value ?? episode.duration * millisPerSecond,
                                        speed: Double(state.speed), savedAt: .now)
            }
        )
        guard snapshot.needsRedraw(after: last, toleranceMs: seekToleranceMs) else { return }
        last = snapshot
        try? snapshot.write()
        WidgetCenter.shared.reloadAllTimelines()
    }
}
