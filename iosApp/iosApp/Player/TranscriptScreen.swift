import Shared
import SwiftUI

/// The episode's transcript (25.8e, 19.2), over the player: the line being said highlighted and kept in view; scrolling
/// by hand stops that, and "Back to the current line" brings it back. A search keeps only the lines with its words,
/// each with its time. A tap plays from the line. As the Compose screen does (feature/player).
struct TranscriptScreen: View {
    let model: ShellModel
    @State private var scope: ScreenScope
    @State private var state: Watched<TranscriptUiState>
    @State private var following = true

    init(model: ShellModel) {
        self.model = model
        let scope = ScreenScope()
        _scope = State(initialValue: scope)
        _state = State(initialValue: Watched(scope.viewModels.transcript().uiState))
    }

    var body: some View {
        let state = state.value
        let searching = TranscriptRows.isSearching(state.query)
        NavigationStack {
            content(state, searching: searching)
                .navigationTitle(Res.string.shared.transcript.localized())
                .navigationBarTitleDisplayMode(.inline)
                .searchable(text: Binding(get: { state.query }, set: { send(TranscriptIntentChangeQuery(query: $0)) }),
                            placement: .navigationBarDrawer(displayMode: .always),
                            prompt: Res.string.shared.transcript_search.localized())
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button(Res.string.shared.close.localized()) { model.root.onBackClicked() }
                    }
                }
        }
    }

    @ViewBuilder
    private func content(_ state: TranscriptUiState, searching: Bool) -> some View {
        if state.isLoading {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if state.failed {
            ContentUnavailableView {
                Label(Res.string.shared.transcript_failed.localized(), systemImage: "icloud.slash")
            } actions: {
                Button(Res.string.shared.try_again.localized()) { send(TranscriptIntentRetry.shared) }
            }
        } else if searching && state.shown.isEmpty {
            ContentUnavailableView(Res.string.shared.transcript_no_match.localized([state.query.trimmingCharacters(in: .whitespacesAndNewlines)]),
                                   systemImage: "magnifyingglass")
        } else {
            lines(state, searching: searching)
        }
    }

    private func lines(_ state: TranscriptUiState, searching: Bool) -> some View {
        let current = Int(state.current)
        return ScrollViewReader { proxy in
            List(state.shown.map { Int(truncating: $0) }, id: \.self) { index in
                let line = state.lines[index]
                let isCurrent = index == current && !searching
                Button { send(TranscriptIntentPlayFrom(line: line)) } label: {
                    VStack(alignment: .leading, spacing: Spacing.xs) {
                        if let label = TranscriptRows.label(line, searching: searching) {
                            Text(label).textStyle(Typography.labelMedium).foregroundStyle(Color(.Theme.accentText))
                        }
                        Text(line.text).textStyle(Typography.bodyLarge)
                            .foregroundStyle(isCurrent ? Color(.Theme.onSecondaryContainer) : .primary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .listRowBackground(isCurrent ? Color(.Theme.secondaryContainer) : nil)
                .accessibilityAddTraits(isCurrent ? .isSelected : [])
            }
            .listStyle(.plain)
            .onScrollPhaseChange { _, phase in if phase == .interacting { following = false } }
            .onChange(of: current, initial: true) { _, line in
                if following && !searching && line >= 0 { withAnimation { proxy.scrollTo(line, anchor: .center) } }
            }
            .overlay(alignment: .bottom) {
                if !following && !searching && current >= 0 {
                    Button {
                        following = true
                        withAnimation { proxy.scrollTo(current, anchor: .center) }
                    } label: {
                        Label(Res.string.shared.transcript_follow.localized(), systemImage: "location")
                    }
                    .buttonStyle(.borderedProminent)
                    .padding(Spacing.l)
                }
            }
        }
    }

    private func send(_ intent: any TranscriptIntent) {
        scope.viewModels.transcript().onIntent(intent: intent)
    }
}

/// What goes above a line: its time while searching, and who says it when the transcript names them.
enum TranscriptRows {
    static func isSearching(_ query: String) -> Bool {
        !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    static func label(_ line: TranscriptLine, searching: Bool) -> String? {
        let parts = [searching ? LongExtensionsKt.toTime(line.startMs) : nil, line.speaker].compactMap { $0 }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }
}
