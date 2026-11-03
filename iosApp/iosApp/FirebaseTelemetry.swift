import FirebaseAnalytics
import FirebaseCore
import FirebaseCrashlytics
import Shared

/// Firebase for the shared code (25.15): Analytics and Crashlytics from the Swift package, which the Kotlin side reaches
/// through `FirebaseBridge`. Both start off (Info.plist) until the user's choice arrives, so a user who turned
/// telemetry off sends nothing on launch; calls before `configure` are dropped.
final class FirebaseTelemetry: FirebaseBridge {
    func configure() {
        FirebaseApp.configure()
    }

    func setCollectionEnabled(enabled: Bool) {
        guard FirebaseApp.app() != nil else { return }
        Analytics.setAnalyticsCollectionEnabled(enabled)
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(enabled)
        if !enabled { Crashlytics.crashlytics().deleteUnsentReports() }
    }

    func logEvent(name: String, params: [String: Any]?) {
        guard FirebaseApp.app() != nil else { return }
        Analytics.logEvent(name, parameters: params)
    }

    func log(message: String) {
        guard FirebaseApp.app() != nil else { return }
        Crashlytics.crashlytics().log(message)
    }

    func recordError(type: String, message: String, stack: String) {
        guard FirebaseApp.app() != nil else { return }
        let exception = ExceptionModel(name: type, reason: message)
        exception.stackTrace = stack.split(separator: "\n").map { StackFrame(symbol: String($0), file: "", line: 0) }
        Crashlytics.crashlytics().record(exceptionModel: exception)
    }
}
