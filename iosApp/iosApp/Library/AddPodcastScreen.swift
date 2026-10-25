import Shared
import SwiftUI

/// "Add podcast" (25.4), for now its feed address part: the address goes to the same view model the Compose
/// directory screen uses, and the sheet closes once the podcast is in the library. A failure shows as a message.
struct AddPodcastScreen: View {
    let model: ShellModel
    @State private var scope: ScreenScope
    @State private var discover: Watched<DiscoverUiState>
    @State private var added: Watching?
    @FocusState private var isTyping: Bool

    init(model: ShellModel) {
        self.model = model
        let scope = ScreenScope()
        _scope = State(initialValue: scope)
        _discover = State(initialValue: Watched(scope.viewModels.discover().uiState))
    }

    var body: some View {
        let state = discover.value
        NavigationStack {
            Form {
                Section {
                    TextField(
                        Res.string.shared.rss_url_label.localized(),
                        text: Binding(get: { state.url }, set: { send(DiscoverIntentChangeUrl(url: $0)) }),
                        prompt: Text(Res.string.shared.rss_url_placeholder.localized())
                    )
                    // In a Form the title is not read out once there is a prompt; VoiceOver would only say "https".
                    .accessibilityLabel(Res.string.shared.rss_url_label.localized())
                    .keyboardType(.URL)
                    .textContentType(.URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.done)
                    .focused($isTyping)
                    .onSubmit(add)
                    .disabled(state.isAddingUrl)
                } footer: {
                    if state.isAddingUrl { ProgressView().frame(maxWidth: .infinity) }
                }
            }
            .navigationTitle(Res.string.shared.add_by_url.localized())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(Res.string.shared.cancel.localized()) { model.root.onBackClicked() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(Res.string.shared.add.localized(), action: add)
                        .disabled(state.url.trimmingCharacters(in: .whitespaces).isEmpty || state.isAddingUrl)
                }
            }
            .messages(from: scope.viewModels.discover().messages)
        }
        .onAppear {
            send(DiscoverIntentOpen(session: model.state.addPodcastSession))
            isTyping = true
            added = SwiftBridgeKt.watch(scope.viewModels.discover().addedByUrl) { _ in model.root.onBackClicked() }
        }
        .onDisappear { added?.cancel() }
    }

    private func add() {
        send(DiscoverIntentConfirmUrl.shared)
    }

    private func send(_ intent: any DiscoverIntent) {
        scope.viewModels.discover().onIntent(intent: intent)
    }
}
