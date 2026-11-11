import Shared
import SwiftUI
import UniformTypeIdentifiers

/// Settings (25.8b), as the Compose screen has them (feature/settings): appearance, the library's files, playback,
/// downloads, content and privacy, and the guardians' section in Brazil. The language is the system's per-app setting on
/// iOS, and new episodes are not looked for in the background yet (25.11), as on the Compose app for iOS.
struct SettingsScreen: View {
    let model: ShellModel
    @State private var scope: ScreenScope
    @State private var settings: Watched<SettingsUiState>
    @State private var importing: FileKind?
    @State private var exporting: ExportedFile?

    init(model: ShellModel) {
        self.model = model
        let scope = ScreenScope()
        _scope = State(initialValue: scope)
        _settings = State(initialValue: Watched(scope.viewModels.settings().uiState))
    }

    var body: some View {
        let state = settings.value
        NavigationStack {
            Form {
                Section {
                    Button { model.root.onOpenListeningStats() } label: {
                        row(Res.string.shared.stats_title, Res.string.shared.stats_description)
                    }
                }
                appearance(state)
                files(state)
                updates(state)
                playback(state)
                if let used = state.usedBytes { downloads(state, used: used.int64Value) }
                content(state)
                GuardianSection(state: state, onOpenStats: { model.root.onOpenListeningStats() }, send: send)
            }
            .formPickers()
            .navigationTitle(Res.string.shared.settings.localized())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(Res.string.shared.close.localized()) { model.root.onBackClicked() }
                }
            }
            .confirmationDialog(Res.string.shared.settings_delete_all.localized(), isPresented: Binding(
                get: { state.isConfirmingDeleteAll }, set: { if !$0 { send(SettingsIntentDismissDeleteAll.shared) } }
            ), titleVisibility: .visible) {
                Button(Res.string.shared.delete_.localized(), role: .destructive) { send(SettingsIntentConfirmDeleteAll.shared) }
                Button(Res.string.shared.cancel.localized(), role: .cancel) { send(SettingsIntentDismissDeleteAll.shared) }
            } message: {
                Text(Res.string.shared.settings_delete_all_confirmation.localized([bytes(state.usedBytes?.int64Value ?? 0)]))
            }
            .fileImporter(isPresented: Binding(get: { importing != nil }, set: { if !$0 { importing = nil } }),
                          allowedContentTypes: [.item]) { picked in
                let text = try? OpmlFile.read(picked.get())
                send(importing == .backup ? SettingsIntentRestoreBackup(text: text) : SettingsIntentImportOpml(text: text))
                importing = nil
            }
            .fileExporter(isPresented: Binding(get: { exporting != nil }, set: { if !$0 { exporting = nil } }),
                          document: exporting?.document, contentType: .plainText,
                          defaultFilename: exporting?.name) { result in
                let saved = (try? result.get()) != nil
                send(exporting?.kind == .backup ? SettingsIntentBackupSaved(saved: saved) : SettingsIntentOpmlSaved(saved: saved))
                exporting = nil
            }
            .sheet(isPresented: Binding(
                get: { state.guardian.pinStep != nil }, set: { if !$0 { send(GuardianIntentDismissPin.shared) } }
            )) {
                if let step = state.guardian.pinStep {
                    GuardianPinSheet(step: step, error: state.guardian.pinError, waitSeconds: state.guardian.pinWaitSeconds, send: send)
                }
            }
            .onChange(of: state.opmlToSave) { _, text in handOver(text, kind: .opml) }
            .onChange(of: state.backupToSave) { _, text in handOver(text, kind: .backup) }
        }
    }

    // MARK: Sections

    private func appearance(_ state: SettingsUiState) -> some View {
        Section(titled: Res.string.shared.settings_appearance.localized()) {
            Picker(Res.string.shared.settings_theme.localized(), selection: Binding(
                get: { state.themeMode }, set: { send(SettingsIntentSetTheme(mode: $0)) }
            )) {
                Text(Res.string.shared.settings_theme_system.localized()).tag(ThemeMode.system)
                Text(Res.string.shared.settings_theme_light.localized()).tag(ThemeMode.light)
                Text(Res.string.shared.settings_theme_dark.localized()).tag(ThemeMode.dark)
            }
            // The app's language is the system's per-app setting on iOS (18.5).
            Button { UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!) } label: {
                row(Res.string.shared.settings_language, Res.string.shared.settings_language_ios)
            }
        }
    }

    /// OPML for other apps (18.13) and the backup of everything for this one (19.11); each row says how its last run
    /// went. Sync through a folder (19.12) has no iOS folder to keep, as on the Compose app.
    private func files(_ state: SettingsUiState) -> some View {
        Section {
            fileRows(state.opml, kind: .opml)
            fileRows(state.backup, kind: .backup)
        } header: {
            Text.header(Res.string.shared.settings_subscriptions.localized())
        } footer: {
            // Those files hold each feed's address, a private one's token included (22.6).
            if state.hasPrivateFeeds { Text(Res.string.shared.settings_private_feeds_note.localized()) }
        }
    }

    @ViewBuilder
    private func fileRows(_ status: OpmlStatus, kind: FileKind) -> some View {
        let isBackup = kind == .backup
        Button { if !(status is OpmlStatusImporting) { importing = kind } } label: {
            row(isBackup ? Res.string.shared.settings_restore_backup : Res.string.shared.settings_import_opml,
                text: SettingsTexts.importStatus(status, idle: isBackup ? Res.string.shared.settings_restore_backup_description
                                                                         : Res.string.shared.settings_import_opml_description))
        }
        Button { send(isBackup ? SettingsIntentExportBackup.shared : SettingsIntentExportOpml.shared) } label: {
            row(isBackup ? Res.string.shared.settings_export_backup : Res.string.shared.settings_export_opml,
                text: SettingsTexts.exportStatus(status, idle: isBackup ? Res.string.shared.settings_export_backup_description
                                                                         : Res.string.shared.settings_export_opml_description))
        }
    }

    private func playback(_ state: SettingsUiState) -> some View {
        let skips = state.skipIntervals
        // A minor in Brazil, unless the guardians allow it (26.5, 26.6).
        let locked = state.isMinor && !state.guardian.controls.allowContinuousPlayback
        return Section(titled: Res.string.shared.settings_playback.localized()) {
            skipPicker(Res.string.shared.settings_skip_backward, skips.backwardSeconds) {
                send(SettingsIntentSetSkipIntervals(intervals: SkipIntervals(backwardSeconds: $0, forwardSeconds: skips.forwardSeconds)))
            }
            skipPicker(Res.string.shared.settings_skip_forward, skips.forwardSeconds) {
                send(SettingsIntentSetSkipIntervals(intervals: SkipIntervals(backwardSeconds: skips.backwardSeconds, forwardSeconds: $0)))
            }
            toggle(Res.string.shared.settings_continuous_playback,
                   locked ? Res.string.shared.settings_locked_for_minors : Res.string.shared.settings_continuous_playback_description,
                   isOn: state.continuousPlayback && !locked, disabled: locked) {
                send(SettingsIntentSetContinuousPlayback(enabled: $0))
            }
        }
    }

    /// How often to look for new episodes while closed (25.11) and whether to tell about them.
    private func updates(_ state: SettingsUiState) -> some View {
        // A minor in Brazil, unless the guardians allow them (26.5, 26.6).
        let locked = state.isMinor && !state.guardian.controls.allowNewEpisodeNotifications
        return Section(titled: Res.string.shared.settings_updates.localized()) {
            Picker(Res.string.shared.settings_refresh.localized(), selection: Binding(
                get: { Int(state.refreshIntervalHours) }, set: { send(SettingsIntentSetRefreshInterval(hours: Int32($0))) }
            )) {
                ForEach(RefreshInterval.shared.CHOICES.map(\.intValue), id: \.self) { hours in
                    Text(SettingsTexts.refresh(hours)).tag(hours)
                }
            }
            // Nothing is looked for while closed, so nothing would notify.
            if state.refreshIntervalHours > 0 {
                toggle(Res.string.shared.settings_notify_new,
                       locked ? Res.string.shared.settings_locked_for_minors : Res.string.shared.settings_notify_new_description,
                       isOn: state.notifyNewEpisodes && !locked, disabled: locked) { send(SettingsIntentSetNotifyNewEpisodes(enabled: $0)) }
            }
        }
    }

    private func downloads(_ state: SettingsUiState, used: Int64) -> some View {
        Section(titled: Res.string.shared.settings_downloads.localized()) {
            Text(Res.string.shared.downloads_storage_used.localized([bytes(used)]))
            LimitSlider(stops: state.limitStops.map { ($0 as? NSNumber)?.int64Value }, limit: state.downloadLimitBytes?.int64Value) {
                send(SettingsIntentSetDownloadLimit(bytes: $0.map { KotlinLong(value: $0) }))
            }
            toggle(Res.string.shared.settings_wifi_only, Res.string.shared.settings_wifi_only_description, isOn: state.wifiOnlyDownloads) {
                send(SettingsIntentSetWifiOnly(enabled: $0))
            }
            toggle(Res.string.shared.settings_download_next, Res.string.shared.settings_download_next_description,
                   isOn: state.downloadNextOnFinish) { send(SettingsIntentSetDownloadNext(enabled: $0)) }
            toggle(Res.string.shared.settings_delete_finished, Res.string.shared.settings_delete_finished_description,
                   isOn: state.deleteFinishedDownloads) { send(SettingsIntentSetDeleteFinished(enabled: $0)) }
            Picker(Res.string.shared.settings_delete_after.localized(), selection: Binding(
                get: { Int(state.deleteDownloadsAfterDays) }, set: { send(SettingsIntentSetDeleteAfter(days: Int32($0))) }
            )) {
                ForEach(DeleteOldDownloads.shared.CHOICES.map(\.intValue), id: \.self) { days in
                    Text(SettingsTexts.deleteAfter(days)).tag(days)
                }
            }
            if used > 0 {
                Button(Res.string.shared.settings_delete_all.localized(), role: .destructive) {
                    send(SettingsIntentRequestDeleteAll.shared)
                }
            }
        }
    }

    private func content(_ state: SettingsUiState) -> some View {
        Group {
            Section(titled: Res.string.shared.settings_content.localized()) {
                toggle(Res.string.shared.settings_show_explicit,
                       state.explicitLocked ? Res.string.shared.settings_show_explicit_locked : Res.string.shared.settings_show_explicit_description,
                       isOn: state.showExplicit, disabled: state.explicitLocked) { send(SettingsIntentSetShowExplicit(enabled: $0)) }
            }
            Section(titled: Res.string.shared.settings_privacy.localized()) {
                Link(destination: SettingsTexts.privacyPolicy) {
                    row(Res.string.shared.settings_privacy_policy, Res.string.shared.settings_privacy_policy_description)
                }
                // Access, correction or deletion of personal data (LGPD Art. 18), privately (27.4).
                if let contact = URL(string: ReportMailKt.contactMailto(subject: Res.string.shared.privacy_contact_subject.localized(), body: "")) {
                    Link(destination: contact) {
                        row(Res.string.shared.settings_privacy_contact, Res.string.shared.settings_privacy_contact_description)
                    }
                }
                // A minor in Brazil has telemetry off for good (26.4): nothing to choose, so no row.
                if !state.isMinor {
                    toggle(Res.string.shared.settings_telemetry, Res.string.shared.settings_telemetry_description,
                           isOn: state.telemetryEnabled) { send(SettingsIntentSetTelemetry(enabled: $0)) }
                }
            }
        }
    }

    // MARK: Rows

    private func row(_ title: LibraryStringResource, _ detail: LibraryStringResource) -> some View {
        row(title, text: detail.localized())
    }

    private func row(_ title: LibraryStringResource, text: String) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xxs) {
            Text(title.localized()).foregroundStyle(Color.primary)
            Text(text).textStyle(Typography.bodySmall).foregroundStyle(Color(.Theme.onSurfaceVariant))
        }
    }

    private func toggle(_ title: LibraryStringResource, _ detail: LibraryStringResource, isOn: Bool, disabled: Bool = false,
                        onChange: @escaping (Bool) -> Void) -> some View {
        Toggle(isOn: Binding(get: { isOn }, set: onChange)) { row(title, detail) }
            .disabled(disabled)
    }

    private func skipPicker(_ title: LibraryStringResource, _ seconds: Int32, onChange: @escaping (Int32) -> Void) -> some View {
        Picker(title.localized(), selection: Binding(get: { seconds }, set: onChange)) {
            ForEach(SkipIntervals.companion.CHOICES.map(\.int32Value), id: \.self) {
                Text(Res.string.shared.seconds_short.localized([KotlinInt(int: $0)])).tag($0)
            }
        }
    }

    // MARK: Files

    private func handOver(_ text: String?, kind: FileKind) {
        guard let text else { return }
        send(kind == .backup ? SettingsIntentBackupHandedOver.shared : SettingsIntentOpmlHandedOver.shared)
        exporting = ExportedFile(kind: kind, document: TextDocument(text: text))
    }

    private func bytes(_ count: Int64) -> String {
        ByteCountFormatter.string(fromByteCount: count, countStyle: .file)
    }

    private func send(_ intent: any SettingsIntent) {
        scope.viewModels.settings().onIntent(intent: intent)
    }
}

enum FileKind { case opml, backup }

private struct ExportedFile {
    let kind: FileKind
    let document: TextDocument

    /// "podcasts.opml" and "podcast-backup.json", as the Compose app names them.
    var name: String { kind == .backup ? "podcast-backup.json" : "podcasts.opml" }
}

/// An export's text, for the system's save dialog.
struct TextDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.plainText] }
    let text: String

    init(text: String) {
        self.text = text
    }

    init(configuration: ReadConfiguration) throws {
        text = String(decoding: configuration.file.regularFileContents ?? Data(), as: UTF8.self)
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(text.utf8))
    }
}

/// The settings' words that depend on a value, as the Compose rows write them.
enum SettingsTexts {
    static let privacyPolicy = URL(string: "https://github.com/rcleandro/Multiplatform-Podcast/blob/main/docs/PRIVACIDADE.md")!

    static func importStatus(_ status: OpmlStatus, idle: LibraryStringResource, bundle: Bundle = .main) -> String {
        let strings = Res.string.shared
        switch status {
        case let importing as OpmlStatusImporting:
            return strings.opml_importing.localized([importing.done, importing.total].map { KotlinInt(int: $0) }, bundle: bundle)
        case let imported as OpmlStatusImported:
            let result = imported.result
            if result.added == 0 && result.known == 0 && result.failed == 0 { return strings.opml_nothing_to_import.localized(bundle: bundle) }
            return strings.opml_imported.localized([result.added, result.known, result.failed].map { KotlinInt(int: $0) }, bundle: bundle)
        case is OpmlStatusNotABackup:
            return strings.backup_not_valid.localized(bundle: bundle)
        default:
            return idle.localized(bundle: bundle)
        }
    }

    static func exportStatus(_ status: OpmlStatus, idle: LibraryStringResource, bundle: Bundle = .main) -> String {
        switch status {
        case let exported as OpmlStatusExported:
            return Res.string.shared.opml_exported.localized([KotlinInt(int: exported.podcasts)], bundle: bundle)
        case is OpmlStatusExportFailed:
            return Res.string.shared.opml_export_failed.localized(bundle: bundle)
        default:
            return idle.localized(bundle: bundle)
        }
    }

    /// "Every 6 hours", or never.
    static func refresh(_ hours: Int, bundle: Bundle = .main) -> String {
        hours == 0 ? Res.string.shared.settings_refresh_never.localized(bundle: bundle)
            : Res.plurals.shared.settings_refresh_every.localized(count: hours, bundle: bundle)
    }

    /// "30 days", or kept.
    static func deleteAfter(_ days: Int, bundle: Bundle = .main) -> String {
        days == 0 ? Res.string.shared.settings_delete_after_never.localized(bundle: bundle)
            : Res.plurals.shared.settings_delete_after_days.localized(count: days, bundle: bundle)
    }

    /// A storage limit, or "No limit".
    static func limit(_ bytes: Int64?, bundle: Bundle = .main) -> String {
        bytes.map { ByteCountFormatter.string(fromByteCount: $0, countStyle: .file) }
            ?? Res.string.shared.settings_no_limit.localized(bundle: bundle)
    }
}

/// The storage limit as a slider over the stops the view model offers; saved when let go.
private struct LimitSlider: View {
    let stops: [Int64?]
    let limit: Int64?
    let onChange: (Int64?) -> Void
    @State private var index: Double?

    var body: some View {
        let current = index ?? Double(stops.firstIndex(of: limit) ?? 0)
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack {
                Text(Res.string.shared.settings_storage_limit.localized())
                Spacer()
                Text(SettingsTexts.limit(stops[safe: Int(current.rounded())] ?? nil)).foregroundStyle(Color(.Theme.onSurfaceVariant))
            }
            if stops.count > 1 {
                Slider(value: Binding(get: { current }, set: { index = $0 }), in: 0...Double(stops.count - 1), step: 1) { editing in
                    if !editing, let index { onChange(stops[Int(index.rounded())]); self.index = nil }
                }
                .accessibilityValue(SettingsTexts.limit(stops[safe: Int(current.rounded())] ?? nil))
            }
        }
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? { indices.contains(index) ? self[index] : nil }
}

extension View {
    /// A form's pickers as menus, or as pages of their own at the accessibility text sizes, where a menu's value no
    /// longer fits beside its title and is cut (25.8f).
    func formPickers() -> some View { modifier(FormPickers()) }
}

private struct FormPickers: ViewModifier {
    @Environment(\.dynamicTypeSize) private var textSize

    func body(content: Content) -> some View {
        if textSize.isAccessibilitySize {
            // The page link writes the value in the system's secondary gray, too light to read on iOS 27.
            content.pickerStyle(.navigationLink)
                .foregroundStyle(Color(.Theme.onSurface), Color(.Theme.onSurfaceVariant))
        } else {
            content
        }
    }
}
