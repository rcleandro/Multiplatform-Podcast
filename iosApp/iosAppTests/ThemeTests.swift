import UIKit
import XCTest
@testable import Podcast

/// The theme of ADR 0015 (25.2): the copies IosTokensTest keeps of the Kotlin theme reach the app.
final class ThemeTests: XCTestCase {
    func testTheOnestFontsAreInTheApp() {
        ["Onest-Regular", "Onest-Medium", "Onest-SemiBold", "Onest-Bold"].forEach { name in
            XCTAssertNotNil(UIFont(name: name, size: Typography.bodyLarge.size), name)
        }
    }

    func testAColorFollowsTheLightAndDarkTheme() throws {
        let primary = try XCTUnwrap(UIColor(named: "Theme/primary"))
        XCTAssertEqual("BC7300", hex(primary.resolvedColor(with: UITraitCollection(userInterfaceStyle: .light))))
        XCTAssertEqual("F2A93B", hex(primary.resolvedColor(with: UITraitCollection(userInterfaceStyle: .dark))))
    }

    // The cover's color behind the podcast page, the episode page and the player, as ArtworkColorTest in Kotlin.
    private let white = RGB(red: 1, green: 1, blue: 1), black = RGB(red: 0, green: 0, blue: 0)
    private let grey = RGB(red: 0.5, green: 0.5, blue: 0.5)

    func testTheCoverColorIgnoresGreysWhiteAndBlack() throws {
        let teal = RGB(red: 0.1, green: 0.6, blue: 0.6)
        let pixels = Array(repeating: teal, count: 30) + Array(repeating: white, count: 40)
            + Array(repeating: black, count: 20) + Array(repeating: grey, count: 10)
        let color = try XCTUnwrap(ArtworkColor.dominant(pixels))
        XCTAssertEqual(teal.red, color.red, accuracy: 1e-9)
        XCTAssertEqual(teal.green, color.green, accuracy: 1e-9)
        XCTAssertEqual(teal.blue, color.blue, accuracy: 1e-9)
    }

    func testABlackAndWhiteCoverHasNoColor() {
        XCTAssertNil(ArtworkColor.dominant(Array(repeating: white, count: 50) + Array(repeating: black, count: 50)
            + Array(repeating: grey, count: 50)))
    }

    func testTheTintKeepsTextReadableOrGivesUp() throws {
        let background = RGB(red: 0x16 / 255, green: 0x15 / 255, blue: 0x13 / 255)
        let text = RGB(red: 0xF1 / 255, green: 0xEF / 255, blue: 0xEA / 255)
        let tint = try XCTUnwrap(ArtworkColor.tint(RGB(red: 0.9, green: 0.3, blue: 0.1), background: background, foreground: text))
        XCTAssertGreaterThanOrEqual(text.contrast(with: tint), 4.5)
        XCTAssertNotEqual(background, tint)
        XCTAssertNil(ArtworkColor.tint(RGB(red: 1, green: 1, blue: 0), background: white, foreground: text))
    }

    private func hex(_ color: UIColor) -> String {
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
        color.getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        return [red, green, blue].map { String(format: "%02X", Int(($0 * 255).rounded())) }.joined()
    }
}
