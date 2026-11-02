import CryptoKit
import Shared
import UIKit
import WidgetKit

/// Keeps the widget (25.12) showing what the app's player has: on every change of episode, play or pause, or the
/// started episodes, it saves a snapshot with small covers in the App Group and has WidgetKit redraw. The buttons'
/// App Intents run here, in the app's process, on the same player.
@MainActor
enum WidgetSync {
    /// Started episodes the large widget has room for.
    static let continueLimit = 4
    /// Covers are saved at most this big, in pixels: a widget's images share a small memory budget.
    private static let coverPixels: CGFloat = 256
    private static let coverQuality: CGFloat = 0.8
    private static var watching: Watching?
    private static var last: WidgetSnapshot?
    /// Covers load while a newer state may arrive; only the newest one is saved.
    private static var generation = 0

    static func start() {
        WidgetCommand.handler = { command in
            switch command {
            case .playPause: PlayerWidget_iosKt.widgetPlayPause()
            case .skipBackward: PlayerWidget_iosKt.widgetSkipBackward()
            case .skipForward: PlayerWidget_iosKt.widgetSkipForward()
            case let .play(id): try? await PlayerWidget_iosKt.widgetPlayEpisode(id: id)
            }
        }
        guard WidgetSnapshot.folder != nil, watching == nil else { return }
        watching = SwiftBridgeKt.watch(PlayerWidget_iosKt.playerWidgetState()) { state in
            guard let state = state as? PlayerWidgetState else { return }
            Task { await save(state) }
        }
    }

    private static func save(_ state: PlayerWidgetState) async {
        generation += 1
        let mine = generation
        let started = Array(state.continueListening.prefix(continueLimit))
        var items: [WidgetSnapshot.Item] = []
        for episode in [state.episode].compactMap({ $0 }) + started { items.append(await item(episode)) }
        let snapshot = WidgetSnapshot(
            episode: state.episode == nil ? nil : items.first,
            isPlaying: state.isPlaying,
            continueListening: Array(items.dropFirst(state.episode == nil ? 0 : 1)),
            texts: texts(state.skipIntervals)
        )
        // The started episodes' positions change as they play; the widget shows none of that.
        guard mine == generation, snapshot != last else { return }
        last = snapshot
        try? snapshot.write()
        removeCovers(except: Set(items.compactMap(\.cover)))
        WidgetCenter.shared.reloadTimelines(ofKind: WidgetSnapshot.widgetKind)
    }

    private static func item(_ episode: Episode) async -> WidgetSnapshot.Item {
        WidgetSnapshot.Item(id: episode.id, title: episode.title, podcastTitle: episode.podcastTitle,
                            cover: await cover(episode.imageUrl))
    }

    /// The cover at [url], shrunk and saved once under a name from its address.
    private static func cover(_ url: String?) async -> String? {
        guard let url, let address = URL(string: url), let folder = WidgetSnapshot.folder else { return nil }
        let name = SHA256.hash(data: Data(url.utf8)).map { String(format: "%02x", $0) }.joined() + ".jpg"
        let file = folder.appendingPathComponent(name)
        if FileManager.default.fileExists(atPath: file.path) { return name }
        guard let (data, _) = try? await URLSession.shared.data(from: address), let image = UIImage(data: data) else { return nil }
        let side = min(coverPixels, max(image.size.width, image.size.height))
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let small = UIGraphicsImageRenderer(size: CGSize(width: side, height: side), format: format).image { _ in
            image.draw(in: CGRect(x: 0, y: 0, width: side, height: side))
        }
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        guard (try? small.jpegData(compressionQuality: coverQuality)?.write(to: file)) != nil else { return nil }
        return name
    }

    private static func removeCovers(except kept: Set<String>) {
        guard let folder = WidgetSnapshot.folder,
              let files = try? FileManager.default.contentsOfDirectory(atPath: folder.path) else { return }
        for file in files where file.hasSuffix(".jpg") && !kept.contains(file) {
            try? FileManager.default.removeItem(at: folder.appendingPathComponent(file))
        }
    }

    static func texts(_ skips: SkipIntervals, bundle: Bundle = .main) -> WidgetSnapshot.Texts {
        WidgetSnapshot.Texts(
            nothingPlaying: Res.string.shared.widget_nothing_playing.localized(bundle: bundle),
            play: Res.string.shared.play.localized(bundle: bundle),
            pause: Res.string.shared.pause.localized(bundle: bundle),
            skipBackward: Res.string.shared.skip_backward.localized([KotlinInt(int: skips.backwardSeconds)], bundle: bundle),
            skipForward: Res.string.shared.skip_forward.localized([KotlinInt(int: skips.forwardSeconds)], bundle: bundle)
        )
    }
}
