import Shared
import XCTest
@testable import Podcast

/// The shared values in words (25.4), as the Compose screens write them: in the language's own order and plurals.
final class FormatsTests: XCTestCase {
    func testHowLongAgoReadsInEachLanguage() {
        XCTAssertEqual("Há 3 dias", RelativeTimeDays(count: 3).localized(bundle: language("pt")))
        XCTAssertEqual("1 day ago", RelativeTimeDays(count: 1).localized(bundle: language("en")))
        XCTAssertEqual("Ontem", RelativeTimeYesterday.shared.localized(bundle: language("pt")))
    }

    func testADateKeepsEachLanguagesOrder() {
        let date = RelativeTimeDate(day: 5, month: 10, year: 2026)
        XCTAssertEqual("5/10/2026", date.localized(bundle: language("pt")))
        XCTAssertEqual("10/5/2026", date.localized(bundle: language("en")))
    }

    func testACategoryHasItsName() {
        XCTAssertEqual("Tecnologia", PodcastCategory.technology.localized(bundle: language("pt")))
    }

    private func language(_ code: String) -> Bundle {
        Bundle(path: Bundle.main.path(forResource: code, ofType: "lproj")!)!
    }
}
