import Foundation
import Shared

// The shared code's values in words, as the Compose screens write them (core/ui/presentation/format).

extension RelativeTime {
    func localized(bundle: Bundle = .main) -> String {
        switch self {
        case let minutes as RelativeTimeMinutes:
            Res.plurals.shared.time_minutes_ago.localized(count: Int(minutes.count), bundle: bundle)
        case let hours as RelativeTimeHours:
            Res.plurals.shared.time_hours_ago.localized(count: Int(hours.count), bundle: bundle)
        case let days as RelativeTimeDays:
            Res.plurals.shared.time_days_ago.localized(count: Int(days.count), bundle: bundle)
        case let date as RelativeTimeDate:
            Res.string.shared.date_short.localized([date.day, date.month, date.year].map { KotlinInt(int: $0) }, bundle: bundle)
        case is RelativeTimeYesterday:
            Res.string.shared.time_yesterday.localized(bundle: bundle)
        default:
            Res.string.shared.time_just_now.localized(bundle: bundle)
        }
    }
}

/// How long ago [timestamp] (epoch ms) was, or nil when the feed gave no date.
func relativeTime(_ timestamp: Int64) -> String? {
    SwiftBridgeKt.relativeTimeNow(timestampMs: timestamp)?.localized()
}

extension PodcastCategory {
    func localized(bundle: Bundle = .main) -> String { label.localized(bundle: bundle) }
}
