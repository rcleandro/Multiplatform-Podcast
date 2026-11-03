import Observation
import Shared
import SwiftUI

/// The SwiftUI shell's side of the navigation (ADR 0015, decision 2): the shared `RootComponent` decides, and this
/// only mirrors its state for SwiftUI and hands back what the person did.
@MainActor
@Observable
final class ShellModel {
    let navigation: SwiftNavigation
    private(set) var state: NavigationState
    /// The view models that live as long as the app, like the activity's in the Compose app.
    let app = ScreenScope()
    let hasEpisode: Watched<Bool>
    @ObservationIgnored private var cancellation: DecomposeCancellation?

    init(saved: String?) {
        navigation = SwiftNavigation(saved: saved)
        state = navigation.root.state.value
        hasEpisode = Watched(app.viewModels.player().hasEpisode())
        cancellation = navigation.root.state.subscribe { [weak self] next in self?.state = next }
    }

    deinit {
        cancellation?.cancel()
    }

    var root: RootComponent { navigation.root }

    var selectedTab: Binding<Shared.Tab> {
        Binding(get: { self.state.selectedTab }, set: { self.root.onTabClicked(tab: $0) })
    }

    /// The pages open on [tab], as the `NavigationStack` shows them.
    func path(_ tab: Shared.Tab) -> Binding<[Page]> {
        Binding(get: { self.stack(tab) }, set: { self.setPath(tab, to: $0) })
    }

    /// The pages open on [tab] past the first, which a wide window shows as the root of its detail column.
    func detailPath(_ tab: Shared.Tab) -> Binding<[Page]> {
        Binding(get: { Array(self.stack(tab).dropFirst()) },
                set: { self.setPath(tab, to: Array(self.stack(tab).prefix(1)) + $0) })
    }

    /// The pages open on [tab].
    func pages(_ tab: Shared.Tab) -> [Page] { stack(tab) }

    /**
     * SwiftUI only shortens a path (the back gesture, the back button, tapping the selected tab); pages are pushed by
     * the root, so a path as long as the root's, or longer, is the root's own and changes nothing. Without this rule
     * the path the root just set would come back as a new change, and go round.
     */
    func setPath(_ tab: Shared.Tab, to path: [Page]) {
        let open = stack(tab).count
        guard tab == state.selectedTab, path.count < open else { return }
        for _ in path.count..<open { root.onBackClicked() }
    }

    /// Whether [keyPath] is open over the tabs; closing it by a gesture is the same as going back.
    func isOpen(_ keyPath: KeyPath<NavigationState, Bool>) -> Binding<Bool> {
        Binding(get: { self.state[keyPath: keyPath] }, set: { open in
            if !open && self.state[keyPath: keyPath] { self.root.onBackClicked() }
        })
    }

    private func stack(_ tab: Shared.Tab) -> [Page] {
        (state.stacks[tab] ?? []).compactMap(Page.init)
    }
}

/// A page on a tab's stack. A Swift value, because the Kotlin objects in a `[AnyHashable]` path never reached
/// `navigationDestination(for:)` on iOS 27, which showed a warning sign instead of the page.
enum Page: Hashable {
    case podcast(String)
    case episode(String)

    init?(_ detail: Detail) {
        switch detail {
        case let podcast as DetailPodcast: self = .podcast(podcast.podcastId)
        case let episode as DetailEpisode: self = .episode(episode.episodeId)
        default: return nil
        }
    }
}
