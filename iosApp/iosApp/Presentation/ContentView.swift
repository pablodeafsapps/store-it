import SwiftUI
import ComposeApp

enum NavScreen {
    case rackList
    case addRack
    case addItem(initialRackId: String?, initialSlotId: String?)
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
                ZStack {
                    RackListView(
                        uiState: state,
                        uiEvent: event,
                        onAddRackSelect: { rackListViewModel.sharedVm.onAddRackSelect() },
                        onRackSelect: { rack in rackListViewModel.sharedVm.onRackSelect(rack: rack) }
                    )
                    .onChange(of: onEnum(of: event)) { _, _ in
                        handleRackListEvent(event)
                    }

                    VStack {
                        Spacer()
                        HStack {
                            Spacer()
                            Button(action: { currentScreen = .addItem(initialRackId: nil, initialSlotId: nil) }) {
                                Image(systemName: "plus.circle.fill")
                                    .font(.system(size: 28))
                            }
                            .disabled(state.racks.isEmpty)
                            .opacity(state.racks.isEmpty ? 0.5 : 1)
                            .padding()
                            .accessibilityIdentifier("addItemFloatingButton")
                            .accessibilityHint(state.racks.isEmpty ? "Add a rack first to place items" : "Add item")
                        }
                    }
                }
            }
        case .addRack:
            Observing(addRackViewModel.sharedVm.uiState, addRackViewModel.sharedVm.uiEvent.withInitialValue(nil)) { state, event in
                AddRackView(
                    uiState: state,
                    uiEvent: event,
                    onUpdateName: addRackViewModel.sharedVm.onUpdateName,
                    onUpdateDescription: addRackViewModel.sharedVm.onUpdateDescription,
                    onUpdateLocation: addRackViewModel.sharedVm.onUpdateLocation,
                    onUpdatePhotoUri: addRackViewModel.sharedVm.onUpdatePhotoUri,
                    onSaveRack: addRackViewModel.sharedVm.onSaveRack,
                    onNavigateBack: { currentScreen = .rackList },
                )
                .onChange(of: onEnum(of: event)) { _, _ in
                    if event != nil {
                        currentScreen = .rackList
                    }
                }
            }
        case .addItem(let initialRackId, let initialSlotId):
            AddItemScreen(
                initialRackId: initialRackId,
                initialSlotId: initialSlotId,
                onNavigateToAddRack: { currentScreen = .addRack },
                onNavigateBack: { currentScreen = .rackList }
            )
        case .rackDetail(let rackId):
            RackDetailView(
                rackId: rackId,
                onNavigateBack: { currentScreen = .rackList },
                onAddItemHere: { initialRackId, initialSlotId in
                    currentScreen = .addItem(initialRackId: initialRackId, initialSlotId: initialSlotId)
                }
            )
        }
    }

    private func handleRackListEvent(_ event: RackListUiEvent?) {
        currentScreen = RackListNavigation.nextScreen(current: currentScreen, event: event)
    }
}

private struct AddItemScreen: View {
    @StateObject private var addItemViewModel: ViewModelHolder<AddItemViewModel>
    let onNavigateToAddRack: () -> Void
    let onNavigateBack: () -> Void

    init(
        initialRackId: String?,
        initialSlotId: String?,
        onNavigateToAddRack: @escaping () -> Void,
        onNavigateBack: @escaping () -> Void
    ) {
        _addItemViewModel = StateObject(
            wrappedValue: ViewModelHolder(IosKoinHelper().getAddItemViewModel(initialRackId: initialRackId, initialSlotId: initialSlotId))
        )
        self.onNavigateToAddRack = onNavigateToAddRack
        self.onNavigateBack = onNavigateBack
    }

    var body: some View {
        Observing(addItemViewModel.sharedVm.uiState, addItemViewModel.sharedVm.uiEvent.withInitialValue(nil)) { state, event in
            AddItemView(
                uiState: state,
                uiEvent: event,
                onUpdateName: addItemViewModel.sharedVm.onUpdateName,
                onUpdateDescription: addItemViewModel.sharedVm.onUpdateDescription,
                onUpdateQuantity: { quantity in
                    if let quantity {
                        addItemViewModel.sharedVm.onUpdateQuantity(quantity: KotlinInt(int: Int32(quantity)))
                    } else {
                        addItemViewModel.sharedVm.onUpdateQuantity(quantity: nil)
                    }
                },
                onUpdateOwner: addItemViewModel.sharedVm.onUpdateOwner,
                onUpdateTagInput: addItemViewModel.sharedVm.onUpdateTagInput,
                onAddTag: addItemViewModel.sharedVm.onAddTag,
                onRemoveTag: addItemViewModel.sharedVm.onRemoveTag,
                onUpdatePhotoUri: addItemViewModel.sharedVm.onUpdatePhotoUri,
                onSelectRackAndSlotClick: addItemViewModel.sharedVm.onSelectRackAndSlotSelect,
                onSaveItem: addItemViewModel.sharedVm.onSaveItem,
                onRackSelected: addItemViewModel.sharedVm.onRackSelected,
                onBackFromSelectRack: addItemViewModel.sharedVm.onBackFromSelectRack,
                onBackFromSelectSlot: addItemViewModel.sharedVm.onBackFromSelectSlot,
                onSlotSelectedForItem: addItemViewModel.sharedVm.onSlotSelectedForItem,
                onNavigateToAddRack: onNavigateToAddRack,
                onNavigateBack: onNavigateBack
            )
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
