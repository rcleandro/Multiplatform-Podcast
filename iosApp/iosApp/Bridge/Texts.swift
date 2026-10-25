import Foundation
import Shared

// The shared code's texts in the app's language (ADR 0015, decision 3): view models hand out the composeResources
// key, and the text comes from Localizable.xcstrings, generated from the same files.

extension LibraryStringResource {
    func localized(_ args: [Any] = [], bundle: Bundle = .main) -> String {
        format(NSLocalizedString(key, bundle: bundle, comment: ""), args)
    }
}

extension LibraryPluralStringResource {
    /// The form for [count], which is also the first placeholder.
    func localized(count: Int, _ args: [Any] = [], bundle: Bundle = .main) -> String {
        format(NSLocalizedString(key, bundle: bundle, comment: ""), [count] + args)
    }
}

extension UiMessage {
    var localized: String {
        text.localized(args)
    }
}

/// Kotlin numbers arrive boxed and `%lld` needs the number itself; anything else goes in as its text for `%@`. No
/// locale: with one, numbers get a thousands separator ("2.026" for a year in Portuguese), which the Compose screens
/// never write; the plural form still comes from the catalog's own language.
private func format(_ format: String, _ args: [Any]) -> String {
    guard !args.isEmpty else { return format }
    let values: [CVarArg] = args.map { arg in
        if let number = arg as? NSNumber { number.int64Value } else { "\(arg)" as NSString }
    }
    return String(format: format, arguments: values)
}
