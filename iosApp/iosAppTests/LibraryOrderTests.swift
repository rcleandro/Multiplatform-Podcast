import XCTest
@testable import Podcast

/// Moving a podcast without dragging (25.4): VoiceOver's "move up" and "move down" in "Organize library".
final class LibraryOrderTests: XCTestCase {
    func testAPodcastMovesOnePlace() {
        XCTAssertEqual(["b", "a", "c"], LibraryOrder.moving(["a", "b", "c"], at: 1, by: -1))
        XCTAssertEqual(["a", "c", "b"], LibraryOrder.moving(["a", "b", "c"], at: 1, by: 1))
    }

    func testTheEndsDoNotMoveOutOfTheList() {
        XCTAssertNil(LibraryOrder.moving(["a", "b"], at: 0, by: -1))
        XCTAssertNil(LibraryOrder.moving(["a", "b"], at: 1, by: 1))
    }
}
