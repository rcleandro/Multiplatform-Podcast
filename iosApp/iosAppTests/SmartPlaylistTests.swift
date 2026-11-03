import Shared
import XCTest
@testable import Podcast

/// The smart playlist editor (25.8c, 19.4): what it saves is what was chosen, a blank name cannot be saved, and the
/// length choices read as the Compose ones do.
final class SmartPlaylistTests: XCTestCase {
    func testTheEditorSavesWhatWasChosenUnderTheSameId() {
        let saved = SmartPlaylist(id: 7, name: "News", category: .news, unplayedOnly: true, downloadedOnly: false,
                                  maxMinutes: KotlinInt(int: 30))
        var draft = PlaylistDraft(saved)
        draft.maxMinutes = nil
        draft.downloadedOnly = true

        let edited = draft.playlist(id: saved.id)

        XCTAssertEqual(7, edited.id)
        XCTAssertEqual(PodcastCategory.news, edited.category)
        XCTAssertNil(edited.maxMinutes)
        XCTAssertTrue(edited.unplayedOnly && edited.downloadedOnly)
        XCTAssertEqual(KotlinInt(int: 30), PlaylistDraft(saved).playlist(id: 7).maxMinutes)
    }

    func testABlankNameCannotBeSaved() {
        var draft = PlaylistDraft(SmartPlaylist(id: 0, name: "  ", category: nil, unplayedOnly: false, downloadedOnly: false,
                                                maxMinutes: nil))
        XCTAssertFalse(draft.canSave)
        draft.name = "Short"
        XCTAssertTrue(draft.canSave)
    }

    func testTheLengthChoicesReadInTheUsersWords() {
        let pt = Bundle(path: Bundle.main.path(forResource: "pt", ofType: "lproj")!)!

        XCTAssertEqual(["Qualquer duração", "Até 15 min", "Até 30 min", "Até 60 min"],
                       PlaylistDraft.lengths.map { PlaylistDraft.lengthLabel($0, bundle: pt) })
    }
}
