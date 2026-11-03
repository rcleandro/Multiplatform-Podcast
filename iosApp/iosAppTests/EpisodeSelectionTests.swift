import Shared
import XCTest
@testable import Podcast

/// Acting on several episodes (25.8c, 18.11): the bar offers the same four things the Compose one does, in its words.
final class EpisodeSelectionTests: XCTestCase {
    func testTheBarOffersEveryActionInTheMenusOwnWords() {
        let pt = Bundle(path: Bundle.main.path(forResource: "pt", ofType: "lproj")!)!

        XCTAssertEqual(["Adicionar à fila", "Baixar", "Marcar como ouvido", "Marcar como não ouvido"],
                       EpisodeAction.entries.map { EpisodeSelectionTexts.label($0, bundle: pt) })
        XCTAssertEqual(4, Set(EpisodeAction.entries.map(EpisodeSelectionTexts.symbol)).count)
    }
}
