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
                            Button(action: {
                                path.append(.addItem)
                            }) {
                                Image(systemName: "plus.circle.fill")
                                    .font(.system(size: 28))
                            }
                            .disabled(state.racks.isEmpty)
                            .opacity(state.racks.isEmpty ? 0.5 : 1)
                            .padding()
                            .accessibilityIdentifier("addItemFloatingButton")
                            .accessibilityHint(state.racks.isEmpty ? "add_item_hint_add_rack_first" : "add_item_hint_add_item")
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
                onNavigateBack: { popRoute() }
            )
        case .addItem:
            AddItemScreen(
                initialRackId: nil,
                addItemSlot: AddItemSlotVoFromRoute.empty,
                onNavigateToAddRack: { path.append(.addRack) },
                onNavigateBack: { popRoute() }
            )
        case .addItemAtSlot(let initialRackId, let initialSlotId):
            AddItemScreen(
                initialRackId: initialRackId,
                addItemSlot: AddItemSlotVoFromRoute.existing(slotId: initialSlotId),
                onNavigateToAddRack: { path.append(.addRack) },
                onNavigateBack: { popRoute() }
            )
        case .addItemAtDraftSlot(let initialRackId, let initialSlotId, let initialSlotXRel, let initialSlotYRel):
            AddItemScreen(
                initialRackId: initialRackId,
                addItemSlot: AddItemSlotVoFromRoute.draft(
                    slotId: initialSlotId,
                    xRel: initialSlotXRel,
                    yRel: initialSlotYRel
                ),
                onNavigateToAddRack: { path.append(.addRack) },
                onNavigateBack: { popRoute() }
            )
        case .rackDetail(let rackId):
            RackDetailView(
                navigationPath: $path,
                rackId: rackId,
                onNavigateBack: { popRoute() },
                onAddItemHere: { initialRackId, initialSlotId, initialSlotXRel, initialSlotYRel in
                    path.append(
                        .addItemAtDraftSlot(
                            initialRackId: initialRackId,
                            initialSlotId: initialSlotId,
                            initialSlotXRel: initialSlotXRel,
                            initialSlotYRel: initialSlotYRel
                        )
                    )
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
                    path.append(.addItemAtSlot(initialRackId: r, initialSlotId: s))
                },
                onItemSelected: { itemId in
                    path.append(.itemDetail(itemId: itemId))
                }
            )
        case .itemDetail(let itemId):
            ItemDetailScreen(
                itemId: itemId,
                onNavigateBack: { popRoute() }
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

    private func popRoute() {
        guard !path.isEmpty else { return }
        path.removeLast()
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
        addItemSlot: AddItemSlotVo,
        onNavigateToAddRack: @escaping () -> Void,
        onNavigateBack: @escaping () -> Void
    ) {
        _addItemViewModel = StateObject(
            wrappedValue: ViewModelHolder(
                IosKoinHelper().getAddItemViewModel(
                    initialRackId: initialRackId,
                    addItemSlot: addItemSlot
                )
            )
        )
        self.onNavigateToAddRack = onNavigateToAddRack
        self.onNavigateBack = onNavigateBack
    }

    var body: some View {
        Observing(addItemViewModel.sharedVm.uiState) { state in
            AddItemView(
                uiState: state,
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
                onSlotSelectedForItem: { rackId, slot in
                    addItemViewModel.sharedVm.onSlotSelectedForItem(rackId: rackId, slot: slot)
                },
                onNavigateToAddRack: onNavigateToAddRack,
                onNavigateBack: onNavigateBack
            )
        }
        .task {
            for await event in addItemViewModel.sharedVm.uiEvent {
                if event != nil {
                    onNavigateBack()
                }
            }
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
        Observing(itemDetailViewModel.sharedVm.uiState) { state in
            ItemDetailView(
                uiState: state,
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
        .task {
            for await event in itemDetailViewModel.sharedVm.uiEvent {
                if event is ItemDetailUiEventNavigateBack {
                    onNavigateBack()
                }
            }
        }
        .id(itemId)
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
