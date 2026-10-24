import Shared
import XCTest
@testable import Podcast

/// The bridge of ADR 0015 (25.2): a SwiftUI screen follows its view model, and the view model goes with the screen.
@MainActor
final class BridgeTests: XCTestCase {
    func testAScreenFollowsItsViewModelsState() {
        let scope = ScreenScope()
        let library = scope.viewModels.library()
        let state = Watched<LibraryUiState>(library.uiState)
        let before = state.value.layout

        library.onIntent(intent: LibraryIntentToggleLayout.shared)
        let changed = expectation(for: NSPredicate { _, _ in state.value.layout != before }, evaluatedWith: nil)
        wait(for: [changed], timeout: 5)

        library.onIntent(intent: LibraryIntentToggleLayout.shared) // the simulator's layout as it was
    }

    func testTheViewModelIsClearedWhenItsScreenGoes() {
        var scope: ScreenScope? = ScreenScope()
        let resource = Resource()
        scope?.viewModels.library().addCloseable(closeable: resource)

        scope = nil

        XCTAssertTrue(resource.isClosed)
    }
}

/// Something a view model holds and closes when it is cleared.
private final class Resource: NSObject, KotlinAutoCloseable {
    private(set) var isClosed = false

    func close() {
        isClosed = true
    }
}
