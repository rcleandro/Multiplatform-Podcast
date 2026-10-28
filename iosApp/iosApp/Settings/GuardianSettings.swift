import Shared
import SwiftUI

/// The guardians' section (26.6, ECA Digital arts. 17 and 18), in Brazil only: what is in effect for a minor, the
/// system's own tools, and, behind the PIN, what the guardians allow, as the Compose settings have it.
struct GuardianSection: View {
    let state: SettingsUiState
    let onOpenStats: () -> Void
    let send: (any SettingsIntent) -> Void

    var body: some View {
        let guardian = state.guardian
        if guardian.inBrazil {
            Section {
                if !guardian.hasPin {
                    Button { send(GuardianIntentOpen.shared) } label: {
                        GuardianRow(title: Res.string.shared.guardian_create_pin, detail: Res.string.shared.guardian_create_pin_description)
                    }
                } else if !guardian.unlocked {
                    Button(Res.string.shared.guardian_open.localized()) { send(GuardianIntentOpen.shared) }
                } else {
                    unlocked(guardian.controls)
                }
            } header: {
                Text.header(Res.string.shared.guardian_title.localized())
            } footer: {
                VStack(alignment: .leading, spacing: Spacing.s) {
                    if state.isMinor { Text(Res.string.shared.guardian_notice.localized()) }
                    Text(Res.string.shared.guardian_system_tools.localized())
                }
            }
        }
    }

    @ViewBuilder
    private func unlocked(_ controls: GuardianControls) -> some View {
        Toggle(isOn: Binding(get: { controls.allowContinuousPlayback }, set: {
            send(GuardianIntentSetControls(controls: controls.doCopy(allowContinuousPlayback: $0,
                                                                      allowNewEpisodeNotifications: controls.allowNewEpisodeNotifications,
                                                                      breakReminderMinutes: controls.breakReminderMinutes)))
        })) {
            GuardianRow(title: Res.string.shared.guardian_allow_continuous, detail: Res.string.shared.guardian_allow_continuous_description)
        }
        Toggle(isOn: Binding(get: { controls.allowNewEpisodeNotifications }, set: {
            send(GuardianIntentSetControls(controls: controls.doCopy(allowContinuousPlayback: controls.allowContinuousPlayback,
                                                                      allowNewEpisodeNotifications: $0,
                                                                      breakReminderMinutes: controls.breakReminderMinutes)))
        })) {
            GuardianRow(title: Res.string.shared.guardian_allow_notifications,
                        detail: Res.string.shared.guardian_allow_notifications_description)
        }
        Picker(Res.string.shared.guardian_break_reminder.localized(), selection: Binding(
            get: { Int(controls.breakReminderMinutes) },
            set: {
                send(GuardianIntentSetControls(controls: controls.doCopy(allowContinuousPlayback: controls.allowContinuousPlayback,
                                                                          allowNewEpisodeNotifications: controls.allowNewEpisodeNotifications,
                                                                          breakReminderMinutes: Int32($0))))
            }
        )) {
            ForEach(GuardianControls.companion.BREAK_CHOICES.map(\.intValue), id: \.self) { Text(PinTexts.breakReminder($0)).tag($0) }
        }
        Button(Res.string.shared.guardian_listening_time.localized(), action: onOpenStats)
        Button(Res.string.shared.guardian_change_pin.localized()) { send(GuardianIntentChangePin.shared) }
        Button { send(GuardianIntentRemovePin.shared) } label: {
            GuardianRow(title: Res.string.shared.guardian_remove_pin, detail: Res.string.shared.guardian_remove_pin_description)
        }
        Button(Res.string.shared.guardian_lock.localized()) { send(GuardianIntentLock.shared) }
    }
}

private struct GuardianRow: View {
    let title: LibraryStringResource
    let detail: LibraryStringResource

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xxs) {
            Text(title.localized()).foregroundStyle(Color.primary)
            Text(detail.localized()).textStyle(Typography.bodySmall).foregroundStyle(Color(.Theme.onSurfaceVariant))
        }
    }
}

/// Asks for the PIN, to create it (twice) or to open the controls: four digits, never shown. A sheet rather than an
/// alert, which closes on every button and would blink back after a wrong PIN.
struct GuardianPinSheet: View {
    let step: PinStep
    let error: PinError?
    let waitSeconds: Int64
    let send: (any SettingsIntent) -> Void
    @State private var pin = ""
    @FocusState private var focused: Bool

    var body: some View {
        NavigationStack {
            Form {
                SecureField("", text: Binding(get: { pin }, set: { pin = PinTexts.digits($0) }))
                    .keyboardType(.numberPad)
                    .textContentType(.oneTimeCode)
                    .focused($focused)
                    .accessibilityLabel(PinTexts.title(step))
                if let error {
                    Text(PinTexts.error(error, waitSeconds: waitSeconds)).foregroundStyle(Color(.Theme.error))
                }
            }
            .navigationTitle(PinTexts.title(step))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(Res.string.shared.cancel.localized()) { send(GuardianIntentDismissPin.shared) }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(Res.string.shared.guardian_pin_ok.localized()) {
                        send(GuardianIntentEnterPin(pin: pin))
                        pin = ""
                    }
                    .disabled(pin.count != Int(GuardianControlsKt.GUARDIAN_PIN_LENGTH))
                }
            }
        }
        .presentationDetents([.medium])
        .onAppear { focused = true }
        .onChange(of: step) { _, _ in pin = "" }
    }
}

/// The PIN's words, as the Compose dialog writes them.
enum PinTexts {
    private static let secondsPerMinute: Int64 = 60

    /// Only digits, at most the PIN's length.
    static func digits(_ typed: String) -> String {
        String(typed.filter(\.isNumber).prefix(Int(GuardianControlsKt.GUARDIAN_PIN_LENGTH)))
    }

    static func title(_ step: PinStep, bundle: Bundle = .main) -> String {
        let strings = Res.string.shared
        return switch step {
        case .create: strings.guardian_pin_create.localized(bundle: bundle)
        case .confirm: strings.guardian_pin_repeat.localized(bundle: bundle)
        default: strings.guardian_pin_unlock.localized(bundle: bundle)
        }
    }

    /// Up to a minute in seconds, then in whole minutes, rounded up so the wait never reads shorter than it is.
    static func error(_ error: PinError, waitSeconds: Int64, bundle: Bundle = .main) -> String {
        let strings = Res.string.shared
        switch error {
        case .wrong: return strings.guardian_pin_wrong.localized(bundle: bundle)
        case .mismatch: return strings.guardian_pin_mismatch.localized(bundle: bundle)
        default:
            if waitSeconds <= secondsPerMinute {
                return strings.guardian_pin_wait_seconds.localized([KotlinLong(value: waitSeconds)], bundle: bundle)
            }
            let minutes = (waitSeconds + secondsPerMinute - 1) / secondsPerMinute
            return strings.guardian_pin_wait_minutes.localized([KotlinLong(value: minutes)], bundle: bundle)
        }
    }

    /// "Every 60 min", or never.
    static func breakReminder(_ minutes: Int, bundle: Bundle = .main) -> String {
        minutes == 0 ? Res.string.shared.guardian_break_never.localized(bundle: bundle)
            : Res.string.shared.guardian_break_every.localized([KotlinInt(int: Int32(minutes))], bundle: bundle)
    }
}
