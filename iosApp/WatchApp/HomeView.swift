import SwiftUI
import WatchShared

/// The watch's home (19.9a): what plays now, the popular podcasts to subscribe to and the library's podcasts.
struct HomeView: View {
    @State private var path: [Route] = []
    @State private var podcasts: WatchedFlow<[Podcast]>
    @State private var playing: WatchedFlow<Episode?>

    init() {
        let library = WatchBridgeKt.watchLibrary()
        _podcasts = State(initialValue: WatchedFlow(library.podcasts, initial: []))
        _playing = State(initialValue: WatchedFlow(library.playing, initial: nil))
    }

    var body: some View {
        NavigationStack(path: $path) {
            List {
                if let episode = playing.value {
                    NavigationLink(value: Route.player) {
                        HStack(spacing: Spacing.s) {
                            Thumbnail(url: episode.imageUrl)
                            VStack(alignment: .leading, spacing: Spacing.xxs) {
                                Text(text("now_playing")).textStyle(Typography.labelSmall).foregroundStyle(Color(.Theme.primary))
                                Text(episode.title).textStyle(Typography.bodyMedium).lineLimit(2)
                            }
                        }
                    }
                }
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
                case .player: NowPlayingView()
                }
            }
        }
        // Playing an episode from a list opens the player, as on Wear OS.
        .environment(\.openPlayer) { path.append(.player) }
    }
}

enum Route: Hashable {
    case popular
    case podcast(String)
    case player
}

extension EnvironmentValues {
    @Entry var openPlayer: () -> Void = {}
}
