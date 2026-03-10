import Foundation
import ComposeApp

class ViewModelHolder <T : StoreItViewModel> : ObservableObject {
    let sharedVm: T
    
    init(_ sharedVm: T) {
        self.sharedVm = sharedVm
    }
    
    deinit {
        sharedVm.clear()
    }
}
