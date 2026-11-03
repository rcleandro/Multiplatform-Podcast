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
        SwiftBridgeKt.startShared(firebase: FirebaseTelemetry())
        WidgetSync.start()
        LibrarySync.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            AppShell()
                // Brazil's age signal (26.2): on opening and each time the app comes back to the front.
                .onChange(of: scenePhase) { _, phase in
                    switch phase {
                    case .active:
                        Task { await AgeSignals.read() }
                        BackgroundRefresh.askToNotify()
                    // The next look in the background (25.11), from the interval Settings have now.
                    case .background: BackgroundRefresh.schedule()
                    default: break
                    }
                }
        }
        .backgroundTask(.appRefresh(BackgroundRefresh.taskId)) { await BackgroundRefresh.run() }
    }
}
