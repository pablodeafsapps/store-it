import Foundation
import ComposeApp

@MainActor
final class ObservableRackDetailViewModel: ObservableObject {
    let sharedVm: RackDetailViewModel

    init(rackId: String) {
        self.sharedVm = IosKoinHelper().getRackDetailViewModel(rackId: rackId)
    }

    deinit {
        sharedVm.clear()
    }
}
