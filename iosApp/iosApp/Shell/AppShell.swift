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
        Group {
            if let model {
                Shell(model: model)
                    .onChange(of: scenePhase) { _, phase in
                        if phase != .active { saved = model.navigation.save() }
                    }
            } else {
                Color.clear.onAppear { model = ShellModel(saved: saved) }
            }
        }
        // A podcast link (pcast://, podcast://, itpc://, feed://) opens in "Add podcast" (18.4).
        .onOpenURL { url in
            if model == nil { model = ShellModel(saved: saved) }
            model?.root.onPodcastLink(link: url.absoluteString)
        }
    }
}

private struct Shell: View {
    let model: ShellModel
    @State private var theme = Watched<ThemeMode>(SwiftBridgeKt.themeMode())

    var body: some View {
        TabView(selection: model.selectedTab) {
            SwiftUI.Tab(Res.string.shared.library_title.localized(), systemImage: "play.square.stack", value: Shared.Tab.library) {
                TabStack(model: model, tab: .library) { LibraryScreen(model: model) }
            }
            SwiftUI.Tab(Res.string.shared.episodes_tab.localized(), systemImage: "list.bullet.rectangle.portrait", value: Shared.Tab.episodes) {
                TabStack(model: model, tab: .episodes) { EpisodesScreen(model: model) }
            }
        }
        .miniPlayer(model: model)
        .preferredColorScheme(theme.value.colorScheme)
        .fullScreenCover(isPresented: model.isOpen(\.isPlayerOpen)) {
            PlayerScreen(model: model)
                .sheet(isPresented: model.isOpen(\.isQueueOpen)) { QueueScreen(model: model) }
                .sheet(isPresented: model.isOpen(\.isTranscriptOpen)) { TranscriptScreen(model: model) }
        }
        .sheet(isPresented: model.isOpen(\.isOrganizingLibrary)) { LibraryOrderScreen(model: model) }
        .sheet(isPresented: model.isOpen(\.isAddingPodcast)) { DiscoverScreen(model: model) }
        .sheet(isPresented: model.isOpen(\.isShowingSettings)) {
            SettingsScreen(model: model)
                .sheet(isPresented: model.isOpen(\.isShowingStats)) { ListeningStatsScreen(model: model) }
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
                .navigationDestination(for: Page.self) { page in
                    switch page {
                    case let .podcast(id): PodcastScreen(model: model, podcastId: id)
                    case let .episode(id): EpisodeScreen(model: model, episodeId: id)
                    }
                }
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

private extension ThemeMode {
    /// Light or dark as chosen in Settings; nil follows the system.
    var colorScheme: ColorScheme? {
        switch self {
        case .light: .light
        case .dark: .dark
        default: nil
        }
    }
}
