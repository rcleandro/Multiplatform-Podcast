import Charts
import Shared
import SwiftUI

/// Listening stats (19.5, 25.8b): this week, all time and what speed and skipped silences saved, the last weeks as
/// bars, and the podcasts by time heard, as the Compose screen shows them.
struct ListeningStatsScreen: View {
    let model: ShellModel
    @State private var scope: ScreenScope
    @State private var stats: Watched<ListeningStats>

    init(model: ShellModel) {
        self.model = model
        let scope = ScreenScope()
        _scope = State(initialValue: scope)
        _stats = State(initialValue: Watched(scope.viewModels.listeningStats().stats))
    }

    private static let chartHeight: CGFloat = 120

    var body: some View {
        let stats = stats.value
        NavigationStack {
            Group {
                if stats.totalListenedMs <= 0 {
                    ContentUnavailableView(Res.string.shared.stats_empty_title.localized(), systemImage: "headphones",
                                           description: Text(Res.string.shared.stats_empty_message.localized()))
                } else {
                    List {
                        Section { totals(stats) }
                        Section(Res.string.shared.stats_by_week.localized()) { weeks(stats.weeks) }
                        Section(Res.string.shared.stats_by_podcast.localized()) {
                            ForEach(stats.podcasts, id: \.podcast.id) { entry in
                                HStack(spacing: Spacing.m) {
                                    Artwork(url: entry.podcast.imageUrl, size: Sizes.artworkS, radius: Radius.small)
                                    Text(entry.podcast.title).textStyle(Typography.titleMedium).lineLimit(2)
                                    Spacer()
                                    Text(StatsTexts.heard(entry.listenedMs)).foregroundStyle(.secondary)
                                }
                                .accessibilityElement(children: .combine)
                            }
                        }
                    }
                }
            }
            .navigationTitle(Res.string.shared.stats_title.localized())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(Res.string.shared.close.localized()) { model.root.onBackClicked() }
                }
            }
        }
    }

    private func totals(_ stats: ListeningStats) -> some View {
        Group {
            LabeledContent(Res.string.shared.stats_this_week.localized(), value: StatsTexts.heard(stats.thisWeekListenedMs))
            LabeledContent(Res.string.shared.stats_total.localized(), value: StatsTexts.heard(stats.totalListenedMs))
            if stats.totalSavedMs > 0 {
                LabeledContent {
                    Text(StatsTexts.heard(stats.totalSavedMs))
                } label: {
                    Text(Res.string.shared.stats_saved.localized())
                    Text(Res.string.shared.stats_saved_description.localized())
                }
            }
        }
    }

    /// One bar per week, oldest first; each tells VoiceOver its week and time.
    private func weeks(_ weeks: [WeekListening]) -> some View {
        Chart(weeks, id: \.startEpochDay) { week in
            BarMark(x: .value("week", StatsTexts.weekLabel(week.startEpochDay)), y: .value("ms", week.listenedMs))
                .foregroundStyle(Color(.Theme.primary))
                .accessibilityLabel(StatsTexts.weekLabel(week.startEpochDay))
                .accessibilityValue(StatsTexts.heard(week.listenedMs))
        }
        .chartYAxis(.hidden)
        .frame(height: Self.chartHeight)
    }
}

enum StatsTexts {
    private static let millisPerSecond: Int64 = 1000
    private static let secondsPerDay: Double = 86_400

    /// Time heard, as episode lengths are written ("1h 5min").
    static func heard(_ millis: Int64) -> String {
        LongExtensionsKt.toDuration(millis / millisPerSecond)
    }

    /// A week by its Monday, day and month in the user's language. The day is a calendar date, not an instant, so it is
    /// read in UTC: in the device's zone, west of Greenwich, midnight UTC is still the day before.
    static func weekLabel(_ epochDay: Int64, locale: Locale = .current) -> String {
        let date = Date(timeIntervalSince1970: Double(epochDay) * secondsPerDay)
        var style = Date.FormatStyle.dateTime.day().month(.abbreviated).locale(locale)
        style.timeZone = TimeZone(identifier: "UTC")!
        return date.formatted(style)
    }
}
