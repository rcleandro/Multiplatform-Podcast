import SwiftUI

/// A style of the type scale (ADR 0001), from the values `DesignTokens.swift` copies from the Kotlin theme. The size
/// grows with Dynamic Type along [relativeTo], as the system styles do.
struct TextToken {
    let font: String
    let size: CGFloat
    let lineHeight: CGFloat
    let tracking: CGFloat
    let relativeTo: Font.TextStyle
    var monospacedDigits = false
}

extension View {
    /// The font, tracking and line height of [token]; SwiftUI spaces lines by the gap between them, not their height.
    func textStyle(_ token: TextToken) -> some View {
        let font = Font.custom(token.font, size: token.size, relativeTo: token.relativeTo)
        return self
            .font(token.monospacedDigits ? font.monospacedDigit() : font)
            .tracking(token.tracking)
            .lineSpacing(token.lineHeight - token.size)
    }
}

extension Text {
    /// A list or form section's title in the theme's muted color: the system's header gray is too light to read (25.8f).
    static func header(_ title: String) -> Text {
        Text(title).foregroundStyle(Color(.Theme.onSurfaceVariant))
    }
}

extension Section where Parent == Text, Content: View, Footer == EmptyView {
    /// A section under a [Text.header] title.
    init(titled title: String, @ViewBuilder content: () -> Content) {
        self.init(content: content, header: { Text.header(title) })
    }
}
