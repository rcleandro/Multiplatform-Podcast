import Shared
import XCTest
@testable import Podcast

/// The podcast and episode pages (25.5): downloads as the rows show them, and descriptions with their links.
final class PodcastScreenTests: XCTestCase {
    func testADownloadShowsWhatTheDownloaderAndTheDatabaseSay() {
        XCTAssertEqual(.idle, EpisodeDownload(nil, isDownloaded: false))
        XCTAssertEqual(.downloaded, EpisodeDownload(nil, isDownloaded: true))
        XCTAssertEqual(.queued, EpisodeDownload(DownloadStatus.Queued(priority: 0), isDownloaded: false))
        XCTAssertEqual(.downloading(0.5), EpisodeDownload(DownloadStatus.Downloading(progress: 0.5, bytesDownloaded: 5, totalBytes: 10), isDownloaded: false))
        XCTAssertEqual(.downloaded, EpisodeDownload(DownloadStatus.Completed(localPath: "/a.mp3"), isDownloaded: false))
    }

    // An emoji takes two UTF-16 places: the link after it must still cover its own word.
    func testADescriptionKeepsItsLinkAndBoldInPlace() throws {
        let attributed = StyledHtmlKt.styledHtml(html: "🎙️ <b>New</b> on <a href=\"https://ex.com\">our site</a>").attributed

        let link = try XCTUnwrap(attributed.runs.first { $0.link != nil })
        XCTAssertEqual("our site", String(attributed[link.range].characters))
        XCTAssertEqual(URL(string: "https://ex.com"), link.link)
        let bold = try XCTUnwrap(attributed.runs.first { $0.inlinePresentationIntent == .stronglyEmphasized })
        XCTAssertEqual("New", String(attributed[bold.range].characters))
    }
}
