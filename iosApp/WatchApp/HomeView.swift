import SwiftUI
import WatchShared

/// The watch's home (19.9a): the popular podcasts to subscribe to and the library's podcasts, each opening its
/// episodes. What plays now joins it with the player (25.14c).
struct HomeView: View {
    @State private var podcasts: WatchedFlow<[Podcast]>

    init() {
        _podcasts = State(initialValue: WatchedFlow(WatchBridgeKt.watchLibrary().podcasts, initial: []))
    }

    var body: some View {
        NavigationStack {
            List {
                NavigationLink(value: Route.popular) {
                    Label(text("popular"), systemImage: "chart.line.uptrend.xyaxis").textStyle(Typography.titleMedium)
                }
                ForEach(podcasts.value, id: \.id) { podcast in
                    NavigationLink(value: Route.podcast(podcast.id)) {
                        HStack(spacing: Spacing.s) {
                            Thumbnail(url: podcast.imageUrl)
                            Text(podcast.title).textStyle(Typography.bodyMedium).lineLimit(2)
                        }
                    }
                }
            }
            .navigationTitle(text("library_title"))
            .navigationDestination(for: Route.self) { route in
                switch route {
                case .popular: PopularView()
                case let .podcast(id): PodcastView(podcastId: id)
                }
            }
        }
    }
}

enum Route: Hashable {
    case popular
    case podcast(String)
}
