import Shared
import SwiftUI

/// What is playing, over the tabs; tapping it opens the player. It follows the player's whole state, which moves
/// every 500 ms, on its own, so nothing else redraws with it (23.2).
struct MiniPlayerBar: View {
    let model: ShellModel
    @State private var player: Watched<PlayerState>

    init(model: ShellModel) {
        self.model = model
        _player = State(initialValue: Watched(model.app.viewModels.player().playerState))
    }

    var body: some View {
        let state = player.value
        HStack(spacing: Spacing.m) {
            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text(state.currentEpisode?.title ?? "").textStyle(Typography.titleMedium).lineLimit(1)
                Text(state.currentEpisode?.podcastTitle ?? "").textStyle(Typography.bodyMedium).lineLimit(1)
                    .foregroundStyle(Color(.Theme.onSurfaceVariant))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                model.app.viewModels.player().onIntent(intent: PlayerIntentPlayPause.shared)
            } label: {
                Image(systemName: state.isPlaying ? "pause.fill" : "play.fill")
                    .frame(width: Sizes.touchTarget, height: Sizes.touchTarget)
            }
            .accessibilityLabel(state.isPlaying ? Res.string.shared.pause.localized() : Res.string.shared.play.localized())
        }
        .padding(.horizontal, Spacing.l)
        .contentShape(Rectangle())
        .onTapGesture { model.root.onPlayerClicked() }
        .accessibilityIdentifier("miniPlayer")
    }
}
