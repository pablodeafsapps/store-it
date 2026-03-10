import SwiftUI
import ComposeApp

enum NavScreen {
    case rackList
    case addRack
    case rackDetail(rackId: String)
}

enum RackListNavigation {
    static func nextScreen(current: NavScreen, event: RackListUiEvent?) -> NavScreen {
        guard let event = event else { return current }
        if event is RackListUiEventNavigateToAddRack {
            return .addRack
        } else if let detail = event as? RackListUiEventNavigateToRackDetail {
            return .rackDetail(rackId: detail.rackId)
        } else {
            return current
        }
    }
}

struct ContentView: View {
    @StateObject private var rackListViewModel: ViewModelHolder<RackListViewModel> = ViewModelHolder(IosKoinHelper().getRackListViewModel())
    @StateObject private var addRackViewModel: ViewModelHolder<AddRackViewModel> = ViewModelHolder(IosKoinHelper().getAddRackViewModel())
    @State private var currentScreen: NavScreen = .rackList

    var body: some View {
        switch currentScreen {
        case .rackList:
            Observing(rackListViewModel.sharedVm.uiState, rackListViewModel.sharedVm.uiEvent.withInitialValue(nil)) { state, event in
                RackListView(
                    uiState: state,
                    uiEvent: event,
                    onAddRackSelect: { rackListViewModel.sharedVm.onAddRackSelect() },
                    onRackSelect: { rack in rackListViewModel.sharedVm.onRackSelect(rack: rack) }
                )
                .onChange(of: onEnum(of: event)) { _, _ in
                    handleRackListEvent(event)
                }
            }
        case .addRack:
            Observing(addRackViewModel.sharedVm.uiState, addRackViewModel.sharedVm.uiEvent.withInitialValue(nil)) { state, event in
                AddRackView(
                    uiState: state,
                    uiEvent: event,
                    onUpdateName: addRackViewModel.sharedVm.updateName,
                    onUpdateDescription: addRackViewModel.sharedVm.updateDescription,
                    onUpdateLocation: addRackViewModel.sharedVm.updateLocation,
                    onUpdatePhotoUri: addRackViewModel.sharedVm.updatePhotoUri,
                    onSaveRack: addRackViewModel.sharedVm.saveRack,
                )
                .onChange(of: onEnum(of: event)) { _, _ in
                    if event != nil {
                        currentScreen = .rackList
                    }
                }
            }
            case .rackDetail(let rackId):
                    RackDetailView(
                        rackId: rackId,
                        onNavigateBack: { currentScreen = .rackList },
                    )
        }
    }

    private func handleRackListEvent(_ event: RackListUiEvent?) {
        currentScreen = RackListNavigation.nextScreen(current: currentScreen, event: event)
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
