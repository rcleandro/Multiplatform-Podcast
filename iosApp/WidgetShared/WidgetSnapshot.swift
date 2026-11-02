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

    let episode: Item?
    let isPlaying: Bool
    let continueListening: [Item]
    let texts: Texts

    static let appGroup = "group.br.com.carvalho.podcast"
    static let widgetKind = "PlayerWidget"

    /// The App Group's folder for the widget, or nil where the app was signed without the group (the CI build).
    static var folder: URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroup)?
            .appendingPathComponent("Widget", isDirectory: true)
    }

    private static var file: URL? { folder?.appendingPathComponent("snapshot.json") }

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
