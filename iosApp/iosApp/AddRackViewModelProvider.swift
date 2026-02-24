import Foundation
import ComposeApp
import Combine

class AddRackViewModelProvider {
    static let shared = AddRackViewModelProvider()
    
    private init() {}
    
    func createViewModel() -> AddRackViewModelWrapper {
        let viewModel = AddRackViewModelFactory.shared.createViewModel()
        return AddRackViewModelWrapper(viewModel: viewModel)
    }
}

class AddRackViewModelWrapper: ObservableObject {
    @Published var uiState: AddRackUiState
    @Published var uiEvent: AddRackUiEvent?
    
    private let viewModel: AddRackViewModel
    private var stateTimer: Timer?
    private var eventTimer: Timer?
    
    init(viewModel: AddRackViewModel) {
        self.viewModel = viewModel
        self.uiState = viewModel.uiState.value as! AddRackUiState
        
        startObserving()
    }
    
    private func startObserving() {
        stateTimer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            let newState = self.viewModel.uiState.value as! AddRackUiState
            if self.uiState.name != newState.name ||
               self.uiState.description != newState.description ||
               self.uiState.location != newState.location ||
               self.uiState.photoUri != newState.photoUri ||
               self.uiState.isLoading != newState.isLoading ||
               self.uiState.error != newState.error ||
               self.uiState.isSuccess != newState.isSuccess {
                DispatchQueue.main.async {
                    self.uiState = newState
                }
            }
        }
        
        eventTimer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            let newEvent = self.viewModel.uiEvent.value as? AddRackUiEvent
            if self.uiEvent !== newEvent {
                DispatchQueue.main.async {
                    self.uiEvent = newEvent
                }
            }
        }
    }
    
    func updateName(name: String) {
        viewModel.updateName(name: name)
    }
    
    func updateDescription(description: String) {
        viewModel.updateDescription(description: description)
    }
    
    func updateLocation(location: String) {
        viewModel.updateLocation(location: location)
    }
    
    func updatePhotoUri(uri: String?) {
        viewModel.updatePhotoUri(uri: uri)
    }
    
    func saveRack() {
        viewModel.saveRack()
    }
    
    func clearEvent() {
        viewModel.clearEvent()
    }
    
    deinit {
        stateTimer?.invalidate()
        eventTimer?.invalidate()
    }
}
