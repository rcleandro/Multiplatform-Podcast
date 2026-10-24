import Observation
import Shared

/// A view model's state for SwiftUI (ADR 0015): the flow's current value, kept up to date while this lives.
@MainActor
@Observable
final class Watched<Value> {
    private(set) var value: Value
    @ObservationIgnored private var watching: Watching?

    init(_ flow: any Kotlinx_coroutines_coreStateFlow) {
        value = flow.value as! Value
        watching = SwiftBridgeKt.watch(flow) { [weak self] next in
            self?.value = next as! Value
        }
    }

    deinit {
        watching?.cancel()
    }
}

/// The view models of one screen, cleared when the screen lets go of it (the lifetime of 23.10).
final class ScreenScope {
    let viewModels = ScreenViewModels()

    deinit {
        viewModels.clear()
    }
}
