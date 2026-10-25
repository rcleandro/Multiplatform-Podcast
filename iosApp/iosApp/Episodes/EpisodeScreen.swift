import Shared
import SwiftUI

/// An episode's page (25.5): its cover, when it came out and how long it is, play or resume, what can be done with it,
/// and its description, as the Compose screen does (feature/episode).
struct EpisodeScreen: View {
    let model: ShellModel
    let episodeId: String
    @State private var scope: ScreenScope
    @State private var state: Watched<EpisodeDetailUiState>
    @State private var safety = Watched<ChildSafetyState>(SwiftSafetyKt.childSafety())
    @State private var reporting: ReportTarget?

    init(model: ShellModel, episodeId: String) {
        self.model = model
        self.episodeId = episodeId
        let scope = ScreenScope()
        _scope = State(initialValue: scope)
        _state = State(initialValue: Watched(scope.viewModels.episode(episodeId: episodeId).uiState))
    }

    private var viewModel: EpisodeDetailViewModel { scope.viewModels.episode(episodeId: episodeId) }

    var body: some View {
        let state = state.value
        Group {
            if let episode = state.episode {
                content(episode, state)
            } else if state.isLoading {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if state.loadFailed {
                ContentUnavailableView {
                    Label(Res.string.shared.error_load_episode.localized(), systemImage: "exclamationmark.triangle")
                } actions: {
                    Button(Res.string.shared.try_again.localized()) { send(EpisodeDetailIntentRetry.shared) }
                }
            } else {
                ContentUnavailableView(Res.string.shared.episode_not_found.localized(), systemImage: "waveform")
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbar(state.episode) }
        .messages(from: viewModel.messages)
        .sheet(item: $reporting) { target in
            ReportSheet(target: target) {
                SwiftSafetyKt.hideEpisode(episodeId: episodeId)
                model.root.onBackClicked()
            }
        }
    }

    @ToolbarContentBuilder
    private func toolbar(_ episode: Episode?) -> some ToolbarContent {
        if let episode {
            ToolbarItem(placement: .topBarTrailing) {
                // A private feed's audio is never shared (22.6): the text leaves it out.
                ShareLink(item: ShareTextKt.episodeShareText(episode: episode, startsAt: nil, positionMs: nil)) {
                    Label(Res.string.shared.share.localized(), systemImage: "square.and.arrow.up")
                }
            }
            if safety.value.inBrazil {
                ToolbarItem(placement: .topBarTrailing) {
                    // A library podcast's id is its feed address.
                    Button {
                        reporting = ReportTarget(podcastTitle: episode.podcastTitle ?? "", feedUrl: episode.podcastId, episodeTitle: episode.title)
                    } label: {
                        Label(Res.string.shared.report.localized(), systemImage: "flag")
                    }
                }
            }
        }
    }

    private static let artworkSize: CGFloat = 200

    private func content(_ episode: Episode, _ state: EpisodeDetailUiState) -> some View {
        ScrollView {
            VStack(spacing: Spacing.m) {
                Artwork(url: episode.imageUrl, size: Self.artworkSize, radius: Radius.extraLarge)
                VStack(spacing: Spacing.xs) {
                    if let podcast = episode.podcastTitle {
                        Text(podcast).textStyle(Typography.labelLarge).foregroundStyle(Color(.Theme.accentText))
                    }
                    Text(episode.title).textStyle(Typography.titleLarge).multilineTextAlignment(.center)
                        .accessibilityAddTraits(.isHeader)
                    if episode.explicit_ { ExplicitMark() }
                    if !facts(episode).isEmpty {
                        Text(facts(episode)).textStyle(Typography.labelMedium).foregroundStyle(.secondary)
                    }
                }
                Button { send(EpisodeDetailIntentPlayPause.shared) } label: {
                    Label(playLabel(episode, state), systemImage: state.isPlaying ? "pause.fill" : "play.fill")
                        .frame(maxWidth: Sizes.readableWidth / 2)
                }
                .buttonStyle(.borderedProminent)
                .disabled(state.isBuffering)
                actions(episode, state)
                VStack(alignment: .leading, spacing: Spacing.s) {
                    SectionTitle(Res.string.shared.description_.localized())
                    HtmlText(html: episode.description_ ?? Res.string.shared.no_description.localized(), collapsedLines: .max)
                }
            }
            .padding(Spacing.l)
            .padding(.bottom, Sizes.listBottomInset)
            .frame(maxWidth: Sizes.readableWidth)
            .frame(maxWidth: .infinity)
        }
    }

    /// Queue, download, played and saved, as icon buttons with their names for VoiceOver.
    private func actions(_ episode: Episode, _ state: EpisodeDetailUiState) -> some View {
        let download = EpisodeDownload(state.downloadStatus, isDownloaded: episode.isDownloaded)
        return HStack(spacing: Spacing.l) {
            iconButton(Res.string.shared.play_next, "text.line.first.and.arrowtriangle.forward") {
                send(EpisodeDetailIntentEnqueue(next: true))
            }
            switch download {
            case .idle, .failed: iconButton(Res.string.shared.download_cd, "arrow.down.circle") { send(EpisodeDetailIntentDownload.shared) }
            case .queued, .downloading: iconButton(Res.string.shared.cancel_download, "xmark.circle") { send(EpisodeDetailIntentCancelDownload.shared) }
            case .downloaded: iconButton(Res.string.shared.delete_download, "trash") { send(EpisodeDetailIntentDeleteDownload.shared) }
            }
            if episode.isPlayed {
                iconButton(Res.string.shared.mark_as_unplayed, "circle") { send(EpisodeDetailIntentMarkUnplayed.shared) }
            } else {
                iconButton(Res.string.shared.mark_as_played, "checkmark.circle") { send(EpisodeDetailIntentMarkPlayed.shared) }
            }
            iconButton(episode.isSaved ? Res.string.shared.unsave_episode : Res.string.shared.save_episode,
                       episode.isSaved ? "bookmark.fill" : "bookmark") {
                send(EpisodeDetailIntentSetSaved(saved: !episode.isSaved))
            }
        }
        .font(.title2)
    }

    private func iconButton(_ label: LibraryStringResource, _ symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) { Image(systemName: symbol).frame(width: Sizes.touchTarget, height: Sizes.touchTarget) }
            .accessibilityLabel(label.localized())
    }

    /// "Play" for a new episode, "Resume · 42min left" for a started one, "Pause" while it plays.
    private func playLabel(_ episode: Episode, _ state: EpisodeDetailUiState) -> String {
        if state.isPlaying { return Res.string.shared.pause.localized() }
        if let left = episode.remainingDuration() { return Res.string.shared.resume_remaining.localized([left]) }
        return Res.string.shared.play.localized()
    }

    /// When it came out and how long it is; what is left goes on the play button.
    private func facts(_ episode: Episode) -> String {
        let length = episode.duration > 0 ? LongExtensionsKt.toDuration(episode.duration) : nil
        return [relativeTime(episode.publishDate), length].compactMap { $0 }.joined(separator: " · ")
    }

    private func send(_ intent: any EpisodeDetailIntent) {
        viewModel.onIntent(intent: intent)
    }
}
