import Shared
import XCTest
@testable import Podcast

/// The guardians' PIN (25.8b, 26.6, 22.11): four digits only, and a wait that never reads shorter than it is.
final class GuardianTests: XCTestCase {
    private let pt = Bundle(path: Bundle.main.path(forResource: "pt", ofType: "lproj")!)!

    func testOnlyFourDigitsAreTaken() {
        XCTAssertEqual("1234", PinTexts.digits("12a3-45"))
    }

    func testTheWaitReadsInSecondsUpToAMinuteThenInWholeMinutesRoundedUp() {
        XCTAssertEqual("Muitos PINs errados. Tente de novo em 45 s", PinTexts.error(.wait, waitSeconds: 45, bundle: pt))
        XCTAssertEqual("Muitos PINs errados. Tente de novo em 60 s", PinTexts.error(.wait, waitSeconds: 60, bundle: pt))
        XCTAssertEqual("Muitos PINs errados. Tente de novo em 2 min", PinTexts.error(.wait, waitSeconds: 61, bundle: pt))
        XCTAssertEqual("PIN errado", PinTexts.error(.wrong, waitSeconds: 0, bundle: pt))
        XCTAssertEqual("Os PINs não são iguais", PinTexts.error(.mismatch, waitSeconds: 0, bundle: pt))
    }

    func testTheStepsAndTheBreakReminderHaveTheirWords() {
        XCTAssertEqual("Crie um PIN de 4 dígitos", PinTexts.title(.create, bundle: pt))
        XCTAssertEqual("Nunca", PinTexts.breakReminder(0, bundle: pt))
        XCTAssertEqual("A cada 90 min", PinTexts.breakReminder(90, bundle: pt))
    }
}
