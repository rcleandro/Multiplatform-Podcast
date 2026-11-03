import Shared
import XCTest
@testable import Podcast

/// The texts of ADR 0015 (25.2): the catalog generated from composeResources answers the keys the view models use.
final class TextsTests: XCTestCase {
    func testATextComesInEachLanguage() {
        XCTAssertEqual("Library", Res.string.shared.library_title.localized(bundle: language("en")))
        XCTAssertEqual("Biblioteca", Res.string.shared.library_title.localized(bundle: language("pt")))
        XCTAssertEqual("Biblioteca", Res.string.shared.library_title.localized(bundle: language("es")))
    }

    func testPlaceholdersAndPluralsAreFilledIn() {
        XCTAssertEqual("Nenhum trecho com “juros”", Res.string.shared.transcript_no_match.localized(["juros"], bundle: language("pt")))
        XCTAssertEqual("1 episódio", Res.plurals.shared.podcast_episode_count.localized(count: 1, bundle: language("pt")))
        XCTAssertEqual("3 episódios", Res.plurals.shared.podcast_episode_count.localized(count: 3, bundle: language("pt")))
        XCTAssertEqual("3 episodios", Res.plurals.shared.podcast_episode_count.localized(count: 3, bundle: language("es")))
    }

    func testAMessageFromAViewModelReadsWithItsValues() {
        let message = UiMessage(text: Res.string.shared.bookmark_add, args: ["12:30"])
        XCTAssertEqual("Mark 12:30", message.text.localized(message.args, bundle: language("en")))

        // A number from Kotlin comes boxed (KotlinInt), not as a Swift Int.
        let season = UiMessage(text: Res.string.shared.season_number, args: [KotlinInt(int: 3)])
        XCTAssertEqual("Season 3", season.text.localized(season.args, bundle: language("en")))
    }

    private func language(_ code: String) -> Bundle {
        Bundle(path: Bundle.main.path(forResource: code, ofType: "lproj")!)!
    }
}
