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

    private func hex(_ color: UIColor) -> String {
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
        color.getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        return [red, green, blue].map { String(format: "%02X", Int(($0 * 255).rounded())) }.joined()
    }
}
