import SwiftUI
import WatchShared

/// The Apple Watch app (ADR 0016), over the same database, network and player as the phone through WatchShared. For
/// now (25.14a) it only shows the library, which proves the Kotlin side runs on the watch; the screens come in 25.14b.
@main
struct PodcastWatchApp: App {
    init() {
        WatchBridgeKt.startWatch()
    }

    var body: some Scene {
        WindowGroup { LibraryList() }
    }
}

private struct LibraryList: View {
    @State private var podcasts = WatchedList<Podcast>(WatchBridgeKt.watchPodcasts())

    var body: some View {
        NavigationStack {
            List(podcasts.value, id: \.id) { podcast in
                Text(podcast.title).lineLimit(2)
            }
        }
    }
}

/// A Kotlin flow of lists followed by SwiftUI, as `Watched` does on the phone (ADR 0015).
@Observable
final class WatchedList<Element> {
    private(set) var value: [Element] = []
    @ObservationIgnored private var watching: Watching?

    init(_ flow: any Kotlinx_coroutines_coreFlow) {
        watching = WatchBridgeKt.watch(flow) { [weak self] next in
            self?.value = next as? [Element] ?? []
        }
    }

    deinit {
        watching?.cancel()
    }
}
