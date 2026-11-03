import SwiftUI
import WatchKit
import WatchShared

/**
 * The watch's player (25.14c), with the Wear OS look adapted to the Apple Watch (19.9e): the cover behind everything,
 * a ring along the screen's rounded edge for the progress, the Digital Crown for the volume, and the controls in glass
 * on watchOS 26. Speed, sleep timer and the queue sit in the bottom bar (19.9h). The output is chosen when playback
 * starts: the long-form audio session shows the system's picker.
 */
struct NowPlayingView: View {
    private let player = WatchPlayerHolder.shared
    @State private var state: WatchedFlow<PlayerState>
    @State private var sheet: Sheet?

    private static let ringWidth: CGFloat = 4
    /// The Apple Watch screen's corner, which the ring follows.
    private static let screenCorner: CGFloat = 40
    private static let playSize: CGFloat = 44
    private static let skipSize: CGFloat = 22
    private static let coverDimming = 0.7
    private static let coverBlur: CGFloat = 8

    init() {
        _state = State(initialValue: WatchedFlow(WatchPlayerHolder.shared.state, initial: WatchPlayerHolder.shared.state.value as! PlayerState))
    }

    var body: some View {
        let state = state.value
        ZStack {
            background(state.currentEpisode?.imageUrl)
            EdgeRing(corner: Self.screenCorner)
                .trim(from: 0, to: CGFloat(state.progress()))
                .stroke(Color(.Theme.primary), style: StrokeStyle(lineWidth: Self.ringWidth, lineCap: .round))
                .ignoresSafeArea()
                .accessibilityHidden(true)
            if let episode = state.currentEpisode {
                VStack(spacing: Spacing.xs) {
                    Text(episode.title).textStyle(Typography.titleSmall).lineLimit(2).multilineTextAlignment(.center)
                    if let podcast = episode.podcastTitle {
                        Text(podcast).textStyle(Typography.labelSmall).foregroundStyle(Color(.Theme.primary)).lineLimit(1)
                    }
                    controls(state)
                }
                .padding(.horizontal, Spacing.s)
            } else {
                Text(text("no_episode_selected")).textStyle(Typography.bodyMedium).multilineTextAlignment(.center)
            }
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) { CrownVolume() }
            ToolbarItemGroup(placement: .bottomBar) {
                Button { sheet = .speed } label: {
                    Text(speedLabel(state.speed)).textStyle(Typography.labelMedium).foregroundStyle(Color(.Theme.onPrimary))
                }
                    .accessibilityLabel(text("playback_speed"))
                Button { sheet = .timer } label: {
                    Image(systemName: state.sleepTimer == nil ? "moon" : "moon.fill").foregroundStyle(Color(.Theme.onPrimary))
                }
                    .accessibilityLabel(text("sleep_timer"))
                Button { sheet = .queue } label: { Image(systemName: "list.bullet").foregroundStyle(Color(.Theme.onPrimary)) }
                    .accessibilityLabel(text("queue_short"))
            }
        }
        .sheet(item: $sheet) { sheet in
            switch sheet {
            case .speed: SpeedSheet(current: state.speed)
            case .timer: TimerSheet(isRunning: state.sleepTimer != nil)
            case .queue: QueueSheet(queue: state.queue)
            }
        }
    }

    private func background(_ url: String?) -> some View {
        AsyncImage(url: url.flatMap(URL.init(string:))) { image in
            image.resizable().scaledToFill().blur(radius: Self.coverBlur)
        } placeholder: {
            Color.black
        }
        .overlay(Color.black.opacity(Self.coverDimming))
        .ignoresSafeArea()
        .accessibilityHidden(true)
    }

    private func controls(_ state: PlayerState) -> some View {
        HStack(spacing: Spacing.m) {
            control("gobackward", text("skip_backward", Int64(state.skipIntervals.backwardSeconds)), size: Self.skipSize) {
                player.skipBackward()
            }
            control(state.isPlaying ? "pause.fill" : "play.fill", text(state.isPlaying ? "pause" : "play"), size: Self.skipSize) {
                player.playPause()
            }
            .frame(width: Self.playSize, height: Self.playSize)
            control("goforward", text("skip_forward", Int64(state.skipIntervals.forwardSeconds)), size: Self.skipSize) {
                player.skipForward()
            }
        }
    }

    @ViewBuilder
    private func control(_ symbol: String, _ label: String, size: CGFloat, action: @escaping () -> Void) -> some View {
        let button = Button(action: action) { Image(systemName: symbol).font(.system(size: size)) }.accessibilityLabel(label)
        if #available(watchOS 26.0, *) {
            button.buttonStyle(.glass)
        } else {
            button.buttonStyle(.bordered)
        }
    }

    enum Sheet: Identifiable {
        case speed, timer, queue
        var id: Self { self }
    }
}

/// The screen's rounded edge, from the top's middle and clockwise, so the progress fills it as a clock would.
struct EdgeRing: Shape {
    let corner: CGFloat

    func path(in rect: CGRect) -> Path {
        let radius = min(corner, rect.width / 2, rect.height / 2)
        var path = Path()
        path.move(to: CGPoint(x: rect.midX, y: rect.minY))
        path.addArc(tangent1End: CGPoint(x: rect.maxX, y: rect.minY), tangent2End: CGPoint(x: rect.maxX, y: rect.maxY), radius: radius)
        path.addArc(tangent1End: CGPoint(x: rect.maxX, y: rect.maxY), tangent2End: CGPoint(x: rect.minX, y: rect.maxY), radius: radius)
        path.addArc(tangent1End: CGPoint(x: rect.minX, y: rect.maxY), tangent2End: CGPoint(x: rect.minX, y: rect.minY), radius: radius)
        path.addArc(tangent1End: CGPoint(x: rect.minX, y: rect.minY), tangent2End: CGPoint(x: rect.midX, y: rect.minY), radius: radius)
        path.closeSubpath()
        return path
    }
}

/// "1×", "1.25×", as the phone writes it.
func speedLabel(_ speed: Float) -> String {
    (speed.rounded() == speed ? String(Int(speed)) : String(speed)) + "×"
}

/// The app's one player, made once.
enum WatchPlayerHolder {
    static let shared = WatchBridgeKt.watchPlayer()
}

extension WatchPlayer {
    /// Plays [episode], or pauses and resumes it when it is the one playing.
    func playNow(_ episode: Episode) {
        Task { try? await play(episode: episode) }
    }
}

/// The system's volume control, which the Digital Crown turns while the player shows (the Wear OS bezel, 19.9e).
struct CrownVolume: WKInterfaceObjectRepresentable {
    func makeWKInterfaceObject(context: Context) -> WKInterfaceVolumeControl {
        let control = WKInterfaceVolumeControl(origin: .local)
        control.setTintColor(UIColor(Color(.Theme.primary)))
        return control
    }

    func updateWKInterfaceObject(_ control: WKInterfaceVolumeControl, context: Context) {
        control.focus()
    }
}

private struct SpeedSheet: View {
    let current: Float
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        List(WatchPlayerHolder.shared.speeds.map(\.floatValue), id: \.self) { speed in
            Button {
                WatchPlayerHolder.shared.setSpeed(speed: speed)
                dismiss()
            } label: {
                HStack {
                    Text(speedLabel(speed)).textStyle(Typography.bodyMedium)
                    Spacer()
                    if speed == current { Image(systemName: "checkmark").foregroundStyle(Color(.Theme.primary)) }
                }
            }
        }
        .navigationTitle(text("playback_speed"))
    }
}

private struct TimerSheet: View {
    let isRunning: Bool
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        List {
            if isRunning {
                Button(text("sleep_timer_extend")) {
                    WatchPlayerHolder.shared.extendSleepTimer()
                    dismiss()
                }
            }
            ForEach(WatchPlayerHolder.shared.timers, id: \.key) { option in
                Button(text(option.key)) {
                    WatchPlayerHolder.shared.setSleepTimer(timer: option.timer)
                    dismiss()
                }
            }
        }
        .navigationTitle(text("sleep_timer"))
    }
}

private struct QueueSheet: View {
    let queue: [Episode]
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Group {
            if queue.isEmpty {
                Text(text("queue_empty_title")).textStyle(Typography.bodyMedium)
            } else {
                List(queue, id: \.id) { episode in
                    Button {
                        Task { try? await WatchPlayerHolder.shared.play(episode: episode) }
                        dismiss()
                    } label: {
                        HStack(spacing: Spacing.s) {
                            Thumbnail(url: episode.imageUrl)
                            Text(episode.title).textStyle(Typography.bodyMedium).lineLimit(2)
                        }
                    }
                }
            }
        }
        .navigationTitle(text("queue_short"))
    }
}
