import Shared
import SwiftUI

/// The Desktop app's keys (21.7) on an iPad or iPhone with a keyboard (25.9): space plays or pauses, the arrows skip
/// back and forward, Escape goes back. A text field with the focus keeps its own keys.
struct KeyboardShortcuts: View {
    let model: ShellModel

    var body: some View {
        Group {
            key(.space) { send(PlayerIntentPlayPause.shared) }
            key(.leftArrow) { send(PlayerIntentSkipBackward.shared) }
            key(.rightArrow) { send(PlayerIntentSkipForward.shared) }
            key(.escape) { model.root.onBackClicked() }
        }
        .frame(width: 0, height: 0)
        .opacity(0)
        .accessibilityHidden(true)
    }

    private func key(_ key: KeyEquivalent, action: @escaping () -> Void) -> some View {
        Button(action: action) { EmptyView() }.keyboardShortcut(key, modifiers: [])
    }

    private func send(_ intent: any PlayerIntent) {
        model.app.viewModels.player().onIntent(intent: intent)
    }
}
