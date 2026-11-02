import SwiftUI
import WidgetKit

/**
 * The episode playing on the watch face and in the Smart Stack (25.14f), as the Wear OS complication and tile (19.9f):
 * a ring of the progress around the app's symbol, or the title, the podcast and a bar. While it plays the progress
 * moves by itself; the app only asks for a redraw when something else changes. A tap opens the app.
 */
@main
struct PodcastWatchWidgets: WidgetBundle {
    var body: some Widget { NowPlayingComplication() }
}

struct NowPlayingComplication: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: WidgetSnapshot.widgetKind, provider: SnapshotProvider()) { entry in
            ComplicationView(snapshot: entry.snapshot)
                .containerBackground(for: .widget) { Color.clear }
        }
        .configurationDisplayName("Podcast")
        .supportedFamilies([.accessoryCircular, .accessoryCorner, .accessoryRectangular, .accessoryInline])
    }
}

struct SnapshotEntry: TimelineEntry {
    let date: Date
    let snapshot: WidgetSnapshot?
    /// The Smart Stack puts the widget forward while something plays.
    let relevance: TimelineEntryRelevance?
}

struct SnapshotProvider: TimelineProvider {
    private static let playingScore: Float = 100
    private static let pausedScore: Float = 1

    func placeholder(in context: Context) -> SnapshotEntry { SnapshotEntry(date: .now, snapshot: nil, relevance: nil) }

    func getSnapshot(in context: Context, completion: @escaping (SnapshotEntry) -> Void) {
        completion(entry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SnapshotEntry>) -> Void) {
        completion(Timeline(entries: [entry()], policy: .never))
    }

    private func entry() -> SnapshotEntry {
        let snapshot = WidgetSnapshot.read()
        let score = snapshot?.isPlaying == true ? Self.playingScore : Self.pausedScore
        return SnapshotEntry(date: .now, snapshot: snapshot, relevance: TimelineEntryRelevance(score: score))
    }
}

struct ComplicationView: View {
    let snapshot: WidgetSnapshot?
    @Environment(\.widgetFamily) private var family

    private static let symbol = "headphones"
    private static let lineSpacing: CGFloat = 2

    var body: some View {
        switch family {
        case .accessoryRectangular: rectangular
        case .accessoryInline: Label(snapshot?.episode?.title ?? snapshot?.texts.nothingPlaying ?? "", systemImage: Self.symbol)
        case .accessoryCorner:
            Image(systemName: Self.symbol).font(.title3).widgetLabel { progress }
        default:
            ZStack {
                AccessoryWidgetBackground()
                progress.progressViewStyle(.circular)
                Image(systemName: Self.symbol)
            }
        }
    }

    private var rectangular: some View {
        VStack(alignment: .leading, spacing: Self.lineSpacing) {
            if let episode = snapshot?.episode {
                Label(episode.podcastTitle ?? "", systemImage: Self.symbol).font(.caption2).foregroundStyle(.tint).lineLimit(1)
                Text(episode.title).font(.headline).lineLimit(2).widgetAccentable()
                progress.progressViewStyle(.linear)
            } else {
                Label(snapshot?.texts.nothingPlaying ?? "", systemImage: Self.symbol).font(.headline)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// Moving by itself while the episode plays; still while paused.
    @ViewBuilder private var progress: some View {
        if let progress = snapshot?.progress {
            if snapshot?.isPlaying == true {
                ProgressView(timerInterval: progress.interval, countsDown: false) { EmptyView() } currentValueLabel: { EmptyView() }
            } else {
                ProgressView(value: progress.fraction)
            }
        } else {
            ProgressView(value: 0)
        }
    }
}
