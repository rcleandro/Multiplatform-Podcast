import Shared
import SwiftUI

/// The full-screen player (25.7): cover, a progress bar to drag, the skips Settings chose, previous and next, speed,
/// sleep timer and the queue, as the Compose screen does (feature/player); bookmarks, chapters and sharing from the
/// moment, the transcript and a video episode's picture (25.8e). The system's Now Playing stays as it is.
struct PlayerScreen: View {
    let model: ShellModel
    @State private var player: Watched<PlayerState>
    /// Where the thumb is while it is dragged; the player only seeks when it is let go.
    @State private var dragged: Double?
    @State private var bookmarksOpen = false
    @State private var size: CGSize = .zero
    /// A foldable half open (21.3): its hinge splits the screen in two, as the Compose player's tabletop posture.
    @State private var halfOpen = false
    /// The whole window and where the player's content starts in it, to put the split on the hinge.
    @State private var window: CGSize = .zero
    @State private var contentOrigin: CGPoint = .zero
    /// Lets the cover and the details glide to their halves when the hinge moves.
    @Namespace private var foldTransition

    init(model: ShellModel) {
        self.model = model
        _player = State(initialValue: Watched(model.app.viewModels.player().playerState))
    }

    @Environment(\.dynamicTypeSize) private var textSize

    private static let artworkSize: CGFloat = 280
    private static let titleLines = 3
    private static let videoAspectRatio: CGFloat = 16 / 9
    /// Cover beside the rest: as wide as two readable columns.
    private static let twoColumnsWidth = Sizes.readableWidth * 2
    /// The details beside the cover: a little over the controls' row, so a low window still has room for the cover.
    private static let detailsWidth: CGFloat = 380

    var body: some View {
        let state = player.value
        NavigationStack {
            // At the accessibility text sizes the player no longer fits the screen, so it scrolls there.
            Group {
                if textSize.isAccessibilitySize { ScrollView { content(state) } } else { content(state) }
            }
            .artworkBackdrop(url: state.currentEpisode?.imageUrl)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button { model.root.onBackClicked() } label: { Image(systemName: "chevron.down") }
                        .accessibilityLabel(Res.string.shared.minimize.localized())
                }
                if let episode = state.currentEpisode {
                    ToolbarItem(placement: .topBarTrailing) {
                        AirPlayButton(prefersVideo: episode.isVideo).frame(width: Sizes.iconXl, height: Sizes.iconXl)
                    }
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
        // The screen's size, not the content's, which grows past a low screen when it is in one column.
        .onGeometryChange(for: CGSize.self) { $0.size } action: { size = $0 }
        .background { Color.clear.ignoresSafeArea().onGeometryChange(for: CGSize.self) { $0.size } action: { window = $0 } }
        .modifier(HingeWatcher(halfOpen: $halfOpen))
    }

    /**
     * The cover or the video, the titles, the progress, the controls and the options. A wide window (21.1) or a low one,
     * like a phone on its side (21.2), puts the cover beside the rest, sized by the height, so play shows without
     * scrolling.
     */
    @ViewBuilder private func content(_ state: PlayerState) -> some View {
        if halfOpen, !textSize.isAccessibilitySize, let episode = state.currentEpisode {
            folded(state, episode)
        } else {
            unfolded(state)
        }
    }

    /**
     * Half open: the hinge crosses the middle of the screen's longer side. Across a screen held upright (tabletop) the
     * cover goes above it and the rest below, where the hand is; down a screen on its side (book) they go to each half.
     */
    private func folded(_ state: PlayerState, _ episode: Episode) -> some View {
        let isTabletop = window.height > window.width
        let layout = isTabletop ? AnyLayout(VStackLayout(spacing: 0)) : AnyLayout(HStackLayout(spacing: 0))
        // ponytail: the hinge is taken as the middle of a full-screen window; UIHinge gives no position.
        let firstHalf = max(0, isTabletop ? window.height / 2 - contentOrigin.y : window.width / 2 - contentOrigin.x)
        return layout {
            cover(episode, fitted: true)
                .matchedGeometryEffect(id: FoldPart.cover, in: foldTransition)
                .padding(Spacing.l)
                .frame(width: isTabletop ? nil : firstHalf, height: isTabletop ? firstHalf : nil)
                .frame(maxWidth: isTabletop ? .infinity : nil, maxHeight: isTabletop ? nil : .infinity)
            details(state, episode, isShort: true)
                .matchedGeometryEffect(id: FoldPart.details, in: foldTransition)
                .padding(Spacing.l)
                .frame(maxWidth: Sizes.readableWidth)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .onGeometryChange(for: CGPoint.self) { $0.frame(in: .global).origin } action: { contentOrigin = $0 }
    }

    private func unfolded(_ state: PlayerState) -> some View {
        let isShort = WindowLayout.isShort(height: size.height)
        let twoColumns = isShort || WindowLayout.isExpanded(width: size.width)
        let spacing = isShort ? Spacing.s : Spacing.l
        let layout = twoColumns ? AnyLayout(HStackLayout(spacing: Spacing.xl)) : AnyLayout(VStackLayout(spacing: spacing))
        return layout {
            if let episode = state.currentEpisode {
                cover(episode, fitted: twoColumns)
                    .matchedGeometryEffect(id: FoldPart.cover, in: foldTransition)
                // Beside the cover the details take their width first, up to a cap, and the cover gets the rest.
                details(state, episode, isShort: isShort)
                    .frame(maxWidth: twoColumns ? Self.detailsWidth : nil)
                    .layoutPriority(1)
                    .matchedGeometryEffect(id: FoldPart.details, in: foldTransition)
            } else {
                ContentUnavailableView(Res.string.shared.no_episode_selected.localized(), systemImage: "play.circle")
            }
        }
        .padding(isShort ? Spacing.s : Spacing.l)
        .frame(maxWidth: twoColumns ? Self.twoColumnsWidth : Sizes.readableWidth)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// The video takes the cover's place, as in the Compose player (ADR 0010); a fitted cover follows the space it gets.
    @ViewBuilder private func cover(_ episode: Episode, fitted: Bool) -> some View {
        if episode.isVideo {
            VideoView()
                .aspectRatio(Self.videoAspectRatio, contentMode: .fit)
                .background(.black)
                .clipShape(RoundedRectangle(cornerRadius: Radius.extraLarge))
        } else {
            // Square, so a fitted cover shrinks with the space the controls leave instead of pushing past the edges.
            Artwork(url: episode.imageUrl, size: fitted ? nil : Self.artworkSize, radius: Radius.extraLarge)
                .aspectRatio(1, contentMode: .fit)
                .frame(maxWidth: Self.artworkSize, maxHeight: Self.artworkSize)
        }
    }

    /// The titles, the progress, the controls and the options.
    private func details(_ state: PlayerState, _ episode: Episode, isShort: Bool) -> some View {
        let spacing = isShort ? Spacing.s : Spacing.l
        return VStack(spacing: spacing) {
            VStack(spacing: Spacing.xs) {
                Text(episode.title).textStyle(Typography.titleLarge).multilineTextAlignment(.center)
                    .lineLimit(textSize.isAccessibilitySize ? nil : isShort ? 1 : Self.titleLines)
                if let podcast = episode.podcastTitle {
                    Text(podcast).textStyle(Typography.labelLarge).foregroundStyle(Color(.Theme.accentText))
                }
            }
            progress(state)
            controls(state)
            // A low window keeps the options and the extras on one line.
            let optionsLayout = isShort ? AnyLayout(HStackLayout(spacing: Spacing.l)) : AnyLayout(VStackLayout(spacing: spacing))
            optionsLayout {
                options(state)
                extras(state)
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
        // The feed's length until the audio loads, as the Compose player does.
        let duration = Double(state.knownDurationMs())
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
            .foregroundStyle(Color(.Theme.onSurfaceVariant))
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

    /// Speed and the sleep timer, each a menu that shows what is chosen; one over the other at the accessibility text
    /// sizes, where side by side cuts them.
    private func options(_ state: PlayerState) -> some View {
        let layout = textSize.isAccessibilitySize ? AnyLayout(VStackLayout(spacing: Spacing.s)) : AnyLayout(HStackLayout(spacing: Spacing.l))
        return layout {
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

/// Tells whether a foldable is half open (iOS 27.1); always false before it and on devices without a hinge.
private struct HingeWatcher: ViewModifier {
    @Binding var halfOpen: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        // ponytail: Swift 6.4 stands for the iOS 27.1 SDK, the first with the hinge; the CI's Xcode 26 skips it.
        #if compiler(>=6.4)
            if #available(iOS 27.1, *) {
                content.onHingeChange { _, new in
                    let isHalfOpen = new.hinge?.status == .partiallyOpen
                    guard isHalfOpen != halfOpen else { return }
                    withAnimation(reduceMotion ? nil : .smooth) { halfOpen = isHalfOpen }
                }
            } else {
                content
            }
        #else
            content
        #endif
    }
}

private enum FoldPart { case cover, details }
