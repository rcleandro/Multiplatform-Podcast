import Foundation

/// What the app hands the widget (25.12) through the App Group: the episode playing, its controls' texts, and the
/// started episodes, each with a small cover saved beside it. The widget cannot read the shared Kotlin code.
struct WidgetSnapshot: Codable, Equatable {
    struct Item: Codable, Equatable {
        let id: String
        let title: String
        let podcastTitle: String?
        /// A file in [WidgetSnapshot.folder], or nil while it has no cover.
        let cover: String?
    }

    struct Texts: Codable, Equatable {
        let nothingPlaying: String
        let play: String
        let pause: String
        let skipBackward: String
        let skipForward: String
    }

    /// Where the episode is, for the Apple Watch's complications (25.14f): enough to draw the progress moving by itself
    /// while it plays, with no redraw each second.
    struct Progress: Codable, Equatable {
        let positionMs: Int64
        let durationMs: Int64
        let speed: Double
        let savedAt: Date

        static let millisPerSecond: Double = 1000

        /// When the episode started and will end at this speed, as if it played without stopping since [savedAt].
        var interval: ClosedRange<Date> {
            let start = savedAt.addingTimeInterval(-Double(positionMs) / Self.millisPerSecond / speed)
            return start...start.addingTimeInterval(Double(durationMs) / Self.millisPerSecond / speed)
        }

        var fraction: Double { durationMs > 0 ? min(1, Double(positionMs) / Double(durationMs)) : 0 }
    }

    let episode: Item?
    let isPlaying: Bool
    let continueListening: [Item]
    let texts: Texts
    /// Only the watch writes it; nil on the iPhone and in snapshots saved before it existed.
    var progress: Progress? = nil

    static let appGroup = "group.br.com.carvalho.podcast"
    static let widgetKind = "PlayerWidget"

    /// The App Group's folder for the widget, or nil where the app was signed without the group (the CI build).
    static var folder: URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroup)?
            .appendingPathComponent("Widget", isDirectory: true)
    }

    private static var file: URL? { folder?.appendingPathComponent("snapshot.json") }

    /// Whether this shows something [old] did not, playback going on as expected aside (25.14f): another episode, play or
    /// pause, the speed or the length, or a position further than [toleranceMs] from where [old] said it would be.
    func needsRedraw(after old: WidgetSnapshot?, toleranceMs: Double) -> Bool {
        guard let old else { return true }
        if old.episode != episode || old.isPlaying != isPlaying { return true }
        guard let before = old.progress, let now = progress else { return old.progress != progress }
        if before.speed != now.speed || before.durationMs != now.durationMs { return true }
        let played = old.isPlaying ? now.savedAt.timeIntervalSince(before.savedAt) * Progress.millisPerSecond * before.speed : 0
        return abs(Double(now.positionMs) - (Double(before.positionMs) + played)) > toleranceMs
    }

    static func read() -> WidgetSnapshot? {
        guard let file, let data = try? Data(contentsOf: file) else { return nil }
        return try? JSONDecoder().decode(WidgetSnapshot.self, from: data)
    }

    func write() throws {
        guard let folder = Self.folder, let file = Self.file else { return }
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        try JSONEncoder().encode(self).write(to: file, options: .atomic)
    }
}
