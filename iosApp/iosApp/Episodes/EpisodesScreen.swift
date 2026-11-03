import Shared
import SwiftUI

/// The Episodes tab (25.6): every episode of the library, searched with the system's search field, filtered (New,
/// All, In progress, Saved, Downloaded, History) and grouped by date, as the Compose screen does (feature/search).
/// Opened without network it starts on Downloaded, which the view model chooses (24.15).
struct EpisodesScreen: View {
    let model: ShellModel
    private let viewModel: SearchViewModel
    @State private var state: Watched<SearchUiState>
    @State private var pages: PagedItems<Episode>
    @State private var episodes: Watched<NSArray>
    @State private var playback: Watched<PlayerState>
    @State private var downloads: Watched<NSDictionary>

    init(model: ShellModel) {
        self.model = model
        viewModel = model.app.viewModels.search()
        let pages = viewModel.resultPages()
        _pages = State(initialValue: pages)
        _state = State(initialValue: Watched(viewModel.uiState))
        _episodes = State(initialValue: Watched(pages.items))
        _playback = State(initialValue: Watched(viewModel.rowPlayback()))
        _downloads = State(initialValue: Watched(viewModel.activeDownloads))
    }

    var body: some View {
        let state = state.value
        let list = episodes.value as? [Episode] ?? []
        List {
            filters(state).listRowSeparator(.hidden)
            listAction(state, isEmpty: list.isEmpty).listRowSeparator(.hidden)
            ForEach(sections(list, filter: state.filter), id: \.group) { section in
                Section {
                    ForEach(section.rows, id: \.episode.id) { row in
                        Button {
                            if state.selection.isEmpty {
                                model.root.onEpisodeSelected(episodeId: row.episode.id, podcastId: row.episode.podcastId)
                            } else {
                                send(SearchIntentToggleSelection(episode: row.episode))
                            }
                        } label: {
                            episodeRow(row.episode, selection: state.selection)
                        }
                        .buttonStyle(.plain)
                        .onAppear { pages.loadAround(index: Int32(row.index)) }
                    }
                } header: {
                    SectionTitle(section.group.localized)
                }
            }
        }
        .listStyle(.plain)
        .overlay { if list.isEmpty { empty(state) } }
        .sheet(isPresented: isShowing(state.editingPlaylist != nil, dismiss: SearchIntentDismissPlaylistEditor.shared)) {
            if let playlist = state.editingPlaylist {
                SmartPlaylistEditor(playlist: playlist, onSave: { send(SearchIntentSavePlaylist(playlist: $0)) },
                                    onDelete: { send(SearchIntentDeletePlaylist(playlist: $0)) },
                                    onDismiss: { send(SearchIntentDismissPlaylistEditor.shared) })
            }
        }
        .navigationTitle(Res.string.shared.episodes_tab.localized())
        // Always shown: by default it hides until the list is pulled down, and an empty list cannot be pulled.
        .searchable(text: Binding(get: { state.searchQuery }, set: { send(SearchIntentChangeQuery(query: $0)) }),
                    placement: .navigationBarDrawer(displayMode: .always),
                    prompt: Res.string.shared.search_placeholder.localized())
        .refreshable { send(SearchIntentRefresh.shared) }
        .messages(from: viewModel.messages)
        .episodeSelection(count: state.selection.count, onAction: { send(SearchIntentActOnSelection(action: $0)) },
                          onClose: { send(SearchIntentClearSelection.shared) })
        .confirmationDialog(Res.string.shared.clear_history.localized(),
                            isPresented: isShowing(state.clearHistoryConfirmation, dismiss: SearchIntentDismissClearHistory.shared),
                            titleVisibility: .visible) {
            Button(Res.string.shared.clear.localized(), role: .destructive) { send(SearchIntentConfirmClearHistory.shared) }
            Button(Res.string.shared.cancel.localized(), role: .cancel) { send(SearchIntentDismissClearHistory.shared) }
        } message: {
            Text(Res.string.shared.clear_history_confirmation.localized())
        }
        .confirmationDialog(olderTitle(state.olderMark), isPresented: isShowing(state.olderMark != nil, dismiss: SearchIntentDismissMarkOlder.shared),
                            titleVisibility: .visible, presenting: state.olderMark) { mark in
            Button(Res.string.shared.mark.localized()) { send(SearchIntentConfirmMarkOlder(mark: mark)) }
            Button(Res.string.shared.cancel.localized(), role: .cancel) { send(SearchIntentDismissMarkOlder.shared) }
        } message: { mark in
            Text((mark.played ? Res.string.shared.mark_older_confirmation : Res.string.shared.mark_older_unplayed_confirmation)
                .localized([mark.episode.title]))
        }
        .confirmationDialog(Res.string.shared.delete_download.localized(),
                            isPresented: isShowing(state.deleteEpisodeConfirmation != nil, dismiss: SearchIntentDismissDeleteDownload.shared),
                            titleVisibility: .visible, presenting: state.deleteEpisodeConfirmation) { episode in
            Button(Res.string.shared.delete_.localized(), role: .destructive) { send(SearchIntentConfirmDeleteDownload(episode: episode)) }
            Button(Res.string.shared.cancel.localized(), role: .cancel) { send(SearchIntentDismissDeleteDownload.shared) }
        } message: { episode in
            Text(Res.string.shared.delete_download_confirmation.localized([episode.title]))
        }
    }

    /// The fixed filters, then the smart playlists (19.4), then "New playlist", which opens the editor.
    private func filters(_ state: SearchUiState) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.s) {
                ForEach(EpisodeListFilter.entries, id: \.self) { filter in
                    Chip(label: filter.label, isOn: state.playlist == nil && filter == state.filter) {
                        send(SearchIntentChangeFilter(filter: filter))
                    }
                }
                ForEach(state.playlists, id: \.id) { playlist in
                    Chip(label: playlist.name, isOn: playlist.id == state.playlist?.id) { send(SearchIntentSelectPlaylist(playlist: playlist)) }
                }
                Chip(label: "+ " + Res.string.shared.smart_playlist_new.localized(), isOn: false) {
                    send(SearchIntentEditPlaylist(playlist: nil))
                }
            }
        }
    }

    /// "Edit" and "Play all" above a smart playlist; "Dismiss all" above New, "Clear history" above History, and the
    /// space the downloads take above Downloaded.
    @ViewBuilder
    private func listAction(_ state: SearchUiState, isEmpty: Bool) -> some View {
        if let playlist = state.playlist {
            if !isEmpty {
                HStack {
                    Button { send(SearchIntentEditPlaylist(playlist: playlist)) } label: {
                        Label(Res.string.shared.smart_playlist_edit.localized(), systemImage: "pencil")
                    }
                    Spacer()
                    Button { send(SearchIntentPlayPlaylist.shared) } label: {
                        Label(Res.string.shared.smart_playlist_play_all.localized(), systemImage: "play.circle")
                    }
                }
                .buttonStyle(.borderless)
            }
        } else if !isEmpty {
            switch state.filter {
            case .theNew:
                Button(Res.string.shared.dismiss_all_new.localized()) { send(SearchIntentDismissNew(episode: nil)) }
                    .frame(maxWidth: .infinity, alignment: .trailing)
            case .history:
                Button(Res.string.shared.clear_history.localized()) { send(SearchIntentRequestClearHistory.shared) }
                    .frame(maxWidth: .infinity, alignment: .trailing)
            case .downloaded:
                Text(Res.string.shared.downloads_storage_used.localized([
                    ByteCountFormatter.string(fromByteCount: state.usedBytes, countStyle: .file),
                ]))
                .textStyle(Typography.bodySmall).foregroundStyle(Color(.Theme.onSurfaceVariant))
            default:
                EmptyView()
            }
        }
    }

    private func episodeRow(_ episode: Episode, selection: [Episode]) -> some View {
        let player = playback.value
        let isCurrent = player.currentEpisode?.id == episode.id
        return EpisodeRow(
            episode: episode,
            podcastTitle: episode.podcastTitle,
            isPlaying: isCurrent && player.isPlaying,
            isLoading: isCurrent && player.isBuffering,
            download: EpisodeDownload((downloads.value as? [String: DownloadStatus])?[episode.id], isDownloaded: episode.isDownloaded),
            actions: EpisodeActions(
                play: { send(SearchIntentPlay(episode: episode)) },
                playNext: { send(SearchIntentEnqueue(episode: episode, next: true)) },
                addToQueue: { send(SearchIntentEnqueue(episode: episode, next: false)) },
                markPlayed: episode.isPlayed ? nil : { send(SearchIntentSetPlayed(episode: episode, played: true)) },
                markUnplayed: episode.isPlayed ? { send(SearchIntentSetPlayed(episode: episode, played: false)) } : nil,
                markOlderPlayed: { send(SearchIntentRequestMarkOlder(mark: OlderMark(episode: episode, played: true))) },
                markOlderUnplayed: { send(SearchIntentRequestMarkOlder(mark: OlderMark(episode: episode, played: false))) },
                download: { send(SearchIntentDownload(episode: episode)) },
                cancelDownload: { send(SearchIntentCancelDownload(episode: episode)) },
                deleteDownload: { send(SearchIntentRequestDeleteDownload(episode: episode)) },
                dismissNew: episode.isNew ? { send(SearchIntentDismissNew(episode: episode)) } : nil,
                goToPodcast: { model.root.onPodcastSelected(podcastId: episode.podcastId) },
                select: { send(SearchIntentToggleSelection(episode: episode)) }
            ),
            selected: selection.isEmpty ? nil : selection.contains { $0.id == episode.id }
        )
    }

    @ViewBuilder
    private func empty(_ state: SearchUiState) -> some View {
        if let playlist = state.playlist {
            ContentUnavailableView {
                Label(Res.string.shared.smart_playlist_empty_title.localized(), systemImage: "music.note.list")
            } description: {
                Text(Res.string.shared.smart_playlist_empty_message.localized())
                    .foregroundStyle(Color(.Theme.onSurfaceVariant))
            } actions: {
                Button(Res.string.shared.smart_playlist_edit.localized()) { send(SearchIntentEditPlaylist(playlist: playlist)) }
            }
        } else {
            let (title, message, symbol) = EpisodesEmpty.of(filter: state.filter, searching: !state.searchQuery.isEmpty)
            ContentUnavailableView(title.localized(), systemImage: symbol,
                                   description: Text(message.localized())
                                       .foregroundStyle(Color(.Theme.onSurfaceVariant)))
        }
    }

    private func olderTitle(_ mark: OlderMark?) -> String {
        (mark?.played == false ? Res.string.shared.mark_older_as_unplayed : Res.string.shared.mark_older_as_played).localized()
    }

    private func isShowing(_ shown: Bool, dismiss: any SearchIntent) -> Binding<Bool> {
        Binding(get: { shown }, set: { if !$0 { send(dismiss) } })
    }

    private func send(_ intent: any SearchIntent) {
        viewModel.onIntent(intent: intent)
    }
}

/// The episodes under date headers: History by when each last played, the rest by when it came out. Each row keeps its
/// place in the whole list, which is what the pages count.
struct EpisodeSection {
    struct Row {
        let index: Int
        let episode: Episode
    }

    let group: DateGroup
    var rows: [Row]
}

func sections(_ episodes: [Episode], filter: EpisodeListFilter) -> [EpisodeSection] {
    var result: [EpisodeSection] = []
    for (index, episode) in episodes.enumerated() {
        let date = filter == .history ? (episode.lastPlayedAt?.int64Value ?? episode.publishDate) : episode.publishDate
        let group = SwiftListsKt.dateGroupNow(timestampMs: date)
        if result.last?.group == group {
            result[result.count - 1].rows.append(.init(index: index, episode: episode))
        } else {
            result.append(EpisodeSection(group: group, rows: [.init(index: index, episode: episode)]))
        }
    }
    return result
}

/// What an empty list says, by filter; a search with no match says so whatever the filter.
enum EpisodesEmpty {
    static func of(filter: EpisodeListFilter, searching: Bool) -> (LibraryStringResource, LibraryStringResource, String) {
        let strings = Res.string.shared
        if searching || filter == .all { return (strings.search_empty_title, strings.search_empty_message, "magnifyingglass") }
        switch filter {
        case .theNew: return (strings.new_empty_title, strings.new_empty_message, "tray")
        case .history: return (strings.history_empty_title, strings.history_empty_message, "clock.arrow.circlepath")
        case .saved: return (strings.saved_empty_title, strings.saved_empty_message, "bookmark")
        case .inProgress: return (strings.in_progress_empty_title, strings.in_progress_empty_message, "timelapse")
        default: return (strings.no_downloads, strings.downloads_empty_message, "arrow.down.circle")
        }
    }
}

private extension EpisodeListFilter {
    var label: String {
        let strings = Res.string.shared
        return switch self {
        case .theNew: strings.filter_new.localized()
        case .inProgress: strings.filter_in_progress.localized()
        case .saved: strings.filter_saved.localized()
        case .downloaded: strings.filter_downloaded.localized()
        case .history: strings.filter_history.localized()
        default: strings.filter_all.localized()
        }
    }
}

extension DateGroup {
    var localized: String {
        let strings = Res.string.shared
        return switch self {
        case .last24Hours: strings.date_group_last_24_hours.localized()
        case .last7Days: strings.date_group_last_7_days.localized()
        case .last30Days: strings.date_group_last_30_days.localized()
        default: strings.date_group_older.localized()
        }
    }
}
