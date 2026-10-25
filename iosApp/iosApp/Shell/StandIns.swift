import Shared
import SwiftUI

// Stand-ins for the screens 25.4 to 25.7 write; each goes when its screen comes. They show only data and symbols,
// so they need no texts of their own.

struct EpisodesStandIn: View {
    var body: some View {
        StandIn(symbol: "list.bullet.rectangle.portrait")
            .navigationTitle(Res.string.shared.episodes_tab.localized())
    }
}

struct PlayerStandIn: View {
    let model: ShellModel

    var body: some View {
        NavigationStack {
            StandIn(symbol: "play.circle")
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button { model.root.onBackClicked() } label: { Image(systemName: "chevron.down") }
                            .accessibilityLabel(Res.string.shared.minimize.localized())
                    }
                }
        }
    }
}

struct StandIn: View {
    let symbol: String
    var detail: String?

    var body: some View {
        VStack(spacing: Spacing.s) {
            Image(systemName: symbol).font(.largeTitle)
            if let detail { Text(detail).textStyle(Typography.bodySmall).foregroundStyle(.secondary) }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
