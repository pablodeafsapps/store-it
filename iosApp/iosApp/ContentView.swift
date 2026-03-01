import SwiftUI
import ComposeApp

struct ContentView: View {
    
    private let viewModel: AddRackViewModel = AddRackViewModel()
    
    @State private var showContent = false

    var body: some View {
        Observing(viewModel.uiState, viewModel.uiEvent.withInitialValue(nil)) { state, event in
            AddRackView(
                uiState: state,
                uiEvent: event,
                onUpdateName: viewModel.updateName,
                onUpdateDescription: viewModel.updateDescription,
                onUpdateLocation: viewModel.updateLocation,
                onUpdatePhotoUri: viewModel.updatePhotoUri,
                onSaveRack: viewModel.saveRack,
            )
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
