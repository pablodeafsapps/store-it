import SwiftUI
import ComposeApp

private enum NavScreen {
    case rackList
    case addRack
    case rackDetail(rackId: String)
}

struct ContentView: View {
    @StateObject private var rackListViewModel = ObservableRackListViewModel()
    @StateObject private var addRackViewModel = ObservableAddRackViewModel()

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
                onNavigateBack: { currentScreen = .rackList }
            )
        }
    }

    private func handleRackListEvent(_ event: RackListUiEvent?) {
        guard let event = event else { return }
        if event is RackListUiEventNavigateToAddRack {
            currentScreen = .addRack
        } else if let detail = event as? RackListUiEventNavigateToRackDetail {
            currentScreen = .rackDetail(rackId: detail.rackId)
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
