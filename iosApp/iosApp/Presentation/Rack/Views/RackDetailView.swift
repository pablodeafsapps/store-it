import SwiftUI
import ComposeApp

struct RackDetailView: View {
    @StateObject private var rackDetailViewModel: ViewModelHolder<RackDetailViewModel>
    let onNavigateBack: () -> Void
    let forItemPlacement: Bool
    let onSlotSelectedForItem: ((String, String) -> Void)?
    let onAddItemHere: ((String, String) -> Void)?
    let onNavigateToSlotItems: ((String, String) -> Void)?

    init(
        rackId: String,
        onNavigateBack: @escaping () -> Void,
        forItemPlacement: Bool = false,
        onSlotSelectedForItem: ((String, String) -> Void)? = nil,
        onAddItemHere: ((String, String) -> Void)? = nil,
        onNavigateToSlotItems: ((String, String) -> Void)? = nil
    ) {
        _rackDetailViewModel = StateObject(
            wrappedValue: ViewModelHolder(IosKoinHelper().getRackDetailViewModel(rackId: rackId))
        )
        self.onNavigateBack = onNavigateBack
        self.forItemPlacement = forItemPlacement
        self.onSlotSelectedForItem = onSlotSelectedForItem
        self.onAddItemHere = onAddItemHere
        self.onNavigateToSlotItems = onNavigateToSlotItems
    }

    var body: some View {
        Observing(rackDetailViewModel.sharedVm.uiState) { state in
            RackDetailContent(
                state: state,
                onImageTap: { xRel, yRel in
                    rackDetailViewModel.sharedVm.onImageTap(xRel: xRel, yRel: yRel, forItemPlacement: forItemPlacement)
                },
                onEditSelected: rackDetailViewModel.sharedVm.onEditSelected,
                onRemoveRackSelected: rackDetailViewModel.sharedVm.onRemoveRackSelected,
                onDismissEditDialog: rackDetailViewModel.sharedVm.onDismissEditDialog,
                onUpdateEditName: rackDetailViewModel.sharedVm.onUpdateEditName,
                onUpdateEditDescription: rackDetailViewModel.sharedVm.onUpdateEditDescription,
                onUpdateEditLocation: rackDetailViewModel.sharedVm.onUpdateEditLocation,
                onSaveRackEdits: rackDetailViewModel.sharedVm.onSaveRackEdits,
                onDismissDeleteConfirm: rackDetailViewModel.sharedVm.onDismissDeleteConfirm,
                onConfirmDeleteRack: rackDetailViewModel.sharedVm.onConfirmDeleteRack,
                onNavigateBack: onNavigateBack,
                forItemPlacement: forItemPlacement,
                onUseSelectedSlot: {
                    if let rack = state.rack,
                       let slotId = state.selectedSlot?.id {
                        onSlotSelectedForItem?(rack.id, slotId)
                    }
                }
            )
        }
        .task {
            for await event in rackDetailViewModel.sharedVm.uiEvent {
                guard let event else { continue }
                if event is RackDetailUiEventNavigateBack {
                    onNavigateBack()
                } else if !forItemPlacement,
                          let nav = event as? RackDetailUiEventNavigateToSlotItems,
                          let onNavigateToSlotItems {
                    onNavigateToSlotItems(nav.rackId, nav.slotId)
                } else if !forItemPlacement,
                          let slotSelected = event as? RackDetailUiEventSlotSelected,
                          let onAddItemHere {
                    onAddItemHere(slotSelected.rackId, slotSelected.slotId)
                }
            }
        }
    }
}

private struct RackDetailContent: View {
    let state: RackDetailUiState
    let onImageTap: (Float, Float) -> Void
    let onEditSelected: () -> Void
    let onRemoveRackSelected: () -> Void
    let onDismissEditDialog: () -> Void
    let onUpdateEditName: (String) -> Void
    let onUpdateEditDescription: (String) -> Void
    let onUpdateEditLocation: (String) -> Void
    let onSaveRackEdits: () -> Void
    let onDismissDeleteConfirm: () -> Void
    let onConfirmDeleteRack: () -> Void
    let onNavigateBack: () -> Void
    let forItemPlacement: Bool
    let onUseSelectedSlot: () -> Void

    var body: some View {
        ZStack {
            if state.isLoading {
                ProgressView()
                    .scaleEffect(1.2)
            } else if let rack = state.rack {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        RackImageView(
                            photoUri: rack.photoUri,
                            slots: state.slots,
                            selectedSlotId: state.selectedSlot?.id,
                            onTap: onImageTap
                        )
                        if !rack.description_.isEmpty {
                            Text(rack.description_)
                                .font(.body)
                        }
                        if !rack.location.isEmpty {
                            Text("Location: \(rack.location)")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding()
                }
            } else {
                Text(state.error ?? "Rack not found")
                    .foregroundColor(.red)
                    .padding()
            }
            if let error = state.error, state.rack != nil {
                VStack {
                    Text(error)
                        .foregroundColor(.red)
                        .padding()
                    Spacer()
                }
            }

            if forItemPlacement, state.selectedSlot != nil {
                VStack {
                    Spacer()
                    Button(action: { onUseSelectedSlot() }) {
                        Text("Use this slot")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .padding()
                }
            }
        }
        .navigationTitle(state.rack?.name ?? "Rack")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(forItemPlacement)
        .toolbar {
            if forItemPlacement {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Back") {
                        onNavigateBack()
                    }
                }
            }
            if !forItemPlacement {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button("Edit") {
                            onEditSelected()
                        }
                        .accessibilityIdentifier("editRackMenuItem")
                        Button("Remove rack", role: .destructive) {
                            onRemoveRackSelected()
                        }
                        .accessibilityIdentifier("removeRackMenuItem")
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                    .accessibilityIdentifier("rackDetailMenuButton")
                }
            }
        }
        .sheet(isPresented: Binding(
            get: { state.showEditDialog },
            set: { if !$0 { onDismissEditDialog() } }
        )) {
            EditRackSheet(
                name: state.editName,
                description: state.editDescription,
                location: state.editLocation,
                onNameChange: onUpdateEditName,
                onDescriptionChange: onUpdateEditDescription,
                onLocationChange: onUpdateEditLocation,
                onDismiss: onDismissEditDialog,
                onSave: onSaveRackEdits
            )
        }
        .alert("Remove rack?", isPresented: Binding(
            get: { state.showDeleteConfirm },
            set: { if !$0 { onDismissDeleteConfirm() } }
        )) {
            Button("Cancel", role: .cancel) {
                onDismissDeleteConfirm()
            }
            Button("Remove", role: .destructive) {
                onConfirmDeleteRack()
            }
        } message: {
            Text("This will delete the rack and all its slots and items. This cannot be undone.")
        }
    }
}

private struct RackImageView: View {
    let photoUri: String?
    let slots: [RackDetailSlotVo]
    let selectedSlotId: String?
    let onTap: (Float, Float) -> Void

    var body: some View {
        Group {
            if let path = photoUri {
                AsyncImage(url: URL(fileURLWithPath: path)) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFit()
                    default:
                        Rectangle()
                            .fill(Color.gray.opacity(0.2))
                            .frame(height: 200)
                            .overlay(alignment: .center) {
                                ProgressView()
                            }
                    }
                }
                .frame(maxWidth: .infinity)
                .overlay(alignment: .topLeading) {
                    GeometryReader { geo in
                        let w = max(geo.size.width, 1)
                        let h = max(geo.size.height, 1)
                        Color.clear
                            .contentShape(Rectangle())
                            .accessibilityIdentifier("rackDetailImageArea")
                            .onTapGesture(coordinateSpace: .local) { location in
                                let xRel = Float((location.x / w).clamped(to: 0...1))
                                let yRel = Float((location.y / h).clamped(to: 0...1))
                                onTap(xRel, yRel)
                            }
                        ForEach(slots, id: \.id) { slot in
                            Circle()
                                .fill(selectedSlotId == slot.id ? Color.accentColor : Color.accentColor.opacity(0.6))
                                .frame(width: 24, height: 24)
                                .position(
                                    x: CGFloat(slot.xRel) * geo.size.width,
                                    y: CGFloat(slot.yRel) * geo.size.height
                                )
                                .allowsHitTesting(false)
                        }
                    }
                }
                .accessibilityIdentifier("rackDetailImageArea")
            } else {
                Rectangle()
                    .fill(Color.gray.opacity(0.2))
                    .frame(height: 200)
                    .overlay(alignment: .center) {
                        Text("No photo")
                            .foregroundColor(.secondary)
                    }
                    .overlay(alignment: .topLeading) {
                        GeometryReader { geo in
                            let w = max(geo.size.width, 1)
                            let h = max(geo.size.height, 1)
                            Color.clear
                                .contentShape(Rectangle())
                                .accessibilityIdentifier("rackDetailImageArea")
                                .onTapGesture(coordinateSpace: .local) { location in
                                    let xRel = Float((location.x / w).clamped(to: 0...1))
                                    let yRel = Float((location.y / h).clamped(to: 0...1))
                                    onTap(xRel, yRel)
                                }
                            ForEach(slots, id: \.id) { slot in
                                Circle()
                                    .fill(selectedSlotId == slot.id ? Color.accentColor : Color.accentColor.opacity(0.6))
                                    .frame(width: 24, height: 24)
                                    .position(
                                        x: CGFloat(slot.xRel) * geo.size.width,
                                        y: CGFloat(slot.yRel) * geo.size.height
                                    )
                                    .allowsHitTesting(false)
                            }
                        }
                    }
                    .accessibilityIdentifier("rackDetailImageArea")
            }
        }
    }
}

extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}

private struct EditRackSheet: View {
    let name: String
    let description: String
    let location: String
    let onNameChange: (String) -> Void
    let onDescriptionChange: (String) -> Void
    let onLocationChange: (String) -> Void
    let onDismiss: () -> Void
    let onSave: () -> Void

    var body: some View {
        NavigationStack {
            Form {
                TextField("Name", text: Binding(get: { name }, set: onNameChange))
                TextField("Description", text: Binding(get: { description }, set: onDescriptionChange), axis: .vertical)
                    .lineLimit(3...6)
                TextField("Location", text: Binding(get: { location }, set: onLocationChange))
            }
            .navigationTitle("Edit rack")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onDismiss() }
                        .accessibilityIdentifier("editRackCancelButton")
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { onSave() }
                        .accessibilityIdentifier("editRackSaveButton")
                }
            }
        }
    }
}
