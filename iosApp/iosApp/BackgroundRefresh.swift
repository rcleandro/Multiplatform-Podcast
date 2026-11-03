import BackgroundTasks
import Shared
import UserNotifications

/// Looks for new episodes while the app is closed (25.11), as the Android RefreshWorker does (18.18): iOS wakes the app
/// around the interval Settings chose, when it judges best for the battery, and may skip a turn. What arrived goes to
/// a notification, unless Settings, the podcast or the guardians say otherwise.
enum BackgroundRefresh {
    static let taskId = "br.com.carvalho.podcast.refresh"
    /// Every feed failing is most likely the network: ask again sooner than a whole interval.
    private static let retryAfter: TimeInterval = 15 * 60
    private static let secondsPerHour: TimeInterval = 60 * 60
    /// One at a time: a newer look replaces what the last one said.
    private static let notificationId = "new-episodes"

    /// Asks for the next look a whole interval from now, or after [delay]; "never" cancels it.
    static func schedule(after delay: TimeInterval? = nil) {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: taskId)
        let hours = BackgroundRefresh_iosKt.refreshIntervalHours()
        guard hours > 0 else { return }
        let request = BGAppRefreshTaskRequest(identifier: taskId)
        request.earliestBeginDate = Date(timeIntervalSinceNow: delay ?? Double(hours) * secondsPerHour)
        try? BGTaskScheduler.shared.submit(request)
    }

    /// One look, the next one asked for, and a notification for what arrived.
    static func run() async {
        guard let result = try? await BackgroundRefresh_iosKt.refreshInBackground() else {
            schedule(after: retryAfter)
            return
        }
        schedule(after: result.allFailed ? retryAfter : nil)
        guard !result.toNotify.isEmpty else { return }
        let center = UNUserNotificationCenter.current()
        let status = await center.notificationSettings().authorizationStatus
        guard status == .authorized || status == .provisional else { return }
        let message = message(result.toNotify)
        let content = UNMutableNotificationContent()
        content.title = message.title
        content.body = message.body
        content.sound = .default
        try? await center.add(UNNotificationRequest(identifier: notificationId, content: content, trigger: nil))
    }

    /**
     * Provisional permission, with no alert: the first notices arrive quietly in Notification Center, where the person
     * keeps or turns them off. New episodes are not urgent enough to ask on opening; a person who already answered
     * keeps their answer.
     */
    static func askToNotify() {
        guard BackgroundRefresh_iosKt.refreshIntervalHours() > 0 else { return }
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .provisional]) { _, _ in }
    }

    /// The episode itself (its podcast and title), or how many and from which podcasts.
    static func message(_ episodes: [Episode], bundle: Bundle = .main) -> (title: String, body: String) {
        let count = Res.plurals.shared.new_episodes_count.localized(count: episodes.count, bundle: bundle)
        if episodes.count == 1, let episode = episodes.first {
            return (episode.podcastTitle ?? count, episode.title)
        }
        var podcasts: [String] = []
        for title in episodes.compactMap(\.podcastTitle) where !podcasts.contains(title) { podcasts.append(title) }
        return (count, podcasts.joined(separator: ", "))
    }
}
