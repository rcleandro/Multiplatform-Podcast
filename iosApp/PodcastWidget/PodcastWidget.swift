import AppIntents
import SwiftUI
import WidgetKit

/// The player as a widget (25.12), as the Android PlayerWidget (21.9): the episode playing with play or pause and, from
/// the medium size, the jumps; the large one adds the started episodes, which play with a tap. It shows what the app
/// last saved in the App Group; the buttons run in the app, on its player.
@main
struct PodcastWidgets: WidgetBundle {
    var body: some Widget { PlayerWidget() }
}

struct PlayerWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: WidgetSnapshot.widgetKind, provider: SnapshotProvider()) { entry in
            PlayerWidgetView(snapshot: entry.snapshot)
                .containerBackground(.background, for: .widget)
        }
        .configurationDisplayName("Podcast")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

struct SnapshotEntry: TimelineEntry {
    let date: Date
    let snapshot: WidgetSnapshot?
}

/// One entry, redrawn only when the app asks: nothing in the widget moves with time.
struct SnapshotProvider: TimelineProvider {
    func placeholder(in context: Context) -> SnapshotEntry { SnapshotEntry(date: .now, snapshot: nil) }

    func getSnapshot(in context: Context, completion: @escaping (SnapshotEntry) -> Void) {
        completion(SnapshotEntry(date: .now, snapshot: WidgetSnapshot.read()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SnapshotEntry>) -> Void) {
        completion(Timeline(entries: [SnapshotEntry(date: .now, snapshot: WidgetSnapshot.read())], policy: .never))
    }
}

private enum Layout {
    static let spacing: CGFloat = 8
    static let smallSpacing: CGFloat = 4
    static let coverRadius: CGFloat = 10
    static let coverMedium: CGFloat = 96
    static let continueCover: CGFloat = 56
    static let playSize: CGFloat = 36
    static let skipSize: CGFloat = 22
    static let gridColumns = 2
}

struct PlayerWidgetView: View {
    let snapshot: WidgetSnapshot?
    @Environment(\.widgetFamily) private var family

    var body: some View {
        if let snapshot, let episode = snapshot.episode {
            switch family {
            case .systemSmall: small(episode, snapshot)
            case .systemLarge:
                VStack(alignment: .leading, spacing: Layout.spacing) {
                    medium(episode, snapshot)
                    continueListening(snapshot.continueListening)
                    Spacer(minLength: 0)
                }
            default: medium(episode, snapshot)
            }
        } else {
            // Before the app ran, or with nothing ever played.
            VStack(spacing: Layout.spacing) {
                Image(systemName: "mic").font(.title)
                Text(snapshot?.texts.nothingPlaying ?? "").font(.caption).multilineTextAlignment(.center)
            }
            .foregroundStyle(.secondary)
        }
    }

    private func small(_ episode: WidgetSnapshot.Item, _ snapshot: WidgetSnapshot) -> some View {
        VStack(alignment: .leading, spacing: Layout.smallSpacing) {
            HStack(alignment: .top) {
                Cover(file: episode.cover).frame(width: Layout.continueCover, height: Layout.continueCover)
                Spacer(minLength: 0)
                playPause(snapshot)
            }
            Spacer(minLength: 0)
            titles(episode, lines: 2)
        }
    }

    private func medium(_ episode: WidgetSnapshot.Item, _ snapshot: WidgetSnapshot) -> some View {
        HStack(spacing: Layout.spacing * 2) {
            Cover(file: episode.cover).frame(width: Layout.coverMedium, height: Layout.coverMedium)
            VStack(alignment: .leading, spacing: Layout.spacing) {
                titles(episode, lines: 2)
                HStack(spacing: Layout.spacing * 2) {
                    Button(intent: SkipBackwardIntent()) {
                        Image(systemName: "gobackward").font(.system(size: Layout.skipSize))
                    }
                    .accessibilityLabel(snapshot.texts.skipBackward)
                    playPause(snapshot)
                    Button(intent: SkipForwardIntent()) {
                        Image(systemName: "goforward").font(.system(size: Layout.skipSize))
                    }
                    .accessibilityLabel(snapshot.texts.skipForward)
                }
                .buttonStyle(.plain)
                .foregroundStyle(Color("AccentColor"))
            }
            Spacer(minLength: 0)
        }
    }

    private func titles(_ episode: WidgetSnapshot.Item, lines: Int) -> some View {
        VStack(alignment: .leading, spacing: Layout.smallSpacing / 2) {
            Text(episode.title).font(.headline).lineLimit(lines)
            if let podcast = episode.podcastTitle {
                Text(podcast).font(.caption).foregroundStyle(.secondary).lineLimit(1)
            }
        }
    }

    private func playPause(_ snapshot: WidgetSnapshot) -> some View {
        Button(intent: PlayPauseIntent()) {
            Image(systemName: snapshot.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                .font(.system(size: Layout.playSize))
        }
        .buttonStyle(.plain)
        .foregroundStyle(Color("AccentColor"))
        .accessibilityLabel(snapshot.isPlaying ? snapshot.texts.pause : snapshot.texts.play)
    }

    private func continueListening(_ items: [WidgetSnapshot.Item]) -> some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), alignment: .leading), count: Layout.gridColumns),
                  alignment: .leading, spacing: Layout.spacing) {
            ForEach(items, id: \.id) { item in
                Button(intent: PlayEpisodeIntent(episodeId: item.id)) {
                    HStack(spacing: Layout.spacing) {
                        Cover(file: item.cover).frame(width: Layout.continueCover, height: Layout.continueCover)
                        Text(item.title).font(.caption).lineLimit(3)
                    }
                }
                .buttonStyle(.plain)
            }
        }
    }
}

/// A saved cover, or a symbol when there is none.
private struct Cover: View {
    let file: String?

    var body: some View {
        Group {
            if let file, let url = WidgetSnapshot.folder?.appendingPathComponent(file),
               let image = UIImage(contentsOfFile: url.path) {
                Image(uiImage: image).resizable().scaledToFill()
            } else {
                Image(systemName: "mic").foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(.quaternary)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: Layout.coverRadius))
        .accessibilityHidden(true)
    }
}
