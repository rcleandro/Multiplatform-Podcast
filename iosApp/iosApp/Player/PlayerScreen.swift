import Shared
import SwiftUI

/// The full-screen player (25.7): cover, a progress bar to drag, the skips Settings chose, previous and next, speed,
/// sleep timer and the queue, as the Compose screen does (feature/player); bookmarks, chapters and sharing from the
/// moment (25.8e). The system's Now Playing stays as it is.
struct PlayerScreen: View {
    let model: ShellModel
    @State private var player: Watched<PlayerState>
    /// Where the thumb is while it is dragged; the player only seeks when it is let go.
    @State private var dragged: Double?
    @State private var bookmarksOpen = false

    init(model: ShellModel) {
        self.model = model
        _player = State(initialValue: Watched(model.app.viewModels.player().playerState))
    }

    private static let artworkSize: CGFloat = 280

    var body: some View {
        let state = player.value
        NavigationStack {
            VStack(spacing: Spacing.l) {
                if let episode = state.currentEpisode {
                    Artwork(url: episode.imageUrl, size: Self.artworkSize, radius: Radius.extraLarge)
                        .frame(maxWidth: Self.artworkSize)
                    VStack(spacing: Spacing.xs) {
                        Text(episode.title).textStyle(Typography.titleLarge).multilineTextAlignment(.center).lineLimit(3)
                        if let podcast = episode.podcastTitle {
                            Text(podcast).textStyle(Typography.labelLarge).foregroundStyle(Color(.Theme.accentText))
                        }
                    }
                    progress(state)
                    controls(state)
                    options(state)
                    extras(state)
                } else {
                    ContentUnavailableView(Res.string.shared.no_episode_selected.localized(), systemImage: "play.circle")
                }
            }
            .padding(Spacing.l)
            .frame(maxWidth: Sizes.readableWidth)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button { model.root.onBackClicked() } label: { Image(systemName: "chevron.down") }
                        .accessibilityLabel(Res.string.shared.minimize.localized())
                }
                if let episode = state.currentEpisode {
                    ToolbarItem(placement: .topBarTrailing) { share(episode, at: state.position) }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button { model.root.onQueueClicked() } label: { Image(systemName: "list.bullet") }
                        .accessibilityLabel(Res.string.shared.queue.localized())
                }
            }
            .sheet(isPresented: $bookmarksOpen) {
                BookmarksSheet(viewModel: model.app.viewModels.player(), positionMs: state.position)
            }
        }
    }

    /// The episode from the moment it is at (18.4); a private feed's audio is never shared (22.6).
    private func share(_ episode: Episode, at position: Int64) -> some View {
        ShareLink(item: momentShareText(episode, at: position)) {
            Label(Res.string.shared.share.localized(), systemImage: "square.and.arrow.up")
        }
    }

    /// What the episode has: bookmarks (19.3) and, when the feed gives them, a transcript (19.2) and chapters (19.1), the
    /// one playing marked.
    private func extras(_ state: PlayerState) -> some View {
        HStack(spacing: Spacing.l) {
            Button { bookmarksOpen = true } label: {
                Label(Res.string.shared.bookmarks.localized(), systemImage: "bookmark")
            }
            .labelStyle(.iconOnly)
            if state.currentEpisode?.transcriptUrl != nil {
                Button { model.root.onTranscriptClicked() } label: {
                    Label(Res.string.shared.transcript.localized(), systemImage: "text.quote")
                }
                .labelStyle(.iconOnly)
            }
            if !state.chapters.isEmpty {
                Menu {
                    Picker(Res.string.shared.chapters.localized(), selection: Binding(
                        get: { state.currentChapter?.startMs }, set: { if let start = $0 { send(PlayerIntentSeekTo(positionMs: start)) } }
                    )) {
                        ForEach(state.chapters, id: \.startMs) { chapter in
                            Text("\(LongExtensionsKt.toTime(chapter.startMs))  \(chapterLabel(chapter))").tag(Optional(chapter.startMs))
                        }
                    }
                } label: {
                    Label(state.currentChapter.map(chapterLabel) ?? Res.string.shared.chapters.localized(), systemImage: "list.bullet.indent")
                        .lineLimit(1)
                }
                .accessibilityLabel(Res.string.shared.chapters.localized())
            }
        }
        .buttonStyle(.bordered)
    }

    private func progress(_ state: PlayerState) -> some View {
        let duration = Double(state.duration?.int64Value ?? 0)
        let position = dragged ?? Double(state.position)
        return VStack(spacing: Spacing.xs) {
            Slider(value: Binding(get: { position }, set: { dragged = $0 }), in: 0...max(duration, 1)) { editing in
                if !editing, let target = dragged {
                    send(PlayerIntentSeekTo(positionMs: Int64(target)))
                    dragged = nil
                }
            }
            .disabled(duration <= 0)
            .accessibilityValue(LongExtensionsKt.toTime(Int64(position)))
            HStack {
                Text(LongExtensionsKt.toTime(Int64(position)))
                Spacer()
                Text(duration > 0 ? LongExtensionsKt.toTime(Int64(duration)) : "")
            }
            .textStyle(Typography.timer)
            .foregroundStyle(.secondary)
        }
    }

    private func controls(_ state: PlayerState) -> some View {
        let back = Int(state.skipIntervals.backwardSeconds)
        let forward = Int(state.skipIntervals.forwardSeconds)
        return HStack(spacing: Spacing.xl) {
            control("backward.end.fill", Res.string.shared.previous.localized()) { send(PlayerIntentPrevious.shared) }
            control("gobackward", Res.string.shared.skip_backward.localized([KotlinInt(int: Int32(back))])) {
                send(PlayerIntentSkipBackward.shared)
            }
            Button { send(PlayerIntentPlayPause.shared) } label: {
                Group {
                    if state.isBuffering { ProgressView() } else { Image(systemName: state.isPlaying ? "pause.circle.fill" : "play.circle.fill") }
                }
                .font(.system(size: Sizes.playButtonLarge))
                .frame(width: Sizes.playButtonLarge, height: Sizes.playButtonLarge)
            }
            .accessibilityLabel((state.isPlaying ? Res.string.shared.pause : Res.string.shared.play).localized())
            control("goforward", Res.string.shared.skip_forward.localized([KotlinInt(int: Int32(forward))])) {
                send(PlayerIntentSkipForward.shared)
            }
            control("forward.end.fill", Res.string.shared.next.localized()) { send(PlayerIntentNext.shared) }
        }
        .foregroundStyle(Color(.Theme.primary))
    }

    private func control(_ symbol: String, _ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol).font(.title2).frame(width: Sizes.touchTarget, height: Sizes.touchTarget)
        }
        .accessibilityLabel(label)
    }

    /// Speed and the sleep timer, each a menu that shows what is chosen.
    private func options(_ state: PlayerState) -> some View {
        HStack(spacing: Spacing.l) {
            Menu {
                Picker(Res.string.shared.playback_speed.localized(), selection: Binding(
                    get: { state.speed }, set: { send(PlayerIntentSetSpeed(speed: $0)) }
                )) {
                    ForEach(AppConfig.shared.PLAYBACK_SPEEDS.map(\.floatValue), id: \.self) { speed in
                        Text(SpeedLabelKt.formatSpeed(speed: speed)).tag(speed)
                    }
                }
            } label: {
                Label(SpeedLabelKt.formatSpeed(speed: state.speed), systemImage: "gauge.with.dots.needle.67percent")
            }
            .accessibilityLabel(Res.string.shared.playback_speed.localized())
            .accessibilityValue(SpeedLabelKt.formatSpeed(speed: state.speed))
            Menu {
                ForEach(SleepTimers.options, id: \.label) { option in
                    Button(option.label.localized()) { send(PlayerIntentSetSleepTimer(timer: option.timer)) }
                }
                if state.sleepTimer != nil {
                    Divider()
                    Button(Res.string.shared.sleep_timer_extend.localized()) { send(PlayerIntentExtendSleepTimer.shared) }
                }
            } label: {
                Label(timerLabel(state), systemImage: state.sleepTimer == nil ? "moon" : "moon.fill")
            }
            .accessibilityLabel(Res.string.shared.sleep_timer.localized())
            .accessibilityValue(timerLabel(state))
        }
        .buttonStyle(.bordered)
    }

    private func timerLabel(_ state: PlayerState) -> String {
        SleepTimers.label(timer: state.sleepTimer, millisLeft: state.sleepTimerMillis?.int64Value)
    }

    private func send(_ intent: any PlayerIntent) {
        model.app.viewModels.player().onIntent(intent: intent)
    }
}

/// The episode's share text from [position], by the same builder as the Compose player.
func momentShareText(_ episode: Episode, at position: Int64, bundle: Bundle = .main) -> String {
    let startsAt = Res.string.shared.share_from_time.localized([LongExtensionsKt.toTime(position)], bundle: bundle)
    return ShareTextKt.episodeShareText(episode: episode, startsAt: startsAt, positionMs: KotlinLong(value: position))
}

/// The sleep timer's choices, as the Compose dialog lists them (18.15).
enum SleepTimers {
    struct Option {
        let label: LibraryStringResource
        let timer: (any SleepTimer)?
    }

    static var options: [Option] {
        let strings = Res.string.shared
        return [
            Option(label: strings.timer_disabled, timer: nil),
            Option(label: strings.timer_5_min, timer: SleepTimerMinutes(minutes: 5)),
            Option(label: strings.timer_15_min, timer: SleepTimerMinutes(minutes: 15)),
            Option(label: strings.timer_30_min, timer: SleepTimerMinutes(minutes: 30)),
            Option(label: strings.timer_45_min, timer: SleepTimerMinutes(minutes: 45)),
            Option(label: strings.timer_60_min, timer: SleepTimerMinutes(minutes: 60)),
            Option(label: strings.timer_end_of_episode, timer: SleepTimerEndOfEpisode.shared),
        ]
    }

    /// "Timer" when off, the time left on a timer in minutes, "End of episode" for that one.
    static func label(timer: (any SleepTimer)?, millisLeft: Int64?) -> String {
        if let millisLeft { return LongExtensionsKt.toTime(millisLeft) }
        if timer != nil { return Res.string.shared.timer_end_of_episode.localized() }
        return Res.string.shared.sleep_timer_short.localized()
    }
}
