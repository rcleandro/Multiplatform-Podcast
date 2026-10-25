import Shared
import XCTest
@testable import Podcast

/// The transcript (25.8e, 19.2): above a line, its time only while searching and who says it when named.
final class TranscriptTests: XCTestCase {
    func testALineShowsItsTimeOnlyWhileSearchingAndItsSpeakerWhenNamed() {
        let named = TranscriptLine(startMs: 65_000, endMs: 70_000, text: "Oi", speaker: "Ana")
        let unnamed = TranscriptLine(startMs: 65_000, endMs: 70_000, text: "Oi", speaker: nil)

        XCTAssertEqual("Ana", TranscriptRows.label(named, searching: false))
        XCTAssertEqual("1:05 · Ana", TranscriptRows.label(named, searching: true))
        XCTAssertEqual("1:05", TranscriptRows.label(unnamed, searching: true))
        XCTAssertNil(TranscriptRows.label(unnamed, searching: false))
    }

    func testBlankWordsAreNoSearch() {
        XCTAssertFalse(TranscriptRows.isSearching(" \n"))
        XCTAssertTrue(TranscriptRows.isSearching(" oi "))
    }
}
