import Shared
import SwiftUI

extension View {
    /// The one-off messages a view model sends (a failed refresh, a podcast deleted), shown for a moment at the bottom,
    /// where the Compose screens show their snackbar. VoiceOver reads each one as it comes.
    func messages(from flow: any Kotlinx_coroutines_coreFlow) -> some View {
        modifier(MessageBanner(flow: flow))
    }
}

private struct MessageBanner: ViewModifier {
    let flow: any Kotlinx_coroutines_coreFlow
    @State private var text: String?
    @State private var watching: Watching?
    @State private var hide: Task<Void, Never>?

    private static let shownFor: Duration = .seconds(4)

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if let text {
                    Text(text)
                        .textStyle(Typography.bodyMedium)
                        .foregroundStyle(Color(.Theme.inverseOnSurface))
                        .padding(Spacing.l)
                        .frame(maxWidth: Sizes.readableWidth, alignment: .leading)
                        .background(Color(.Theme.inverseSurface), in: RoundedRectangle(cornerRadius: Radius.small))
                        .padding(Spacing.l)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .animation(.default, value: text)
            .onAppear {
                watching = SwiftBridgeKt.watch(flow) { message in
                    guard let message = message as? UiMessage else { return }
                    show(message.localized)
                }
            }
            .onDisappear { watching?.cancel() }
    }

    private func show(_ message: String) {
        text = message
        UIAccessibility.post(notification: .announcement, argument: message)
        hide?.cancel()
        hide = Task {
            try? await Task.sleep(for: Self.shownFor)
            if !Task.isCancelled { text = nil }
        }
    }
}
