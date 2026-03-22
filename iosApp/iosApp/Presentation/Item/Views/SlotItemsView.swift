import SwiftUI
import ComposeApp

struct SlotItemsView: View {
    @StateObject private var viewModel: ViewModelHolder<SlotItemsViewModel>
    private let rackId: String
    private let slotId: String
    let onNavigateBack: () -> Void
    let onAddItem: (String, String) -> Void

    init(rackId: String, slotId: String, onNavigateBack: @escaping () -> Void, onAddItem: @escaping (String, String) -> Void) {
        self.rackId = rackId
        self.slotId = slotId
        _viewModel = StateObject(wrappedValue: ViewModelHolder(IosKoinHelper().getSlotItemsViewModel(rackId: rackId, slotId: slotId)))
        self.onNavigateBack = onNavigateBack
        self.onAddItem = onAddItem
    }

    var body: some View {
        NavigationView {
            Observing(viewModel.sharedVm.uiState, viewModel.sharedVm.uiEvent.withInitialValue(nil)) { state, _ in
                ZStack {
                    if state.isLoading {
                        ProgressView()
                            .accessibilityIdentifier("slotItemsScreenLoading")
                    } else if let err = state.error {
                        Text(err)
                            .foregroundColor(.red)
                            .padding()
                            .accessibilityIdentifier("slotItemsScreenError")
                    } else if state.items.isEmpty {
                        VStack(spacing: 16) {
                            Spacer()
                            Text("No items stored here.")
                                .foregroundColor(.secondary)
                                .accessibilityIdentifier("slotItemsScreenEmpty")
                            Button("Add item") {
                                onAddItem(rackId, slotId)
                            }
                            .accessibilityIdentifier("slotItemsScreenAddItemButton")
                            Spacer()
                        }
                    } else {
                        List(state.items, id: \.id) { item in
                            Text(item.name)
                                .accessibilityIdentifier("slotItemRow_\(item.id)")
                        }
                        .accessibilityIdentifier("slotItemsScreenList")
                        .safeAreaInset(edge: .bottom) {
                            Button("Add item") {
                                onAddItem(rackId, slotId)
                            }
                            .frame(maxWidth: .infinity)
                            .buttonStyle(.borderedProminent)
                            .padding()
                            .accessibilityIdentifier("slotItemsScreenAddItemButton")
                        }
                    }
                }
                .navigationTitle("Items in this slot")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Back") {
                            onNavigateBack()
                        }
                        .accessibilityIdentifier("slotItemsScreenBackButton")
                    }
                }
            }
        }
    }
}
