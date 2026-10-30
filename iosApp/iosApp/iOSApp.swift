import SwiftUI
import Shared

/// Hands background download events to the shared code; iOS relaunches the app to deliver finished downloads.
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        BackgroundDownloadsKt.handleBackgroundDownloadEvents(completionHandler: completionHandler)
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    init() {
        SwiftBridgeKt.startShared()
    }

    var body: some Scene {
        WindowGroup {
            AppShell()
                // Brazil's age signal (26.2): on opening and each time the app comes back to the front.
                .onChange(of: scenePhase) { _, phase in
                    if phase == .active { Task { await AgeSignals.read() } }
                }
        }
    }
}
