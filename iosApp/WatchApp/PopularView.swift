import SwiftUI
import WatchShared

/// The most popular podcasts, subscribed with a tap (19.9a): on a watch there is no keyboard for a feed address. A row
/// being subscribed says so at once, and a subscribed one is drawn dimmer.
struct PopularView: View {
    private let popular = WatchBridgeKt.watchPopular()
    @State private var state: WatchedFlow<WatchPopularState?>

    init() {
        _state = State(initialValue: WatchedFlow(popular.state, initial: nil))
    }

    var body: some View {
        Group {
            if let state = state.value, !state.isLoading {
                if state.failed {
                    VStack(spacing: Spacing.s) {
                        Text(text("error_search_directory")).textStyle(Typography.bodyMedium).multilineTextAlignment(.center)
                        Button(text("try_again")) { Task { try? await popular.load() } }
                    }
                } else {
                    List(state.results, id: \.podcast.feedUrl) { result in
                        row(result, subscribing: state.subscribing.contains(result.podcast.feedUrl))
                    }
                }
            } else {
                ProgressView()
            }
        }
        .navigationTitle(text("popular"))
        .task { try? await popular.load() }
    }

    private func row(_ result: DirectoryResult, subscribing: Bool) -> some View {
        Button { Task { try? await popular.subscribe(result: result) } } label: {
            HStack(spacing: Spacing.s) {
                if subscribing { ProgressView().frame(width: Sizes.iconL, height: Sizes.iconL) } else { Thumbnail(url: result.podcast.artworkUrl) }
                VStack(alignment: .leading, spacing: Spacing.xxs) {
                    Text(result.podcast.title).textStyle(Typography.bodyMedium).lineLimit(2)
                    Text(text(subscribing ? "subscribing" : result.isSubscribed ? "subscribed" : "subscribe"))
                        .textStyle(Typography.labelMedium).foregroundStyle(Color(.Theme.primary))
                }
            }
        }
        .opacity(result.isSubscribed ? Opacity.muted : 1)
    }
}
