import Foundation
import ComposeApp

@MainActor
final class ObservableRackListViewModel: ObservableObject {
    let sharedVm: RackListViewModel

    init() {
        self.sharedVm = IosKoinHelper().getRackListViewModel()
    }

    deinit {
        sharedVm.clear()
    }
}
