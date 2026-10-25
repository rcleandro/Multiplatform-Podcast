import Shared
import XCTest
@testable import Podcast

/// A podcast's own settings (25.8d): the same choices as the Compose sheet, in its words.
final class PodcastSettingsTests: XCTestCase {
    private let pt = Bundle(path: Bundle.main.path(forResource: "pt", ofType: "lproj")!)!

    func testTheSpeedStartsWithThePlayersOwn() {
        XCTAssertEqual("A mesma do player", PodcastSettingsTexts.speed(nil, bundle: pt))
        XCTAssertNil(PodcastSettingsTexts.speeds.first!)
        XCTAssertEqual(AppConfig.shared.PLAYBACK_SPEEDS.count + 1, PodcastSettingsTexts.speeds.count)
    }

    func testSkipsReadAsOffOrSeconds() {
        XCTAssertEqual(["Não", "10 s", "15 s", "30 s", "45 s", "60 s", "90 s", "120 s"],
                       PodcastSettingsTexts.skips.map { PodcastSettingsTexts.seconds($0, bundle: pt) })
    }

    func testAutoDownloadReadsAsOffOrTheNewest() {
        XCTAssertEqual(["Não", "O mais novo", "Os 3 mais novos", "Os 5 mais novos"],
                       PodcastSettingsTexts.autoDownloads.map { PodcastSettingsTexts.newest($0, bundle: pt) })
    }
}
