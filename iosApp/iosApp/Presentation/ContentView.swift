import SwiftUI
import ComposeApp

private enum NavScreen {
    case rackList
    case addRack
    case rackDetail(rackId: String)
}

struct ContentView: View {
    private let rackListViewModel: RackListViewModel = IosKoinHelper().getRackListViewModel()
    private let addRackViewModel: AddRackViewModel = IosKoinHelper().getAddRackViewModel()

    @State private var currentScreen: NavScreen = .rackList

    var body: some View {
        switch currentScreen {
        case .rackList:
                Observing(rackListViewModel.uiState, rackListViewModel.uiEvent.withInitialValue(nil)) { state, event in
                RackListView(
                    uiState: state,
                    uiEvent: event,
                    onAddRackSelect: { rackListViewModel.onAddRackSelect() },
                    onRackSelect: { rack in rackListViewModel.onRackSelect(rack: rack) }
                )
                .onChange(of: onEnum(of: event)) { _, _ in
                    handleRackListEvent(event)
                }
            }
        case .addRack:
            Observing(addRackViewModel.uiState, addRackViewModel.uiEvent.withInitialValue(nil)) { state, event in
                AddRackView(
                    uiState: state,
                    uiEvent: event,
                    onUpdateName: addRackViewModel.updateName,
                    onUpdateDescription: addRackViewModel.updateDescription,
                    onUpdateLocation: addRackViewModel.updateLocation,
                    onUpdatePhotoUri: addRackViewModel.updatePhotoUri,
                    onSaveRack: addRackViewModel.saveRack,
                )
                .onChange(of: onEnum(of: event)) { _, _ in
                    if event != nil {
                        currentScreen = .rackList
                    }
                }
            }
        case .rackDetail(let rackId):
            RackDetailPlaceholderView(
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
