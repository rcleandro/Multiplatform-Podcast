import Shared
import XCTest
@testable import Podcast

/// Reporting (25.8c, 26.7): the email names the reason, the podcast and the episode, and only the feed's host.
final class ReportTests: XCTestCase {
    private let en = Bundle(path: Bundle.main.path(forResource: "en", ofType: "lproj")!)!

    func testAReportOfAPrivateFeedCarriesOnlyItsHost() throws {
        let target = ReportTarget(podcastTitle: "Paid show", feedUrl: "https://ana:s3cret@feeds.example.com/private?key=abc123",
                                  episodeTitle: "Episode 5")

        let url = try XCTUnwrap(ReportTexts.mailto(target, reason: .gambling, bundle: en))
        let mail = try XCTUnwrap(url.absoluteString.removingPercentEncoding)

        XCTAssertTrue(mail.hasPrefix("mailto:"))
        XCTAssertTrue(mail.contains("Report: Episode 5"))
        XCTAssertTrue(mail.contains("Podcast: Paid show"))
        XCTAssertTrue(mail.contains("feeds.example.com"))
        XCTAssertFalse(mail.contains("s3cret"))
        XCTAssertFalse(mail.contains("abc123"))
    }
}
