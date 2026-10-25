import Shared
import SwiftUI

// Stand-ins for the screens 25.8 still writes (the transcript); each goes when its screen comes. They show only data and symbols,
// so they need no texts of their own.

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
