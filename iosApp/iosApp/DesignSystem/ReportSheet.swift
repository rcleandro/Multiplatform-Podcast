import Shared
import SwiftUI

/// What is reported (26.7), the shared code's own: a podcast, or one of its episodes when `episodeTitle` is set.
extension ReportTarget: @retroactive Identifiable {
    public var id: String { feedUrl + (episodeTitle ?? "") }
}

/// A report of content that violates children's rights (26.7, ECA Digital art. 28), as the Compose dialog has it: the
/// law's reasons, whether to hide it here at once, and the official channels for a crime. It goes by email, with only
/// the feed's host, where a private feed keeps its token.
struct ReportSheet: View {
    let target: ReportTarget
    let onHide: () -> Void
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @State private var reason: ReportReason?
    @State private var hide = true

    var body: some View {
        NavigationStack {
            Form {
                Picker(selection: $reason) {
                    ForEach(ReportReason.entries, id: \.self) { Text(ReportTexts.reason($0)).tag(Optional($0)) }
                } label: {
                    EmptyView()
                }
                .pickerStyle(.inline)
                Section {
                    Toggle(Res.string.shared.report_hide.localized(), isOn: $hide)
                } footer: {
                    Text(Res.string.shared.report_channels.localized())
                }
            }
            .navigationTitle((target.episodeTitle == nil ? Res.string.shared.report_podcast_title
                                                         : Res.string.shared.report_episode_title).localized())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(Res.string.shared.cancel.localized()) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(Res.string.shared.report_send.localized(), action: send).disabled(reason == nil)
                }
            }
        }
    }

    private func send() {
        guard let reason, let url = ReportTexts.mailto(target, reason: reason) else { return }
        dismiss()
        openURL(url)
        if hide { onHide() }
    }
}

enum ReportTexts {
    static func reason(_ reason: ReportReason, bundle: Bundle = .main) -> String {
        let strings = Res.string.shared
        return switch reason {
        case .sexualAbuse: strings.report_reason_sexual_abuse.localized(bundle: bundle)
        case .violence: strings.report_reason_violence.localized(bundle: bundle)
        case .harmToHealth: strings.report_reason_harm_to_health.localized(bundle: bundle)
        case .gambling: strings.report_reason_gambling.localized(bundle: bundle)
        case .advertising: strings.report_reason_advertising.localized(bundle: bundle)
        case .pornography: strings.report_reason_pornography.localized(bundle: bundle)
        default: strings.report_reason_other.localized(bundle: bundle)
        }
    }

    /// The email, by the same builder as the Compose app, so the address and the encoding stay the same.
    static func mailto(_ target: ReportTarget, reason: ReportReason, bundle: Bundle = .main) -> URL? {
        let strings = Res.string.shared
        let subject = strings.report_email_subject.localized([target.episodeTitle ?? target.podcastTitle], bundle: bundle)
        let body = strings.report_email_body.localized(
            [Self.reason(reason, bundle: bundle), target.podcastTitle, target.episodeTitle ?? "",
             ReportMailKt.feedHost(feedUrl: target.feedUrl)],
            bundle: bundle
        )
        return URL(string: ReportMailKt.reportMailto(subject: subject, body: body))
    }
}
