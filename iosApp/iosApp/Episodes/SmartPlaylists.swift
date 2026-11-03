import Shared
import SwiftUI

/// Makes or changes a smart playlist (19.4): its name, a category, the longest episode, unheard only and downloaded
/// only, as the Compose editor (feature/search) has it. "Delete" only for one already saved.
struct SmartPlaylistEditor: View {
    let playlist: SmartPlaylist
    let onSave: (SmartPlaylist) -> Void
    let onDelete: (SmartPlaylist) -> Void
    let onDismiss: () -> Void
    @State private var draft: PlaylistDraft

    init(playlist: SmartPlaylist, onSave: @escaping (SmartPlaylist) -> Void, onDelete: @escaping (SmartPlaylist) -> Void,
         onDismiss: @escaping () -> Void) {
        self.playlist = playlist
        self.onSave = onSave
        self.onDelete = onDelete
        self.onDismiss = onDismiss
        _draft = State(initialValue: PlaylistDraft(playlist))
    }

    var body: some View {
        let strings = Res.string.shared
        NavigationStack {
            Form {
                TextField(strings.smart_playlist_name.localized(), text: $draft.name)
                    .accessibilityLabel(strings.smart_playlist_name.localized())
                // Chip rows, as the Compose editor has them.
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Spacing.s) {
                        Chip(label: strings.smart_playlist_any_category.localized(), isOn: draft.category == nil) { draft.category = nil }
                        ForEach(DirectoryCategories.sorted(), id: \.self) { category in
                            Chip(label: category.localized(), isOn: category == draft.category) { draft.category = category }
                        }
                    }
                }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Spacing.s) {
                        ForEach(PlaylistDraft.lengths, id: \.self) { minutes in
                            Chip(label: PlaylistDraft.lengthLabel(minutes), isOn: minutes == draft.maxMinutes) { draft.maxMinutes = minutes }
                        }
                    }
                }
                Toggle(strings.filter_unplayed.localized(), isOn: $draft.unplayedOnly)
                Toggle(strings.filter_downloaded.localized(), isOn: $draft.downloadedOnly)
                if playlist.id != 0 {
                    Button(strings.delete_.localized(), role: .destructive) { onDelete(playlist) }
                }
            }
            .navigationTitle((playlist.id == 0 ? strings.smart_playlist_new : strings.smart_playlist_edit).localized())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(strings.cancel.localized(), action: onDismiss) }
                ToolbarItem(placement: .confirmationAction) {
                    Button(strings.smart_playlist_save.localized()) { onSave(draft.playlist(id: playlist.id)) }
                        .disabled(!draft.canSave)
                }
            }
        }
    }
}

/// The editor's fields, in Swift types the form can bind to.
struct PlaylistDraft {
    /// No limit, then the shared choices.
    static let lengths: [Int?] = [nil] + SmartPlaylist.companion.LENGTH_CHOICES.map { Int(truncating: $0) }

    var name: String
    var category: PodcastCategory?
    var maxMinutes: Int?
    var unplayedOnly: Bool
    var downloadedOnly: Bool

    init(_ playlist: SmartPlaylist) {
        name = playlist.name
        category = playlist.category
        maxMinutes = playlist.maxMinutes?.intValue
        unplayedOnly = playlist.unplayedOnly
        downloadedOnly = playlist.downloadedOnly
    }

    var canSave: Bool { !name.trimmingCharacters(in: .whitespaces).isEmpty }

    func playlist(id: Int64) -> SmartPlaylist {
        SmartPlaylist(id: id, name: name, category: category, unplayedOnly: unplayedOnly, downloadedOnly: downloadedOnly,
                      maxMinutes: maxMinutes.map { KotlinInt(int: Int32($0)) })
    }

    static func lengthLabel(_ minutes: Int?, bundle: Bundle = .main) -> String {
        guard let minutes else { return Res.string.shared.smart_playlist_any_length.localized(bundle: bundle) }
        return Res.string.shared.smart_playlist_length.localized([KotlinInt(int: Int32(minutes))], bundle: bundle)
    }
}
