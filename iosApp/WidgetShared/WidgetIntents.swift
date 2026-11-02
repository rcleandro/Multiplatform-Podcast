import AppIntents

/// A widget button (25.12). The intents are built into the widget too, but iOS runs an AudioPlaybackIntent in the
/// app's process, where WidgetSync gives [handler] the shared player; in the widget it stays nil.
enum WidgetCommand {
    case playPause, skipBackward, skipForward
    case play(episodeId: String)

    @MainActor static var handler: ((WidgetCommand) async -> Void)?

    @MainActor static func run(_ command: WidgetCommand) async { await handler?(command) }
}

struct PlayPauseIntent: AudioPlaybackIntent {
    static let title: LocalizedStringResource = "play"
    static let isDiscoverable = false

    func perform() async throws -> some IntentResult {
        await WidgetCommand.run(.playPause)
        return .result()
    }
}

struct SkipBackwardIntent: AudioPlaybackIntent {
    static let title: LocalizedStringResource = "previous"
    static let isDiscoverable = false

    func perform() async throws -> some IntentResult {
        await WidgetCommand.run(.skipBackward)
        return .result()
    }
}

struct SkipForwardIntent: AudioPlaybackIntent {
    static let title: LocalizedStringResource = "next"
    static let isDiscoverable = false

    func perform() async throws -> some IntentResult {
        await WidgetCommand.run(.skipForward)
        return .result()
    }
}

/// Plays one of the started episodes the widget lists.
struct PlayEpisodeIntent: AudioPlaybackIntent {
    static let title: LocalizedStringResource = "play"
    static let isDiscoverable = false

    @Parameter(title: "id") var episodeId: String

    init() {}

    init(episodeId: String) { self.episodeId = episodeId }

    func perform() async throws -> some IntentResult {
        await WidgetCommand.run(.play(episodeId: episodeId))
        return .result()
    }
}
