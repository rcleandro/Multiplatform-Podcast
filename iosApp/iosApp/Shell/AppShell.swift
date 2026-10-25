import Shared
import SwiftUI

/// The SwiftUI app (ADR 0015) while its screens are written (25.3 to 25.7). It opens only with the launch argument
/// `-SwiftUIShell YES`; until the screens are done, the app opens the Compose one, and the shell shows stand-ins.
struct AppShell: View {
    static var isOn: Bool { UserDefaults.standard.bool(forKey: "SwiftUIShell") }

    @SceneStorage("navigation") private var saved: String?
    @Environment(\.scenePhase) private var scenePhase
    @State private var model: ShellModel?

    var body: some View {
        if let model {
            Shell(model: model)
                .onChange(of: scenePhase) { _, phase in
                    if phase != .active { saved = model.navigation.save() }
                }
        } else {
            Color.clear.onAppear { model = ShellModel(saved: saved) }
        }
    }
}

private struct Shell: View {
    let model: ShellModel

    var body: some View {
        TabView(selection: model.selectedTab) {
            SwiftUI.Tab(Res.string.shared.library_title.localized(), systemImage: "play.square.stack", value: Shared.Tab.library) {
                TabStack(model: model, tab: .library) { LibraryStandIn(model: model) }
            }
            SwiftUI.Tab(Res.string.shared.episodes_tab.localized(), systemImage: "list.bullet.rectangle.portrait", value: Shared.Tab.episodes) {
                TabStack(model: model, tab: .episodes) { EpisodesStandIn() }
            }
        }
        .miniPlayer(model: model)
        .fullScreenCover(isPresented: model.isOpen(\.isPlayerOpen)) {
            PlayerStandIn(model: model)
                .sheet(isPresented: model.isOpen(\.isQueueOpen)) { StandIn(symbol: "list.number") }
                .sheet(isPresented: model.isOpen(\.isTranscriptOpen)) { StandIn(symbol: "text.quote") }
        }
        .sheet(isPresented: model.isOpen(\.isOrganizingLibrary)) { StandIn(symbol: "arrow.up.arrow.down") }
        .sheet(isPresented: model.isOpen(\.isAddingPodcast)) { StandIn(symbol: "plus") }
        .sheet(isPresented: model.isOpen(\.isShowingSettings)) {
            StandIn(symbol: "gearshape")
                .sheet(isPresented: model.isOpen(\.isShowingStats)) { StandIn(symbol: "chart.bar") }
        }
    }
}

/// One tab's pages: the podcast and the episode pages the root has open on it.
private struct TabStack<Root: View>: View {
    let model: ShellModel
    let tab: Shared.Tab
    @ViewBuilder let root: () -> Root

    var body: some View {
        NavigationStack(path: model.path(tab)) {
            root()
                .navigationDestination(for: DetailPodcast.self) { StandIn(symbol: "square.stack", detail: $0.podcastId) }
                .navigationDestination(for: DetailEpisode.self) { StandIn(symbol: "waveform", detail: $0.episodeId) }
        }
    }
}

private extension View {
    /// The mini player: the tab bar's accessory on iOS 26, with its glass, and a bar of its own above the tabs before.
    @ViewBuilder
    func miniPlayer(model: ShellModel) -> some View {
        if !model.hasEpisode.value {
            self
        } else if #available(iOS 26.0, *) {
            tabViewBottomAccessory { MiniPlayerBar(model: model) }
        } else {
            safeAreaInset(edge: .bottom) { MiniPlayerBar(model: model).background(.bar) }
        }
    }
}
