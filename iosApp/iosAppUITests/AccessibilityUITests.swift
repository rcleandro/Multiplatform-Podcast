import XCTest

/// VoiceOver and Dynamic Type (25.8f): the system's accessibility audit on each screen a new install reaches, at the
/// default text size and at the largest accessibility size.
final class AccessibilityUITests: AuditedTestCase {
    /// Whether text grows and fits is only known at a large size, so the default one leaves those checks to the other test.
    func testTheScreensPassTheAuditAtTheDefaultTextSize() throws {
        try auditTheScreens(textSize: nil, checks: XCUIAccessibilityAuditType.all.subtracting([.dynamicType, .textClipped]))
    }

    func testTheScreensPassTheAuditAtTheLargestTextSize() throws {
        try auditTheScreens(textSize: Self.largest, checks: .all)
    }

    private func auditTheScreens(textSize: String?, checks: XCUIAccessibilityAuditType) throws {
        let audit = launch(textSize: textSize, checks: checks)

        try audit()

        app.buttons["Settings"].firstMatch.tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))
        try audit()

        app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Listening stats'")).firstMatch.tap()
        XCTAssertTrue(app.navigationBars["Listening stats"].waitForExistence(timeout: 5))
        try audit()
        app.navigationBars["Listening stats"].buttons["Close"].tap()
        app.navigationBars["Settings"].buttons["Close"].tap()

        app.buttons["Episodes"].tap()
        XCTAssertTrue(app.searchFields.firstMatch.waitForExistence(timeout: 5))
        try audit()
    }
}

/// The same audit with a podcast: the directory it comes from, its page, its episode playing in the player, the
/// player's queue and the library with them, after subscribing to the first popular podcast of the directory, at the
/// largest text size, where most can go wrong. A class of its own so it runs after the new install's tests (by name),
/// since it leaves the podcast behind.
final class PodcastAccessibilityUITests: AuditedTestCase {
    private static let swipesToTheDirectory = 6

    func testThePodcastAndThePlayerPassTheAudit() throws {
        let audit = launch(textSize: Self.largest, checks: .all)
        let subscribe = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Subscribe to '")).firstMatch
        // At this size the directory starts below the fold, and a list only makes the rows it shows.
        _ = app.buttons["Add Podcast"].waitForExistence(timeout: 30)
        for _ in 0..<Self.swipesToTheDirectory where !subscribe.waitForExistence(timeout: 5) { app.swipeUp() }
        guard subscribe.exists else {
            throw XCTSkip("Needs a new install and the directory, which needs the network.")
        }
        let title = String(subscribe.label.dropFirst("Subscribe to ".count))
        subscribe.tap()
        // The directory's rows, whose titles and authors must show whole.
        try audit()

        // The library shows it once its feed is read.
        let podcast = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", title)).firstMatch
        XCTAssertTrue(podcast.waitForExistence(timeout: 30))
        podcast.tap()
        let play = app.buttons["Latest"]
        XCTAssertTrue(play.waitForExistence(timeout: 30))
        try audit()

        play.tap()
        let miniPlayer = app.descendants(matching: .any).matching(identifier: "miniPlayer").firstMatch
        XCTAssertTrue(miniPlayer.waitForExistence(timeout: 10))
        miniPlayer.tap()
        XCTAssertTrue(app.buttons["Minimize"].waitForExistence(timeout: 5))
        try audit()

        app.buttons["Queue"].tap()
        XCTAssertTrue(app.navigationBars["Queue"].waitForExistence(timeout: 5))
        try audit()

        // The library with the podcast and the episode in "Continue listening".
        app.navigationBars["Queue"].buttons["Close"].tap()
        app.buttons["Minimize"].tap()
        app.navigationBars.buttons["BackButton"].tap()
        XCTAssertTrue(app.staticTexts["CONTINUE LISTENING"].waitForExistence(timeout: 5))
        try audit()
    }
}

/// Opens the app and audits its screens, leaving out what only the system draws.
class AuditedTestCase: XCTestCase {
    let app = XCUIApplication()

    static let largest = "UICTContentSizeCategoryAccessibilityXXXL"
    private static let tabBarMargin: CGFloat = 16
    /// Taller than a line of the smallest style (16 pt) at the default size can be: the text grew.
    private static let grownTextHeight: CGFloat = 64

    /// Opens the app at [textSize] and waits for its tabs; the result audits the screen showing.
    func launch(textSize: String?, checks: XCUIAccessibilityAuditType) -> () throws -> Void {
        app.launchArguments = ["-AppleLanguages", "(en)"]
            + (textSize.map { ["-UIPreferredContentSizeCategoryName", $0] } ?? [])
        app.launch()
        let library = app.buttons["Library"]
        let episodes = app.buttons["Episodes"]
        let hittable = expectation(for: NSPredicate(format: "isHittable == true"), evaluatedWith: episodes)
        wait(for: [hittable], timeout: 30)
        let tabBar = library.frame.union(episodes.frame)
        return { try self.app.performAccessibilityAudit(for: checks) { self.isTheSystems($0, under: tabBar) } }
    }

    /// What the app cannot change: the system's search field cuts its own placeholder, the glass buttons of a
    /// navigation bar are read against their own edge, a row scrolled under the glass tab bar or mini player against
    /// their blur, and one half off the screen against nothing. Also a section title in two lines that the audit calls
    /// fixed in size though it grew with the text ("CONTINUE LISTENING", checked on screen). On iOS 27 the audit also
    /// reports issues with no element, which come and go with what the live directory shows and cannot be traced, such
    /// as the light gray chevron of a picker opened as a page, and calls the queue's "Now playing" row clipped though
    /// its title and details show whole (checked on screen).
    private func isTheSystems(_ issue: XCUIAccessibilityAuditIssue, under tabBar: CGRect) -> Bool {
        guard let element = issue.element else { return true }
        let inNavigationBar = app.navigationBars.allElementsBoundByIndex.contains { $0.frame.contains(element.frame) }
        let miniPlayer = app.descendants(matching: .any).matching(identifier: "miniPlayer").firstMatch
        let bottomBars = miniPlayer.exists ? tabBar.union(miniPlayer.frame) : tabBar
        let grew = issue.auditType == .dynamicType && element.frame.height > Self.grownTextHeight
        return element.elementType == .searchField || inNavigationBar || !app.frame.contains(element.frame)
            || element.frame.intersects(bottomBars.insetBy(dx: 0, dy: -Self.tabBarMargin)) || grew
            || issue.auditType == .textClipped && isNowPlayingInTheQueue(element)
    }

    /// Above what comes next, "Up next" or the empty queue's message, while the queue shows.
    private func isNowPlayingInTheQueue(_ element: XCUIElement) -> Bool {
        guard app.navigationBars["Queue"].exists else { return false }
        let upNext = app.staticTexts.matching(NSPredicate(format: "label ==[c] 'Up next'")).firstMatch
        let next = [upNext, app.staticTexts["Nothing up next"]].first { $0.exists }
        return element.frame.maxY <= (next?.frame.minY ?? app.frame.maxY)
    }
}
