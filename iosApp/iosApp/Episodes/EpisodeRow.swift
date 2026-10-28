import Shared
import SwiftUI

/// What a download looks like on screen, from the downloader's status and what the database already has.
enum EpisodeDownload: Equatable {
    case idle, queued, downloaded, failed
    case downloading(Double)

    init(_ status: DownloadStatus?, isDownloaded: Bool) {
        switch status {
        case is DownloadStatus.Queued: self = .queued
        case let downloading as DownloadStatus.Downloading: self = .downloading(Double(downloading.progress))
        case is DownloadStatus.Completed: self = .downloaded
        case is DownloadStatus.Failed: self = .failed
        default: self = isDownloaded ? .downloaded : .idle
        }
    }
}

/// Everything a person can do with one episode from a list; nil leaves the action out.
struct EpisodeActions {
    var play: () -> Void
    var playNext: (() -> Void)?
    var addToQueue: (() -> Void)?
    var markPlayed: (() -> Void)?
    var markUnplayed: (() -> Void)?
    var markOlderPlayed: (() -> Void)?
    var markOlderUnplayed: (() -> Void)?
    var download: (() -> Void)?
    var cancelDownload: (() -> Void)?
    var deleteDownload: (() -> Void)?
    var dismissNew: (() -> Void)?
    var goToPodcast: (() -> Void)?
    /// Starts choosing several episodes (18.11).
    var select: (() -> Void)?
}

/// An episode in a list: "Trailer · 2 days ago · 45min" under its title, how much was heard, the play button, and the
/// rest of what can be done in its menu, as the Compose rows have (core/ui EpisodeListItem).
struct EpisodeRow: View {
    let episode: Episode
    var podcastTitle: String?
    let isPlaying: Bool
    let isLoading: Bool
    let download: EpisodeDownload
    let actions: EpisodeActions
    /// Choosing several episodes (18.11): a tap marks this one instead of opening it.
    var selected: Bool?
    @Environment(\.dynamicTypeSize) private var textSize

    private static let titleLines = 2

    var body: some View {
        HStack(alignment: .top, spacing: Spacing.m) {
            if let selected {
                Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                    .font(.title2).foregroundStyle(Color(.Theme.primary))
                    .accessibilityHidden(true)
            }
            Artwork(url: episode.imageUrl, size: Sizes.artworkS, radius: Radius.small)
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(episode.title).textStyle(Typography.titleMedium)
                    // The whole title at the accessibility text sizes, where two lines hold a word or two.
                    .lineLimit(textSize.isAccessibilitySize ? nil : Self.titleLines)
                    .foregroundStyle(episode.isPlayed ? Color(.Theme.onSurfaceVariant) : Color(.Theme.onSurface))
                Text(metadata).textStyle(Typography.bodySmall).foregroundStyle(Color(.Theme.onSurfaceVariant))
                if progress > 0 { ProgressView(value: progress).tint(Color(.Theme.primary)) }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if selected == nil { playButton }
        }
        .frame(maxWidth: Sizes.readableWidth, alignment: .leading)
        .contentShape(Rectangle())
        .contextMenu { EpisodeMenu(isPlaying: isPlaying, download: download, actions: actions) }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(selected == true ? .isSelected : [])
        .accessibilityAction(named: Text(playLabel), actions.play)
    }

    private var playButton: some View {
        Button(action: actions.play) {
            Group {
                if isLoading { ProgressView() } else { Image(systemName: isPlaying ? "pause.circle.fill" : "play.circle.fill") }
            }
            .font(.title)
            .foregroundStyle(Color(.Theme.primary))
            .frame(width: Sizes.touchTarget, height: Sizes.touchTarget)
        }
        .buttonStyle(.borderless)
        .accessibilityLabel(playLabel)
    }

    private var playLabel: String {
        (isPlaying ? Res.string.shared.pause : Res.string.shared.play).localized()
    }

    private static let millisPerSecond = 1000.0

    private var progress: Double {
        let duration = Double(episode.duration) * Self.millisPerSecond
        return duration > 0 && !episode.isPlayed ? min(Double(episode.playbackPosition) / duration, 1) : 0
    }

    private var metadata: String {
        let left = episode.remainingDuration().map { Res.string.shared.remaining_time.localized([$0]) }
        return [episode.type.label, podcastTitle, relativeTime(episode.publishDate), left ?? LongExtensionsKt.toDuration(episode.duration)]
            .compactMap { $0 }.joined(separator: " · ")
    }
}

/// The actions of [EpisodeRow], as its context menu.
struct EpisodeMenu: View {
    let isPlaying: Bool
    let download: EpisodeDownload
    let actions: EpisodeActions

    var body: some View {
        Button(isPlaying ? Res.string.shared.pause.localized() : Res.string.shared.play.localized(),
               systemImage: isPlaying ? "pause" : "play", action: actions.play)
        item(Res.string.shared.play_next, "text.line.first.and.arrowtriangle.forward", actions.playNext)
        item(Res.string.shared.add_to_queue, "text.append", actions.addToQueue)
        Divider()
        item(Res.string.shared.mark_as_played, "checkmark.circle", actions.markPlayed)
        item(Res.string.shared.mark_as_unplayed, "circle", actions.markUnplayed)
        item(Res.string.shared.mark_older_as_played, "checkmark.circle.badge.questionmark", actions.markOlderPlayed)
        item(Res.string.shared.mark_older_as_unplayed, "circle.dashed", actions.markOlderUnplayed)
        item(Res.string.shared.dismiss_new, "tray.and.arrow.down", actions.dismissNew)
        item(Res.string.shared.go_to_podcast, "square.stack", actions.goToPodcast)
        item(Res.string.shared.select, "checkmark.circle", actions.select)
        Divider()
        switch download {
        case .idle: item(Res.string.shared.download_cd, "arrow.down.circle", actions.download)
        case .failed: item(Res.string.shared.retry_download, "arrow.clockwise", actions.download)
        case .queued, .downloading: item(Res.string.shared.cancel_download, "xmark.circle", actions.cancelDownload)
        case .downloaded: item(Res.string.shared.delete_download, "trash", actions.deleteDownload, role: .destructive)
        }
    }

    @ViewBuilder
    private func item(_ label: LibraryStringResource, _ symbol: String, _ action: (() -> Void)?, role: ButtonRole? = nil) -> some View {
        if let action { Button(label.localized(), systemImage: symbol, role: role, action: action) }
    }
}

private extension EpisodeType {
    var label: String? {
        switch self {
        case .trailer: Res.string.shared.episode_trailer.localized()
        case .bonus: Res.string.shared.episode_bonus.localized()
        default: nil
        }
    }
}
