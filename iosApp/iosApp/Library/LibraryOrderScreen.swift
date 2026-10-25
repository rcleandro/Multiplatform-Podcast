import Shared
import SwiftUI

/// "Organize library" (25.4): the podcasts in their custom order, always a list, dragged by the system's own handles,
/// or moved up and down with VoiceOver. Each move is saved, so closing the screen loses nothing (ADR 0007).
struct LibraryOrderScreen: View {
    let model: ShellModel
    @State private var scope: ScreenScope
    @State private var podcasts: Watched<NSArray>
    /// Shown as moved at once; the database's order replaces it when it comes back.
    @State private var shown: [LibraryEntry] = []

    init(model: ShellModel) {
        self.model = model
        let scope = ScreenScope()
        _scope = State(initialValue: scope)
        _podcasts = State(initialValue: Watched(scope.viewModels.libraryOrder().podcasts))
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(Array(shown.enumerated()), id: \.element.podcast.id) { index, entry in
                        PodcastRow(entry: entry)
                            .accessibilityActions {
                                if index > 0 {
                                    Button(Res.string.shared.move_up.localized()) { move(index, by: -1) }
                                }
                                if index < shown.count - 1 {
                                    Button(Res.string.shared.move_down.localized()) { move(index, by: 1) }
                                }
                            }
                    }
                    .onMove { from, to in
                        shown.move(fromOffsets: from, toOffset: to)
                        save()
                    }
                } header: {
                    Text(Res.string.shared.library_organize_hint.localized()).textCase(nil)
                }
            }
            .environment(\.editMode, .constant(.active))
            .navigationTitle(Res.string.shared.library_organize_title.localized())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(Res.string.shared.close.localized()) { model.root.onBackClicked() }
                }
            }
        }
        .onAppear { shown = entries }
        .onChange(of: podcasts.value) { _, _ in shown = entries }
    }

    private var entries: [LibraryEntry] { podcasts.value as? [LibraryEntry] ?? [] }

    private func move(_ index: Int, by offset: Int) {
        guard let ids = LibraryOrder.moving(shown.map(\.podcast.id), at: index, by: offset) else { return }
        shown = ids.compactMap { id in shown.first { $0.podcast.id == id } }
        save()
    }

    private func save() {
        scope.viewModels.libraryOrder().reorder(podcastIds: shown.map(\.podcast.id))
    }
}

enum LibraryOrder {
    /// [ids] with the one at [index] moved [offset] places, or nil when that leaves the list.
    static func moving(_ ids: [String], at index: Int, by offset: Int) -> [String]? {
        let target = index + offset
        guard ids.indices.contains(index), ids.indices.contains(target) else { return nil }
        var moved = ids
        moved.swapAt(index, target)
        return moved
    }
}
