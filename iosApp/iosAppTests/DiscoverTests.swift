import Shared
import XCTest
@testable import Podcast

/// "Add podcast" (25.8a): the categories in the language's own order, and the OPML file read within its limit.
final class DiscoverTests: XCTestCase {
    func testTheCategoriesComeInTheLanguagesAlphabeticalOrder() {
        let portuguese = Bundle(path: Bundle.main.path(forResource: "pt", ofType: "lproj")!)!
        let names = DirectoryCategories.sorted(bundle: portuguese).map { $0.localized(bundle: portuguese) }

        XCTAssertEqual(PodcastCategory.entries.count, names.count)
        XCTAssertEqual(names.sorted { $0.localizedCaseInsensitiveCompare($1) == .orderedAscending }, names)
    }

    func testAnOpmlFileIsReadAndOneTooBigIsNot() throws {
        let small = FileManager.default.temporaryDirectory.appendingPathComponent("feeds.opml")
        try "<opml/>".write(to: small, atomically: true, encoding: .utf8)
        XCTAssertEqual("<opml/>", try OpmlFile.read(small))

        let big = FileManager.default.temporaryDirectory.appendingPathComponent("big.opml")
        FileManager.default.createFile(atPath: big.path, contents: nil)
        let handle = try FileHandle(forWritingTo: big)
        try handle.truncate(atOffset: UInt64(AppConfig.shared.MAX_TEXT_BYTES + 1))
        try handle.close()
        XCTAssertNil(try OpmlFile.read(big))
    }
}
