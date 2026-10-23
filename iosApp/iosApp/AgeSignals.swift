import DeclaredAgeRange
import Shared
import StoreKit
import UIKit

/// Brazil's age signal for the ECA Digital (26.2, ADR 0012): the App Store's country and, only there, the age range
/// the person agrees to share, handed to the shared code each time the app comes to the front.
@MainActor
enum AgeSignals {
    private static let adultAge = 18

    static func read() async {
        let country = await Storefront.current?.countryCode
        var lower: KotlinInt?
        var upper: KotlinInt?
        if StoreAgeSignalSourceKt.isInBrazil(storeCountry: country), #available(iOS 26.0, *),
           let controller = frontController(),
           case .sharing(let range)? = try? await AgeRangeService.shared.requestAgeRange(
               ageGates: adultAge, in: controller
           ) {
            lower = range.lowerBound.map { KotlinInt(value: Int32($0)) }
            upper = range.upperBound.map { KotlinInt(value: Int32($0)) }
        }
        StoreAgeSignalSourceKt.updateAgeSignal(storeCountry: country, lower: lower, upper: upper)
    }

    private static func frontController() -> UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow?.rootViewController }
            .first
    }
}
