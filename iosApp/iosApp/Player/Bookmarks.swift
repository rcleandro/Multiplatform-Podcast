import Shared
import SwiftUI

/// A bookmark (19.3): its moment and note; a tap plays from it, the trash deletes it. On the player and on the
/// episode page, as the Compose `BookmarkRow`.
struct BookmarkRow: View {
    let bookmark: Bookmark
    let onSelect: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: Spacing.m) {
            Button(action: onSelect) {
                HStack(spacing: Spacing.m) {
                    Text(LongExtensionsKt.toTime(bookmark.positionMs))
                        .textStyle(Typography.labelLarge).foregroundStyle(.secondary)
                    Text(bookmark.note).textStyle(Typography.bodyLarge)
                    Spacer(minLength: 0)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            Button(action: onDelete) { Image(systemName: "trash") }
                .buttonStyle(.borderless)
                .accessibilityLabel(Res.string.shared.bookmark_delete.localized())
        }
    }
}

/// Marks the moment the episode is at, with an optional note, and lists its bookmarks; picking one jumps to it. Stays
/// open after marking, so the new bookmark shows in the list, as the Compose dialog does.
struct BookmarksSheet: View {
    let viewModel: PlayerViewModel
    let positionMs: Int64
    @State private var bookmarks: Watched<NSArray>
    @State private var note = ""
    @Environment(\.dismiss) private var dismiss

    init(viewModel: PlayerViewModel, positionMs: Int64) {
        self.viewModel = viewModel
        self.positionMs = positionMs
        _bookmarks = State(initialValue: Watched(viewModel.bookmarks))
    }

    var body: some View {
        let strings = Res.string.shared
        NavigationStack {
            List {
                Section {
                    TextField(strings.bookmark_note.localized(), text: $note)
                    Button(strings.bookmark_add.localized([LongExtensionsKt.toTime(positionMs)])) {
                        viewModel.onIntent(intent: PlayerIntentAddBookmark(note: note))
                        note = ""
                    }
                }
                ForEach(bookmarks.value as? [Bookmark] ?? [], id: \.id) { bookmark in
                    BookmarkRow(bookmark: bookmark, onSelect: {
                        viewModel.onIntent(intent: PlayerIntentSeekTo(positionMs: bookmark.positionMs))
                        dismiss()
                    }, onDelete: {
                        viewModel.onIntent(intent: PlayerIntentDeleteBookmark(id: bookmark.id))
                    })
                }
            }
            .navigationTitle(strings.bookmarks.localized())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button(strings.close.localized()) { dismiss() } }
            }
        }
    }
}

/// A chapter's title, or when it starts if it has none, as the Compose `chapterLabel`.
func chapterLabel(_ chapter: Chapter) -> String {
    chapter.title.trimmingCharacters(in: .whitespaces).isEmpty ? LongExtensionsKt.toTime(chapter.startMs) : chapter.title
}
