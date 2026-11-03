import CoreGraphics

/// The window bands of 21.1 and 21.2, in points, the same numbers PaneLayout.kt takes from androidx.window: decided by
/// the window's size, never by the device, so Split View, Slide Over and Stage Manager move a screen between them.
enum WindowLayout {
    /// From here on, a tab's list and its pages side by side, and the player in two columns.
    static let expandedWidth: CGFloat = 840
    /// Below this, a phone on its side or a low window: the player trades height for width.
    static let shortHeight: CGFloat = 480
    /// The tab's list beside its pages in a wide window.
    static let listWidth: CGFloat = 360

    static func isExpanded(width: CGFloat) -> Bool { width >= expandedWidth }

    static func isShort(height: CGFloat) -> Bool { height > 0 && height < shortHeight }
}
