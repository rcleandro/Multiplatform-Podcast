import XCTest

/// What the main screens do (25.8g), beyond the audit of 25.8f that only passes through them: "Add podcast"
/// subscribing from the directory, the Episodes tab's search and filters, an episode's page, an episode's menu and
/// the queue. Each test subscribes to the directory's first popular podcast when the library has none, which needs
/// the network, and puts back what it changed, so they run in any order and again on the same install.
final class ScreensUITests: XCTestCase {
    private let app = XCUIApplication()
    private var rows: XCUIElementQuery { app.buttons.matching(identifier: "episodeRow") }

    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launchArguments = ["-AppleLanguages", "(en)"]
        app.launch()
        waitUntil("isHittable == true", app.buttons["Episodes"], timeout: 30)
    }

    func testAddPodcastSubscribesFromTheDirectory() throws {
        let title = try subscribeFromTheDirectory()

        let podcast = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", title)).firstMatch
        XCTAssertTrue(podcast.waitForExistence(timeout: 30))
    }

    func testTheEpisodesTabSearchesAndFilters() throws {
        let episode = try openAllEpisodes()
        // The row reads its title first; up to a comma is whole words of it.
        let words = String(episode.label.prefix { $0 != "," })
        // Its longest plain word, since the simulator's keyboard drops accented letters ("#91: DE VIRADA É…").
        let word = Array(words).split { !($0.isASCII && $0.isLetter) }.map { String($0) }
            .max { $0.count < $1.count } ?? words

        // Lowercase: typing capitals taps Shift for each letter, and the simulator dropped most of "PERDOADAS".
        search(word.lowercased())
        XCTAssertTrue(row(containing: word).waitForExistence(timeout: 10))
        search("zzqx nothing matches this")
        XCTAssertTrue(app.staticTexts["No episodes found"].waitForExistence(timeout: 10))
        XCTAssertFalse(rows.firstMatch.exists)

        search("")
        app.buttons["Saved"].tap()
        XCTAssertFalse(row(containing: words).waitForExistence(timeout: 2))
        app.buttons["All"].tap()
        XCTAssertTrue(row(containing: words).waitForExistence(timeout: 10))
    }

    func testAnEpisodesPageSavesIt() throws {
        let episode = try openAllEpisodes()
        let words = String(episode.label.prefix { $0 != "," })

        episode.tap()
        XCTAssertTrue(app.buttons["Play"].waitForExistence(timeout: 10))
        let save = app.buttons["Save episode"]
        if !save.exists { app.buttons["Remove from saved"].tap() }
        app.buttons["Save episode"].tap()
        XCTAssertTrue(app.buttons["Remove from saved"].waitForExistence(timeout: 5))
        backToTheEpisodesTab()

        app.buttons["Saved"].tap()
        XCTAssertTrue(row(containing: words).waitForExistence(timeout: 10))
        row(containing: words).tap()
        app.buttons["Remove from saved"].tap()
        XCTAssertTrue(app.buttons["Save episode"].waitForExistence(timeout: 5))
        backToTheEpisodesTab()
        XCTAssertFalse(row(containing: words).waitForExistence(timeout: 2))
        app.buttons["All"].tap()
    }

    func testAnEpisodesMenuMarksItPlayed() throws {
        let episode = try openAllEpisodes()
        let words = String(episode.label.prefix { $0 != "," })
        // An earlier run may have left it played; the second tap puts it back as it was.
        XCTAssertTrue(menu(of: episode, has: "Mark as"))
        let first = app.buttons["Mark as unplayed"].exists ? "Mark as unplayed" : "Mark as played"
        let second = first == "Mark as played" ? "Mark as unplayed" : "Mark as played"
        app.buttons[first].tap()

        XCTAssertTrue(menu(of: row(containing: words), has: second))
        XCTAssertFalse(app.buttons[first].exists)
        app.buttons[second].tap()
        XCTAssertTrue(menu(of: row(containing: words), has: first))
        app.buttons[first].tap()
        XCTAssertTrue(menu(of: row(containing: words), has: second))
        app.buttons[second].tap()
    }

    func testTheQueueReordersAndClears() throws {
        _ = try openAllEpisodes()
        let labels = (0..<3).map { rows.element(boundBy: $0).label.prefix { $0 != "," } }.map(String.init)
        XCTAssertTrue(menu(of: row(containing: labels[0]), has: "Play"))
        app.buttons["Play"].firstMatch.tap()
        for title in labels.dropFirst() {
            XCTAssertTrue(menu(of: row(containing: title), has: "Add to queue"))
            app.buttons["Add to queue"].tap()
        }

        let miniPlayer = app.descendants(matching: .any).matching(identifier: "miniPlayer").firstMatch
        XCTAssertTrue(miniPlayer.waitForExistence(timeout: 10))
        miniPlayer.tap()
        app.buttons["Queue"].tap()
        XCTAssertTrue(app.navigationBars["Queue"].waitForExistence(timeout: 5))
        let second = row(containing: labels[1])
        let third = row(containing: labels[2])
        XCTAssertTrue(third.waitForExistence(timeout: 5))
        XCTAssertLessThan(second.frame.minY, third.frame.minY)

        // The system's handle, moved over the row above it.
        app.navigationBars["Queue"].buttons["Edit"].tap()
        let handle = reorderHandle(of: labels[2])
        let above = reorderHandle(of: labels[1])
        handle.press(forDuration: 0.5, thenDragTo: above)
        XCTAssertGreaterThan(second.frame.minY, third.frame.minY)
        app.navigationBars["Queue"].buttons["Done"].tap()

        app.buttons["Clear queue"].tap()
        app.buttons["Clear"].tap()
        XCTAssertTrue(app.staticTexts["Nothing up next"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["Clear queue"].exists)
        app.navigationBars["Queue"].buttons["Close"].tap()
        app.buttons["Minimize"].tap()
    }

    /// A phone on its side (21.2, 25.9): the player puts the cover beside the controls, so play shows without scrolling.
    func testThePlayerFitsAPhoneOnItsSide() throws {
        _ = try openAllEpisodes()
        XCTAssertTrue(menu(of: rows.firstMatch, has: "Play"))
        app.buttons["Play"].firstMatch.tap()
        let miniPlayer = app.descendants(matching: .any).matching(identifier: "miniPlayer").firstMatch
        XCTAssertTrue(miniPlayer.waitForExistence(timeout: 10))
        miniPlayer.tap()
        XCUIDevice.shared.orientation = .landscapeLeft
        defer { XCUIDevice.shared.orientation = .portrait }

        let pause = app.buttons["Pause"].firstMatch
        XCTAssertTrue(pause.waitForExistence(timeout: 10))
        XCTAssertTrue(pause.isHittable)
        let screen = app.windows.firstMatch.frame
        XCTAssertLessThanOrEqual(pause.frame.maxY, screen.maxY)
        XCTAssertLessThanOrEqual(app.buttons["Playback speed"].frame.maxY, screen.maxY)
        pause.tap()
        app.buttons["Minimize"].tap()
    }

    /// Subscribes to the first popular podcast not yet in the library and goes back to it; the result is its title.
    @discardableResult
    private func subscribeFromTheDirectory() throws -> String {
        app.buttons.matching(identifier: "Add Podcast").firstMatch.tap()
        XCTAssertTrue(app.searchFields.firstMatch.waitForExistence(timeout: 5))
        let subscribe = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Subscribe to '")).firstMatch
        guard subscribe.waitForExistence(timeout: 30) else {
            throw XCTSkip("The directory needs the network.")
        }
        let title = String(subscribe.label.dropFirst("Subscribe to ".count))
        subscribe.tap()
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label == 'Subscribed'"))
            .firstMatch.waitForExistence(timeout: 30))
        app.navigationBars.buttons["Close"].tap()
        return title
    }

    /// The Episodes tab on All, after subscribing to a podcast when the library has none; the result is its first row.
    private func openAllEpisodes() throws -> XCUIElement {
        if app.staticTexts["What do you like to listen to?"].waitForExistence(timeout: 5) {
            try subscribeFromTheDirectory()
        }
        app.buttons["Episodes"].tap()
        app.buttons["All"].tap()
        let first = rows.firstMatch
        XCTAssertTrue(first.waitForExistence(timeout: 30))
        return first
    }

    private func row(containing words: String) -> XCUIElement {
        rows.matching(NSPredicate(format: "label CONTAINS %@", words)).firstMatch
    }

    private func search(_ text: String) {
        let field = app.searchFields.firstMatch
        field.tap()
        if let typed = field.value as? String, !typed.isEmpty, typed != field.placeholderValue {
            field.buttons["Clear text"].tap()
        }
        // Return closes the keyboard, which an empty search keeps open.
        field.typeText(text + "\n")
    }

    /// Opens the row's menu by a long press; whether it holds an item that starts with [action].
    private func menu(of row: XCUIElement, has action: String) -> Bool {
        row.press(forDuration: 1)
        return app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", action)).firstMatch
            .waitForExistence(timeout: 5)
    }

    private func reorderHandle(of title: String) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Reorder' AND label CONTAINS %@", title)).firstMatch
    }

    /// An episode opens over its podcast's page, so back is pressed until the tab shows.
    private func backToTheEpisodesTab() {
        for _ in 0..<Self.pagesOverTheTab where !app.navigationBars["Episodes"].exists {
            app.navigationBars.buttons["BackButton"].tap()
        }
    }

    private static let pagesOverTheTab = 3

    private func waitUntil(_ predicate: String, _ element: XCUIElement, timeout: TimeInterval = 5) {
        let condition = expectation(for: NSPredicate(format: predicate), evaluatedWith: element)
        wait(for: [condition], timeout: timeout)
    }
}
