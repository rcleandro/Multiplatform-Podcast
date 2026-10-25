import XCTest

/// The SwiftUI shell (25.3), opened with its launch argument: the system's tab bar moves between the two tabs.
final class ShellUITests: XCTestCase {
    private let app = XCUIApplication()

    func testTheTabBarMovesBetweenLibraryAndEpisodes() {
        app.launchArguments = ["-SwiftUIShell", "YES", "-AppleLanguages", "(en)"]
        app.launch()

        // The floating tab bar of iOS 26 is not always a tabBars element, so its tabs are found as buttons.
        let library = app.buttons["Library"]
        let episodes = app.buttons["Episodes"]
        // A tap while the app is still starting is lost, so the tab must first take touches.
        waitUntil("isHittable == true", episodes, timeout: 30)
        XCTAssertTrue(library.isSelected)

        episodes.tap()
        waitUntil("isSelected == true", episodes)
        XCTAssertTrue(app.navigationBars["Episodes"].exists)
        // The Episodes tab is the SwiftUI screen now: its search field and its filters.
        XCTAssertTrue(app.searchFields.firstMatch.waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["New"].exists)

        library.tap()
        waitUntil("isSelected == true", library)
        XCTAssertTrue(app.navigationBars["Library"].exists)
    }

    private func waitUntil(_ predicate: String, _ element: XCUIElement, timeout: TimeInterval = 5) {
        let condition = expectation(for: NSPredicate(format: predicate), evaluatedWith: element)
        wait(for: [condition], timeout: timeout)
    }

    func testSettingsOpenOverTheTabsWithTheirStats() {
        app.launchArguments = ["-SwiftUIShell", "YES", "-AppleLanguages", "(en)"]
        app.launch()
        let settings = app.buttons["Settings"]
        waitUntil("isHittable == true", settings, timeout: 30)

        settings.tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Theme"].exists || app.staticTexts["Theme"].exists)

        // The row reads its title and what it shows, so it is found by how its label starts.
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Listening stats'")).firstMatch.tap()
        XCTAssertTrue(app.navigationBars["Listening stats"].waitForExistence(timeout: 5))
        app.navigationBars["Listening stats"].buttons["Close"].tap()
        app.navigationBars["Settings"].buttons["Close"].tap()
        XCTAssertTrue(app.navigationBars["Library"].waitForExistence(timeout: 5))
    }
}
