import Shared
import SwiftUI

/// This podcast's own settings (25.8d), as the Compose sheet has them (feature/podcast): its speed and the seconds
/// skipped at the start and end (18.14), how many of the newest episodes stay downloaded (18.19) and, where new
/// episodes are looked for in the background, whether they are announced (18.18; iOS waits for 25.11). Each change
/// is saved at once.
struct PodcastSettingsSheet: View {
    let podcast: Podcast
    let send: (any PodcastDetailIntent) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let strings = Res.string.shared
        let playback = podcast.playback
        NavigationStack {
            Form {
                Picker(strings.playback_speed.localized(), selection: Binding(
                    get: { playback.speed?.floatValue },
                    set: { set(playback, speed: $0, intro: playback.skipIntroSeconds, outro: playback.skipOutroSeconds) }
                )) {
                    ForEach(PodcastSettingsTexts.speeds, id: \.self) { Text(PodcastSettingsTexts.speed($0)).tag($0) }
                }
                Section {
                    Picker(strings.skip_intro.localized(), selection: Binding(
                        get: { playback.skipIntroSeconds },
                        set: { set(playback, speed: playback.speed?.floatValue, intro: $0, outro: playback.skipOutroSeconds) }
                    )) {
                        ForEach(PodcastSettingsTexts.skips, id: \.self) { Text(PodcastSettingsTexts.seconds($0)).tag($0) }
                    }
                } footer: {
                    Text(strings.skip_intro_description.localized())
                }
                Section {
                    Picker(strings.skip_outro.localized(), selection: Binding(
                        get: { playback.skipOutroSeconds },
                        set: { set(playback, speed: playback.speed?.floatValue, intro: playback.skipIntroSeconds, outro: $0) }
                    )) {
                        ForEach(PodcastSettingsTexts.skips, id: \.self) { Text(PodcastSettingsTexts.seconds($0)).tag($0) }
                    }
                } footer: {
                    Text(strings.skip_outro_description.localized())
                }
                Section {
                    Picker(strings.auto_download.localized(), selection: Binding(
                        get: { podcast.autoDownloadCount },
                        set: { send(PodcastDetailIntentSetAutoDownload(count: $0)) }
                    )) {
                        ForEach(PodcastSettingsTexts.autoDownloads, id: \.self) { Text(PodcastSettingsTexts.newest($0)).tag($0) }
                    }
                } footer: {
                    Text(strings.auto_download_description.localized())
                }
                if PlatformCapabilities_iosKt.canRefreshInBackground {
                    Toggle(strings.podcast_notify_new.localized(), isOn: Binding(
                        get: { podcast.notifyNewEpisodes }, set: { send(PodcastDetailIntentSetNotifications(enabled: $0)) }
                    ))
                }
            }
            .navigationTitle(strings.podcast_settings_title.localized())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button(strings.close.localized()) { dismiss() } }
            }
        }
    }

    private func set(_ playback: PodcastPlayback, speed: Float?, intro: Int32, outro: Int32) {
        send(PodcastDetailIntentSetPlayback(playback: PodcastPlayback(
            speed: speed.map { KotlinFloat(float: $0) }, skipIntroSeconds: intro, skipOutroSeconds: outro
        )))
    }
}

/// The choices and their words, the shared lists read as the Compose sheet reads them.
enum PodcastSettingsTexts {
    /// The player's own speed first, then the shared ones.
    static let speeds: [Float?] = [nil] + AppConfig.shared.PLAYBACK_SPEEDS.map(\.floatValue)
    static let skips: [Int32] = PodcastPlayback.companion.SKIP_CHOICES.map(\.int32Value)
    static let autoDownloads: [Int32] = AutoDownload.shared.CHOICES.map(\.int32Value)

    static func speed(_ speed: Float?, bundle: Bundle = .main) -> String {
        guard let speed else { return Res.string.shared.speed_general.localized(bundle: bundle) }
        return SpeedLabelKt.formatSpeed(speed: speed)
    }

    static func seconds(_ seconds: Int32, bundle: Bundle = .main) -> String {
        seconds == 0 ? Res.string.shared.skip_off.localized(bundle: bundle)
            : Res.string.shared.seconds_short.localized([KotlinInt(int: seconds)], bundle: bundle)
    }

    static func newest(_ count: Int32, bundle: Bundle = .main) -> String {
        count == 0 ? Res.string.shared.skip_off.localized(bundle: bundle)
            : Res.plurals.shared.auto_download_newest.localized(count: Int(count), bundle: bundle)
    }
}
