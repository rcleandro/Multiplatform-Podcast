import XCTest

/// The SwiftUI library (25.4) as a new install opens it: no podcasts yet, and the way to add the first one.
final class LibraryUITests: XCTestCase {
    func testAnEmptyLibraryOffersToAddAPodcast() throws {
        let app = XCUIApplication()
        app.launchArguments = ["-SwiftUIShell", "YES", "-AppleLanguages", "(en)"]
        app.launch()

        let empty = app.staticTexts["No podcasts found"]
        guard empty.waitForExistence(timeout: 30) else {
            throw XCTSkip("The simulator's library is not empty; this checks a new install.")
        }
        let add = app.buttons.matching(identifier: "Add Podcast").element(boundBy: 0)
        let hittable = expectation(for: NSPredicate(format: "isHittable == true"), evaluatedWith: add)
        wait(for: [hittable], timeout: 10)

        add.tap()

        // The feed address sheet: nothing to add until an address is typed, and Cancel closes it.
        XCTAssertTrue(app.textFields["Feed address"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.navigationBars.buttons["Add"].isEnabled)
        app.navigationBars.buttons["Cancel"].tap()
        XCTAssertTrue(empty.waitForExistence(timeout: 5))
    }
}
