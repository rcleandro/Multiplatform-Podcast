import SwiftUI
import WatchShared

/// The Apple Watch app (ADR 0016), over the same database, network and player as the phone through WatchShared, and
/// with the phone's look: the dark theme, Onest and amber (as the Wear OS app, 19.9d).
@main
struct PodcastWatchApp: App {
    init() {
        WatchBridgeKt.startWatch()
    }

    var body: some Scene {
        WindowGroup {
            // The theme's colors have a watch entry with their dark values (IosTokensTest): the watch draws on black.
            HomeView().tint(Color(.Theme.primary))
        }
    }
}

/// A Kotlin flow followed by SwiftUI on the main thread, as `Watched` does on the phone (ADR 0015).
@Observable
final class WatchedFlow<Value> {
    private(set) var value: Value
    @ObservationIgnored private var watching: Watching?

    init(_ flow: any Kotlinx_coroutines_coreFlow, initial: Value) {
        value = initial
        watching = WatchBridgeKt.watch(flow) { [weak self] next in
            if let next = next as? Value { self?.value = next }
        }
    }

    deinit {
        watching?.cancel()
    }
}

/// A text of the shared catalog, with its arguments.
func text(_ key: String, _ args: CVarArg...) -> String {
    String(format: String(localized: String.LocalizationValue(key)), arguments: args)
}

/// A cover, rounded, or a microphone while it loads or when there is none.
struct Thumbnail: View {
    let url: String?
    private static let size: CGFloat = 32

    var body: some View {
        AsyncImage(url: url.flatMap(URL.init(string:))) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            Image(systemName: "mic").foregroundStyle(Color(.Theme.onSurfaceVariant))
        }
        .frame(width: Self.size, height: Self.size)
        .clipShape(RoundedRectangle(cornerRadius: Radius.small))
        .accessibilityHidden(true)
    }
}
