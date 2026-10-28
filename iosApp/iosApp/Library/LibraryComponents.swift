import Shared
import SwiftUI

/// A podcast in the grid: cover, title, author and the unplayed count over the cover.
struct PodcastCard: View {
    let entry: LibraryEntry

    var body: some View {
        let podcast = entry.podcast
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Artwork(url: podcast.imageUrl, size: nil)
                .overlay(alignment: .topTrailing) {
                    if entry.unplayedCount > 0 { UnplayedBadge(count: Int(entry.unplayedCount)).padding(Spacing.xs) }
                }
            HStack(spacing: Spacing.xs) {
                Text(podcast.title).textStyle(Typography.titleMedium).lineLimit(2)
                if podcast.explicit_ { ExplicitMark() }
            }
            if let author = podcast.author {
                Text(author).textStyle(Typography.bodySmall).foregroundStyle(Color(.Theme.onSurfaceVariant)).lineLimit(1)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

/// A podcast in the list: "2 days ago · 3 unplayed" under its title, each half left out when there is nothing to say.
struct PodcastRow: View {
    let entry: LibraryEntry

    var body: some View {
        let podcast = entry.podcast
        HStack(spacing: Spacing.m) {
            Artwork(url: podcast.imageUrl, size: Sizes.artworkS, radius: Radius.small)
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                HStack(spacing: Spacing.xs) {
                    Text(podcast.title).textStyle(Typography.titleMedium).lineLimit(2)
                    if podcast.explicit_ { ExplicitMark() }
                }
                if let author = podcast.author {
                    Text(author).textStyle(Typography.bodyMedium).foregroundStyle(Color(.Theme.onSurfaceVariant)).lineLimit(1)
                }
                if let summary {
                    Text(summary).textStyle(Typography.bodySmall).foregroundStyle(Color(.Theme.onSurfaceVariant))
                }
            }
        }
        .frame(maxWidth: Sizes.readableWidth, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    private var summary: String? {
        let latest = entry.latestEpisodeDate.flatMap { relativeTime($0.int64Value) }
        let unplayed = entry.unplayedCount > 0
            ? Res.plurals.shared.library_unplayed.localized(count: Int(entry.unplayedCount)) : nil
        let parts = [latest, unplayed].compactMap { $0 }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }
}

/// A started episode in "Continue listening": a tap plays it from where it stopped.
struct ContinueCard: View {
    let episode: Episode
    let onPlay: () -> Void
    /// Wider with the text size, up to what a phone holds, so a large title does not break inside its words.
    @ScaledMetric(relativeTo: .subheadline) private var width = Sizes.artworkM
    @Environment(\.dynamicTypeSize) private var textSize

    private static let widest: CGFloat = 280
    private static let titleLines = 2

    var body: some View {
        Button(action: onPlay) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Artwork(url: episode.imageUrl, size: nil)
                ProgressView(value: progress).tint(Color(.Theme.primary))
                Text(episode.title).textStyle(Typography.labelLarge)
                    .lineLimit(textSize.isAccessibilitySize ? nil : Self.titleLines)
                if let left = episode.remainingDuration() {
                    Text(Res.string.shared.remaining_time.localized([left]))
                        .textStyle(Typography.bodySmall).foregroundStyle(Color(.Theme.onSurfaceVariant))
                }
            }
            .frame(width: min(width, Self.widest))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityHint(Res.string.shared.play.localized())
    }

    private static let millisPerSecond = 1000.0

    private var progress: Double {
        let duration = Double(episode.duration) * Self.millisPerSecond
        return duration > 0 ? min(Double(episode.playbackPosition) / duration, 1) : 0
    }
}

/// "All" and the library's categories, the most common first; picking one shows only its podcasts (18.2).
struct CategoryChips: View {
    let categories: [PodcastCategory]
    let selected: PodcastCategory?
    let onSelect: (PodcastCategory?) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.s) {
                Chip(label: Res.string.shared.category_all.localized(), isOn: selected == nil) { onSelect(nil) }
                ForEach(categories, id: \.self) { category in
                    Chip(label: category.localized(), isOn: selected == category) { onSelect(category) }
                }
            }
        }
    }
}

struct SectionTitle: View {
    let text: String

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text.uppercased()).textStyle(Typography.section).foregroundStyle(Color(.Theme.onSurfaceVariant))
            .accessibilityAddTraits(.isHeader)
    }
}

private struct UnplayedBadge: View {
    let count: Int

    var body: some View {
        Text(count, format: .number).textStyle(Typography.labelMedium)
            .padding(.horizontal, Spacing.s)
            .padding(.vertical, Spacing.xxs)
            .foregroundStyle(Color(.Theme.onBrand))
            .background(Color(.Theme.brand), in: Capsule())
    }
}

/// The "E" next to an explicit podcast, read out as the word.
struct ExplicitMark: View {
    var body: some View {
        Image(systemName: "e.square.fill").foregroundStyle(Color(.Theme.onSurfaceVariant))
            .accessibilityLabel(Text("ds_explicit"))
    }
}
