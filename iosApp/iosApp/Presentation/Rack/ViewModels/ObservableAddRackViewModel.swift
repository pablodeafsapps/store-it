import Foundation
import ComposeApp

@MainActor
final class ObservableAddRackViewModel: ObservableObject {
    let sharedVm: AddRackViewModel

    init() {
        self.sharedVm = IosKoinHelper().getAddRackViewModel()
    }

    deinit {
        sharedVm.clear()
    }
}
