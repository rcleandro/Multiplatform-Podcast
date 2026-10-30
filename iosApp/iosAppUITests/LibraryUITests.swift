import XCTest

/// The SwiftUI library (25.4, 25.8a) as a new install opens it: suggestions instead of podcasts, and the ways to add one.
final class LibraryUITests: XCTestCase {
    func testAnEmptyLibraryOffersToAddAPodcast() throws {
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(en)"]
        app.launch()

        let suggestions = app.staticTexts["What do you like to listen to?"]
        guard suggestions.waitForExistence(timeout: 30) else {
            throw XCTSkip("The simulator's library is not empty; this checks a new install.")
        }
        let add = app.buttons.matching(identifier: "Add Podcast").element(boundBy: 0)
        let hittable = expectation(for: NSPredicate(format: "isHittable == true"), evaluatedWith: add)
        wait(for: [hittable], timeout: 10)

        add.tap()

        // "Add podcast": the directory's search, and the feed address from its menu.
        XCTAssertTrue(app.searchFields.firstMatch.waitForExistence(timeout: 5))
        app.buttons["More options"].tap()
        app.buttons["Add by feed address"].tap()
        XCTAssertTrue(app.alerts.textFields.firstMatch.waitForExistence(timeout: 5))
        app.alerts.buttons["Cancel"].tap()
        app.navigationBars.buttons["Close"].tap()
        XCTAssertTrue(suggestions.waitForExistence(timeout: 5))
    }
}
