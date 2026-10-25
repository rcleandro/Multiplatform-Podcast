import SwiftUI

/// A filter pill: filled while chosen, outlined otherwise, and read out as selected by VoiceOver.
struct Chip: View {
    let label: String
    let isOn: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label).textStyle(Typography.labelLarge)
                .padding(.horizontal, Spacing.m)
                .padding(.vertical, Spacing.s)
                .foregroundStyle(isOn ? Color(.Theme.onSecondaryContainer) : Color(.Theme.onSurfaceVariant))
                .background(isOn ? Color(.Theme.secondaryContainer) : .clear, in: Capsule())
                .overlay(Capsule().stroke(isOn ? .clear : Color(.Theme.outlineVariant)))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }
}
