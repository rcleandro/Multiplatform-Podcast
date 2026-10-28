import Shared
import SwiftUI

/// A podcast of the directory: cover, title, author, "genre · 312 episodes", and its subscribe button.
struct DirectoryRow: View {
    let result: DirectoryResult
    let isSubscribing: Bool
    let onSubscribe: () -> Void

    var body: some View {
        let podcast = result.podcast
        HStack(spacing: Spacing.m) {
            Artwork(url: podcast.artworkUrl, size: Sizes.artworkS, radius: Radius.small)
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                HStack(spacing: Spacing.xs) {
                    Text(podcast.title).textStyle(Typography.titleMedium).lineLimit(2)
                    if podcast.explicit_ { ExplicitMark() }
                }
                if let author = podcast.author {
                    Text(author).textStyle(Typography.bodyMedium).foregroundStyle(Color(.Theme.onSurfaceVariant)).lineLimit(1)
                }
                if let details {
                    Text(details).textStyle(Typography.bodySmall).foregroundStyle(Color(.Theme.onSurfaceVariant))
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            subscribe
        }
        .frame(maxWidth: Sizes.readableWidth, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture(perform: onSubscribe)
    }

    @ViewBuilder
    private var subscribe: some View {
        if result.isSubscribed {
            // Just the check: a "Subscribed" button would squeeze the title and author on a phone.
            Image(systemName: "checkmark.circle.fill").foregroundStyle(Color(.Theme.primary))
                .frame(width: Sizes.touchTarget, height: Sizes.touchTarget)
                .accessibilityLabel(Res.string.shared.subscribed.localized())
        } else if isSubscribing {
            ProgressView().frame(width: Sizes.touchTarget, height: Sizes.touchTarget)
        } else {
            Button(Res.string.shared.subscribe.localized(), action: onSubscribe)
                .buttonStyle(.bordered)
                .accessibilityLabel(Res.string.shared.subscribe_to.localized([result.podcast.title]))
        }
    }

    private var details: String? {
        let episodes = result.podcast.episodeCount.map {
            Res.plurals.shared.podcast_episode_count.localized(count: Int(truncating: $0))
        }
        let parts = [result.podcast.genre, episodes].compactMap { $0 }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }
}

/// Every category in the user's alphabetical order (Apple's is the English one); a tap on the chosen one closes it.
struct DirectoryCategories: View {
    let selected: PodcastCategory?
    let onSelect: (PodcastCategory?) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.s) {
                ForEach(Self.sorted(), id: \.self) { category in
                    Chip(label: category.localized(), isOn: category == selected) {
                        onSelect(category == selected ? nil : category)
                    }
                }
            }
        }
    }

    static func sorted(bundle: Bundle = .main) -> [PodcastCategory] {
        PodcastCategory.entries.sorted {
            $0.localized(bundle: bundle).localizedCaseInsensitiveCompare($1.localized(bundle: bundle)) == .orderedAscending
        }
    }
}

/// The directory's answer to what was asked: loading, a failure to retry, nothing found, or the podcasts, with the
/// opened link, "For you" or "Like …" and the chart's title above them while nothing is typed.
struct DirectoryResults: View {
    let state: DiscoverUiState
    let onSubscribe: (DirectoryResult) -> Void
    let onRetry: () -> Void
    let onAddByUrl: () -> Void

    var body: some View {
        if state.isSearching && state.results.isEmpty {
            ProgressView().frame(maxWidth: .infinity).padding(Spacing.xl)
        } else if state.searchFailed {
            ContentUnavailableView {
                Label(Res.string.shared.error_search_directory.localized(), systemImage: "icloud.slash")
            } actions: {
                Button(Res.string.shared.try_again.localized(), action: onRetry)
            }
        } else if state.results.isEmpty {
            ContentUnavailableView {
                Label(Res.string.shared.no_podcasts_found.localized(), systemImage: "magnifyingglass")
            } actions: {
                Button(Res.string.shared.add_by_url.localized(), action: onAddByUrl)
            }
        } else {
            if state.query.isEmpty {
                if let linked = state.linked {
                    SectionTitle(Res.string.shared.opened_link.localized())
                    row(linked)
                }
                recommendations
                SectionTitle(state.category?.localized() ?? Res.string.shared.popular.localized())
            }
            ForEach(state.results, id: \.podcast.feedUrl) { row($0) }
        }
    }

    @ViewBuilder
    private var recommendations: some View {
        if let similar = state.similar {
            SectionTitle(Res.string.shared.similar_to.localized([similar.title]))
            if similar.results.isEmpty {
                Text(Res.string.shared.similar_none.localized()).textStyle(Typography.bodyMedium).foregroundStyle(Color(.Theme.onSurfaceVariant))
            }
            ForEach(similar.results, id: \.podcast.feedUrl) { row($0) }
        } else if state.category == nil && !state.forYou.isEmpty {
            SectionTitle(Res.string.shared.for_you.localized())
            ForEach(state.forYou, id: \.podcast.feedUrl) { row($0) }
        }
    }

    private func row(_ result: DirectoryResult) -> some View {
        DirectoryRow(result: result, isSubscribing: state.subscribing.contains(result.podcast.feedUrl)) { onSubscribe(result) }
    }
}
