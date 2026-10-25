import Shared
import SwiftUI

extension View {
    /// While [count] episodes are selected (18.11): how many and closing the selection at the top, and what to do to
    /// all of them at the bottom, as the Compose selection bar has it.
    func episodeSelection(count: Int, onAction: @escaping (EpisodeAction) -> Void, onClose: @escaping () -> Void) -> some View {
        toolbar {
            if count > 0 {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: onClose) { Image(systemName: "xmark") }
                        .accessibilityLabel(Res.string.shared.close_selection.localized())
                }
                ToolbarItem(placement: .principal) {
                    Text(Res.string.shared.selected_count.localized([KotlinInt(int: Int32(count))])).textStyle(Typography.titleMedium)
                }
                ToolbarItemGroup(placement: .bottomBar) {
                    ForEach(EpisodeAction.entries, id: \.self) { action in
                        Button { onAction(action) } label: {
                            Label(EpisodeSelectionTexts.label(action), systemImage: EpisodeSelectionTexts.symbol(action))
                        }
                        if action != EpisodeAction.entries.last { Spacer() }
                    }
                }
            }
        }
        .toolbar(count > 0 ? .hidden : .automatic, for: .tabBar)
    }
}

enum EpisodeSelectionTexts {
    static func label(_ action: EpisodeAction, bundle: Bundle = .main) -> String {
        let strings = Res.string.shared
        return switch action {
        case .addToQueue: strings.add_to_queue.localized(bundle: bundle)
        case .download: strings.download_cd.localized(bundle: bundle)
        case .markPlayed: strings.mark_as_played.localized(bundle: bundle)
        default: strings.mark_as_unplayed.localized(bundle: bundle)
        }
    }

    static func symbol(_ action: EpisodeAction) -> String {
        switch action {
        case .addToQueue: "text.append"
        case .download: "arrow.down.circle"
        case .markPlayed: "checkmark.circle"
        default: "circle"
        }
    }
}
