import Shared
import SwiftUI

/// A podcast's page (25.5): its cover over a backdrop tinted by it, what it is about, and its episodes a page at a
/// time, filtered and in its own order, as the Compose screen does (feature/podcast).
struct PodcastScreen: View {
    let model: ShellModel
    let podcastId: String
    @State private var scope: ScreenScope
    @State private var state: Watched<PodcastDetailUiState>
    @State private var episodes: Watched<NSArray>
    @State private var playback: Watched<PlayerState>
    @State private var downloads: Watched<NSDictionary>
    @State private var pages: PagedItems<Episode>
    @State private var safety = Watched<ChildSafetyState>(SwiftSafetyKt.childSafety())
    @State private var reporting: ReportTarget?
    @State private var settingsOpen = false

    init(model: ShellModel, podcastId: String) {
        self.model = model
        self.podcastId = podcastId
        let scope = ScreenScope()
        let viewModel = scope.viewModels.podcast(podcastId: podcastId)
        let pages = viewModel.episodePages()
        _scope = State(initialValue: scope)
        _state = State(initialValue: Watched(viewModel.uiState))
        _pages = State(initialValue: pages)
        _episodes = State(initialValue: Watched(pages.items))
        _playback = State(initialValue: Watched(viewModel.rowPlayback()))
        _downloads = State(initialValue: Watched(viewModel.activeDownloads))
    }

    private var viewModel: PodcastDetailViewModel { scope.viewModels.podcast(podcastId: podcastId) }

    var body: some View {
        let state = state.value
        let list = episodes.value as? [Episode] ?? []
        List {
            if let podcast = state.podcast {
                PodcastHeader(podcast: podcast, state: state, onPlay: { send(PodcastDetailIntentPlay(episode: $0)) },
                              onRefresh: { send(PodcastDetailIntentRefresh.shared) },
                              onSimilar: { model.root.onShowSimilar(podcastId: podcastId) })
                    .listRowInsets(EdgeInsets())
                    .listRowSeparator(.hidden)
            }
            filters(state.filter).listRowSeparator(.hidden)
            ForEach(Array(list.enumerated()), id: \.element.id) { index, episode in
                if let season = season(at: index, in: list) {
                    Text(Res.string.shared.season_number.localized([KotlinInt(int: season)]))
                        .textStyle(Typography.titleSmall).foregroundStyle(Color(.Theme.primary))
                        .accessibilityAddTraits(.isHeader)
                        .listRowSeparator(.hidden)
                }
                Button {
                    if state.selection.isEmpty {
                        model.root.onEpisodeSelected(episodeId: episode.id, podcastId: podcastId)
                    } else {
                        send(PodcastDetailIntentToggleSelection(episode: episode))
                    }
                } label: {
                    row(episode, selection: state.selection)
                }
                .buttonStyle(.plain)
                .onAppear { pages.loadAround(index: Int32(index)) }
            }
        }
        .listStyle(.plain)
        .overlay { if state.isLoading { ProgressView() } }
        .navigationTitle(state.podcast?.title ?? "")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbar(state) }
        .refreshable { send(PodcastDetailIntentRefresh.shared) }
        .messages(from: viewModel.messages)
        .episodeSelection(count: state.selection.count, onAction: { send(PodcastDetailIntentActOnSelection(action: $0)) },
                          onClose: { send(PodcastDetailIntentClearSelection.shared) })
        .sheet(isPresented: $settingsOpen) {
            if let podcast = state.podcast { PodcastSettingsSheet(podcast: podcast, send: send) }
        }
        .sheet(item: $reporting) { target in
            ReportSheet(target: target) {
                SwiftSafetyKt.hidePodcast(podcastId: podcastId)
                model.root.onBackClicked()
            }
        }
        .confirmationDialog(olderTitle(state.olderMark), isPresented: isShowing(state.olderMark != nil, dismiss: PodcastDetailIntentDismissMarkOlder.shared),
                            titleVisibility: .visible, presenting: state.olderMark) { mark in
            Button(Res.string.shared.mark.localized()) { send(PodcastDetailIntentConfirmMarkOlder(mark: mark)) }
            Button(Res.string.shared.cancel.localized(), role: .cancel) { send(PodcastDetailIntentDismissMarkOlder.shared) }
        } message: { mark in
            Text((mark.played ? Res.string.shared.mark_older_confirmation : Res.string.shared.mark_older_unplayed_confirmation)
                .localized([mark.episode.title]))
        }
        .confirmationDialog(Res.string.shared.delete_download.localized(),
                            isPresented: isShowing(state.deleteEpisodeConfirmation != nil, dismiss: PodcastDetailIntentDismissDeleteDownload.shared),
                            titleVisibility: .visible, presenting: state.deleteEpisodeConfirmation) { episode in
            Button(Res.string.shared.delete_.localized(), role: .destructive) { send(PodcastDetailIntentConfirmDeleteDownload(episode: episode)) }
            Button(Res.string.shared.cancel.localized(), role: .cancel) { send(PodcastDetailIntentDismissDeleteDownload.shared) }
        } message: { episode in
            Text(Res.string.shared.delete_download_confirmation.localized([episode.title]))
        }
    }

    private func row(_ episode: Episode, selection: [Episode]) -> some View {
        let player = playback.value
        let isCurrent = player.currentEpisode?.id == episode.id
        let download = EpisodeDownload((downloads.value as? [String: DownloadStatus])?[episode.id], isDownloaded: episode.isDownloaded)
        return EpisodeRow(
            episode: episode,
            isPlaying: isCurrent && player.isPlaying,
            isLoading: isCurrent && player.isBuffering,
            download: download,
            actions: EpisodeActions(
                play: { send(PodcastDetailIntentPlay(episode: episode)) },
                playNext: { send(PodcastDetailIntentEnqueue(episode: episode, next: true)) },
                addToQueue: { send(PodcastDetailIntentEnqueue(episode: episode, next: false)) },
                markPlayed: episode.isPlayed ? nil : { send(PodcastDetailIntentMarkPlayed(episode: episode)) },
                markUnplayed: episode.isPlayed ? { send(PodcastDetailIntentMarkUnplayed(episode: episode)) } : nil,
                markOlderPlayed: { send(PodcastDetailIntentRequestMarkOlder(mark: OlderMark(episode: episode, played: true))) },
                markOlderUnplayed: { send(PodcastDetailIntentRequestMarkOlder(mark: OlderMark(episode: episode, played: false))) },
                download: { send(PodcastDetailIntentDownload(episode: episode)) },
                cancelDownload: { send(PodcastDetailIntentCancelDownload(episode: episode)) },
                deleteDownload: { send(PodcastDetailIntentRequestDeleteDownload(episode: episode)) },
                select: { send(PodcastDetailIntentToggleSelection(episode: episode)) }
            ),
            selected: selection.isEmpty ? nil : selection.contains { $0.id == episode.id }
        )
    }

    private func filters(_ selected: EpisodeFilter) -> some View {
        Picker(Res.string.shared.library_sort.localized(), selection: Binding(
            get: { selected }, set: { send(PodcastDetailIntentSetFilter(filter: $0)) }
        )) {
            Text(Res.string.shared.filter_all.localized()).tag(EpisodeFilter.all)
            Text(Res.string.shared.filter_unplayed.localized()).tag(EpisodeFilter.unplayed)
            Text(Res.string.shared.filter_downloaded.localized()).tag(EpisodeFilter.downloaded)
        }
        .pickerStyle(.segmented)
        .frame(maxWidth: Sizes.readableWidth)
    }

    @ToolbarContentBuilder
    private func toolbar(_ state: PodcastDetailUiState) -> some ToolbarContent {
        if let podcast = state.podcast {
            ToolbarItem(placement: .topBarTrailing) {
                Button { settingsOpen = true } label: {
                    Label(Res.string.shared.podcast_playback.localized(), systemImage: "slider.horizontal.3")
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                // A private feed's address is never shared (22.6): the text leaves it out.
                ShareLink(item: ShareTextKt.podcastShareText(podcast: podcast)) {
                    Label(Res.string.shared.share.localized(), systemImage: "square.and.arrow.up")
                }
            }
            // Reporting is a duty under Brazil's ECA Digital (26.7): offered there only.
            if safety.value.inBrazil {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { reporting = ReportTarget(podcastTitle: podcast.title, feedUrl: podcast.feedUrl, episodeTitle: nil) } label: {
                        Label(Res.string.shared.report.localized(), systemImage: "flag")
                    }
                }
            }
        }
        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                Picker(Res.string.shared.library_sort.localized(), selection: Binding(
                    get: { state.oldestFirst }, set: { send(PodcastDetailIntentSetOldestFirst(oldestFirst: $0)) }
                )) {
                    Text(Res.string.shared.episodes_newest_first.localized()).tag(false)
                    Text(Res.string.shared.episodes_oldest_first.localized()).tag(true)
                }
            } label: {
                Label(Res.string.shared.library_sort.localized(), systemImage: "arrow.up.arrow.down")
            }
        }
    }

    /// The season whose header goes above the episode at [index], by the same rule as the Compose list (18.10).
    private func season(at index: Int, in list: [Episode]) -> Int32? {
        SeasonHeadersKt.seasonStartingAt(index: Int32(index)) { at in list[Int(truncating: at)].season }?.int32Value
    }

    private func olderTitle(_ mark: OlderMark?) -> String {
        (mark?.played == false ? Res.string.shared.mark_older_as_unplayed : Res.string.shared.mark_older_as_played).localized()
    }

    private func isShowing(_ shown: Bool, dismiss: any PodcastDetailIntent) -> Binding<Bool> {
        Binding(get: { shown }, set: { if !$0 { send(dismiss) } })
    }

    private func send(_ intent: any PodcastDetailIntent) {
        viewModel.onIntent(intent: intent)
    }
}

/// The cover over a backdrop tinted by it (24.2), the name, "author · 312 episodes", "Latest" and refresh, and the
/// description.
private struct PodcastHeader: View {
    let podcast: Podcast
    let state: PodcastDetailUiState
    let onPlay: (Episode) -> Void
    let onRefresh: () -> Void
    let onSimilar: () -> Void

    private static let artworkSize: CGFloat = 168

    var body: some View {
        VStack(spacing: Spacing.m) {
            Artwork(url: podcast.imageUrl, size: Self.artworkSize, radius: Radius.extraLarge)
            VStack(spacing: Spacing.xs) {
                Text(podcast.title).textStyle(Typography.titleLarge).multilineTextAlignment(.center)
                    .accessibilityAddTraits(.isHeader)
                if podcast.explicit_ { ExplicitMark() }
                if !byline.isEmpty {
                    Text(byline).textStyle(Typography.labelLarge).foregroundStyle(.secondary).multilineTextAlignment(.center)
                }
            }
            HStack(spacing: Spacing.s) {
                if let latest = state.latestUnplayed {
                    Button { onPlay(latest) } label: {
                        Label(Res.string.shared.play_latest.localized(), systemImage: "play.fill")
                    }
                    .buttonStyle(.borderedProminent)
                }
                Button(action: onRefresh) {
                    Label(Res.string.shared.refresh.localized(), systemImage: "arrow.clockwise").labelStyle(.iconOnly)
                }
                .buttonStyle(.bordered)
                Button(action: onSimilar) {
                    Label(Res.string.shared.similar_button.localized(), systemImage: "safari").labelStyle(.iconOnly)
                }
                .buttonStyle(.bordered)
            }
            HtmlText(html: podcast.description_)
        }
        .padding(Spacing.l)
        .frame(maxWidth: Sizes.readableWidth)
        .frame(maxWidth: .infinity)
        .background {
            Artwork(url: podcast.imageUrl, size: nil, radius: 0).blur(radius: Spacing.xxxl).opacity(Opacity.scrim)
                .clipped()
        }
    }

    private var byline: String {
        let count = state.episodeCount > 0
            ? Res.plurals.shared.podcast_episode_count.localized(count: Int(state.episodeCount)) : nil
        return [podcast.author, count].compactMap { $0 }.joined(separator: " · ")
    }
}
