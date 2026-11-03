import Shared
import SwiftUI

extension StyledText {
    /// The description with its bold, italic and links; the positions are UTF-16 offsets, as NSRange counts them.
    var attributed: AttributedString {
        let result = NSMutableAttributedString(string: text)
        for span in spans {
            let range = NSRange(location: Int(span.start), length: Int(span.end - span.start))
            if let url = span.url.flatMap(URL.init(string:)) { result.addAttribute(.link, value: url, range: range) }
            if span.bold { result.addAttribute(.inlinePresentationIntent, value: InlinePresentationIntent.stronglyEmphasized.rawValue, range: range) }
            if span.italic { result.addAttribute(.inlinePresentationIntent, value: InlinePresentationIntent.emphasized.rawValue, range: range) }
        }
        return (try? AttributedString(result, including: \.foundation)) ?? AttributedString(text)
    }
}

/// A feed description: the first lines and "Show more" when it is long, all of it with no button when short.
struct HtmlText: View {
    let html: String
    var collapsedLines = 4
    @State private var expanded = false

    var body: some View {
        let styled = StyledHtmlKt.styledHtml(html: html).attributed
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(styled)
                .textStyle(Typography.bodyMedium)
                .lineLimit(expanded ? nil : collapsedLines)
                .tint(Color(.Theme.primary))
            if styled.characters.count > Self.shortEnough {
                Button((expanded ? Res.string.shared.show_less : Res.string.shared.show_more).localized()) {
                    expanded.toggle()
                }
                .textStyle(Typography.labelLarge)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // ponytail: a length stands in for "does it overflow four lines"; measuring the text needs a layout pass per width.
    private static let shortEnough = 240
}
