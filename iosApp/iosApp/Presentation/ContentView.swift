import SwiftUI
import ComposeApp

struct ContentView: View {
    @StateObject private var rackListViewModel: ViewModelHolder<RackListViewModel> = ViewModelHolder(IosKoinHelper().getRackListViewModel())
    @State private var path: [AppRoute] = []

    var body: some View {
        NavigationStack(path: $path) {
            Observing(rackListViewModel.sharedVm.uiState) { state in
                ZStack {
                    RackListView(
                        uiState: state,
                        onAddRackSelected: { rackListViewModel.sharedVm.onAddRackSelected() },
                        onRackSelected: { rack in rackListViewModel.sharedVm.onRackSelected(rack: rack) },
                        onNavigateToSearch: { path.append(.search) }
                    )

                    VStack {
                        Spacer()
                        HStack {
                            Spacer()
                            Button(action: { path.append(.addItem(initialRackId: nil, initialSlotId: nil)) }) {
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
            .task {
                for await event in rackListViewModel.sharedVm.uiEvent {
                    handleRackListEvent(event)
                }
            }
            .navigationDestination(for: AppRoute.self) { route in
                routeDestination(route)
            }
        }
    }

    @ViewBuilder
    private func routeDestination(_ route: AppRoute) -> some View {
        switch route {
        case .search:
            SearchFlowScreen(
                onItemSelected: { placement in
                    path.append(.itemDetail(itemId: placement.item.id))
                }
            )
        case .addRack:
            AddRackView(
                onNavigateBack: { path.removeLast() }
            )
        case .addItem(let initialRackId, let initialSlotId):
            AddItemScreen(
                initialRackId: initialRackId,
                initialSlotId: initialSlotId,
                onNavigateToAddRack: { path.append(.addRack) },
                onNavigateBack: { path.removeLast() }
            )
        case .rackDetail(let rackId):
            RackDetailView(
                rackId: rackId,
                onNavigateBack: { path.removeLast() },
                onAddItemHere: { initialRackId, initialSlotId in
                    path.append(.addItem(initialRackId: initialRackId, initialSlotId: initialSlotId))
                },
                onNavigateToSlotItems: { r, s in
                    path.append(.slotItems(rackId: r, slotId: s))
                }
            )
        case .slotItems(let rackId, let slotId):
            SlotItemsView(
                rackId: rackId,
                slotId: slotId,
                onAddItem: { r, s in
                    path.append(.addItem(initialRackId: r, initialSlotId: s))
                },
                onItemSelected: { itemId in
                    path.append(.itemDetail(itemId: itemId))
                }
            )
        case .itemDetail(let itemId):
            ItemDetailScreen(
                itemId: itemId,
                onNavigateBack: { path.removeLast() }
            )
        }
    }

    private func handleRackListEvent(_ event: RackListUiEvent?) {
        guard let event else { return }
        if event is RackListUiEventNavigateToAddRack {
            path.append(.addRack)
        } else if let detail = event as? RackListUiEventNavigateToRackDetail {
            path.append(.rackDetail(rackId: detail.rackId))
        }
    }
}

private struct SearchFlowScreen: View {
    @StateObject private var viewModel: ViewModelHolder<SearchViewModel>
    let onItemSelected: (ItemWithPlacement) -> Void

    init(onItemSelected: @escaping (ItemWithPlacement) -> Void) {
        _viewModel = StateObject(wrappedValue: ViewModelHolder(IosKoinHelper().getSearchViewModel()))
        self.onItemSelected = onItemSelected
    }

    var body: some View {
        Observing(viewModel.sharedVm.uiState) { state in
            GlobalSearchView(
                uiState: state,
                onQueryChange: { viewModel.sharedVm.onQueryChange(query: $0) },
                onItemSelected: onItemSelected
            )
        }
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
                onSelectRackAndSlotSelected: addItemViewModel.sharedVm.onSelectRackAndSlotSelected,
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

private struct ItemDetailScreen: View {
    private let itemId: String
    @StateObject private var itemDetailViewModel: ViewModelHolder<ItemDetailViewModel>
    let onNavigateBack: () -> Void

    init(itemId: String, onNavigateBack: @escaping () -> Void) {
        self.itemId = itemId
        _itemDetailViewModel = StateObject(
            wrappedValue: ViewModelHolder(IosKoinHelper().getItemDetailViewModel(itemId: itemId))
        )
        self.onNavigateBack = onNavigateBack
    }

    var body: some View {
        Observing(itemDetailViewModel.sharedVm.uiState, itemDetailViewModel.sharedVm.uiEvent.withInitialValue(nil)) { state, event in
            ItemDetailView(
                uiState: state,
                uiEvent: event,
                onUpdateName: itemDetailViewModel.sharedVm.onUpdateName,
                onUpdateDescription: itemDetailViewModel.sharedVm.onUpdateDescription,
                onUpdateQuantity: { quantity in
                    if let quantity {
                        itemDetailViewModel.sharedVm.onUpdateQuantity(quantity: KotlinInt(int: Int32(quantity)))
                    } else {
                        itemDetailViewModel.sharedVm.onUpdateQuantity(quantity: nil)
                    }
                },
                onUpdateOwner: itemDetailViewModel.sharedVm.onUpdateOwner,
                onUpdateTagInput: itemDetailViewModel.sharedVm.onUpdateTagInput,
                onAddTag: itemDetailViewModel.sharedVm.onAddTag,
                onRemoveTag: itemDetailViewModel.sharedVm.onRemoveTag,
                onUpdatePhotoUri: itemDetailViewModel.sharedVm.onUpdatePhotoUri,
                onSave: itemDetailViewModel.sharedVm.onSave,
                onDelete: itemDetailViewModel.sharedVm.onDeleteSelected,
                onDismissDeleteConfirm: itemDetailViewModel.sharedVm.onDismissDeleteConfirm,
                onConfirmDelete: itemDetailViewModel.sharedVm.onConfirmDelete,
                onNavigateBack: onNavigateBack
            )
        }
        .id(itemId)
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
