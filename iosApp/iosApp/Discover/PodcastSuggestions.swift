import Shared
import SwiftUI

/// What an empty library shows instead of a blank screen (18.3): pick a category and subscribe to its most popular
/// podcasts, or look for one. The library goes back to its podcasts as soon as it has one.
struct PodcastSuggestions: View {
    let model: ShellModel
    @State private var scope: ScreenScope
    @State private var discover: Watched<DiscoverUiState>

    init(model: ShellModel) {
        self.model = model
        let scope = ScreenScope()
        _scope = State(initialValue: scope)
        _discover = State(initialValue: Watched(scope.viewModels.discover().uiState))
    }

    var body: some View {
        let state = discover.value
        List {
            VStack(alignment: .leading, spacing: Spacing.s) {
                Text(Res.string.shared.suggestions_title.localized()).textStyle(Typography.titleLarge)
                    .accessibilityAddTraits(.isHeader)
                Text(Res.string.shared.suggestions_message.localized()).textStyle(Typography.bodyMedium).foregroundStyle(Color(.Theme.onSurfaceVariant))
                Button(Res.string.shared.add_podcast.localized()) { model.root.onAddPodcast() }.buttonStyle(.borderedProminent)
            }
            .listRowSeparator(.hidden)
            DirectoryCategories(selected: state.category) { send(DiscoverIntentSelectCategory(category: $0)) }
                .listRowSeparator(.hidden)
            DirectoryResults(
                state: state,
                onSubscribe: { send(DiscoverIntentSubscribe(result: $0)) },
                onRetry: { send(DiscoverIntentRetry.shared) },
                onAddByUrl: { model.root.onAddPodcast() }
            )
        }
        .listStyle(.plain)
    }

    private func send(_ intent: any DiscoverIntent) {
        scope.viewModels.discover().onIntent(intent: intent)
    }
}
