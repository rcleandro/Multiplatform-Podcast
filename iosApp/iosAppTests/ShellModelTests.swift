import Shared
import XCTest
@testable import Podcast

/// The shell's navigation (25.3, risk 2 of ADR 0015): SwiftUI hands back only what the person undid, and what the
/// root itself set never comes back to it as a new change.
@MainActor
final class ShellModelTests: XCTestCase {
    private var model: ShellModel!

    override func setUp() {
        model = ShellModel(saved: nil)
        model.root.onPodcastSelected(podcastId: "p1")
        model.root.onEpisodeSelected(episodeId: "e1", podcastId: "p1")
    }

    func testTheBackGestureClosesOnlyThePageItTookAway() {
        let path = model.path(.library)

        path.wrappedValue = Array(path.wrappedValue.dropLast())

        XCTAssertEqual(1, model.path(.library).wrappedValue.count)
    }

    func testAPathTheRootSetDoesNotComeBackAsAChange() {
        let path = model.path(.library)

        path.wrappedValue = path.wrappedValue
        path.wrappedValue = path.wrappedValue + [NSObject()]

        XCTAssertEqual(2, model.path(.library).wrappedValue.count)
    }

    func testClosingThePlayerByAGestureGoesBackOnce() {
        model.root.onPlayerClicked()
        let player = model.isOpen(\.isPlayerOpen)

        player.wrappedValue = false
        player.wrappedValue = false

        XCTAssertFalse(model.state.isPlayerOpen)
        XCTAssertEqual(2, model.path(.library).wrappedValue.count)
    }
}
