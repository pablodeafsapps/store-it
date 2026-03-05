import SwiftUI
import ComposeApp

struct RackDetailView: View {
    let rackId: String
    let onNavigateBack: () -> Void

    @StateObject private var viewModel: ObservableRackDetailViewModel

    init(rackId: String, onNavigateBack: @escaping () -> Void) {
        self.rackId = rackId
        self.onNavigateBack = onNavigateBack
        _viewModel = StateObject(wrappedValue: ObservableRackDetailViewModel(rackId: rackId))
    }

    var body: some View {
        Observing(
            viewModel.sharedVm.uiState,
            viewModel.sharedVm.uiEvent.withInitialValue(nil)
        ) { state, event in
            content(uiState: state)
                .onChange(of: onEnum(of: event)) { _, _ in
                    handleEvent(event)
                }
        }
    }

    @ViewBuilder
    private func content(uiState: RackDetailUiState) -> some View {
        NavigationView {
            ZStack {
                if uiState.isLoading {
                    ProgressView()
                        .scaleEffect(1.2)
                } else if let rack = uiState.rack {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 12) {
                            RackImageView(
                                photoUri: rack.photoUri,
                                slots: uiState.slots,
                                selectedSlotId: uiState.selectedSlotId,
                                onTap: { xRel, yRel in
                                    viewModel.sharedVm.onImageTap(xRel: xRel, yRel: yRel)
                                }
                            )
                            if !rack.description.isEmpty {
                                Text(rack.description)
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
                    Text(uiState.error ?? "Rack not found")
                        .foregroundColor(.red)
                        .padding()
                }
                if let error = uiState.error, uiState.rack != nil {
                    VStack {
                        Text(error)
                            .foregroundColor(.red)
                            .padding()
                        Spacer()
                    }
                }
            }
            .navigationTitle(uiState.rack?.name ?? "Rack")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Back") {
                        onNavigateBack()
                    }
                    .accessibilityIdentifier("rackDetailBackButton")
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button("Edit") {
                            viewModel.sharedVm.onEditClick()
                        }
                        Button("Remove rack", role: .destructive) {
                            viewModel.sharedVm.onRemoveRackClick()
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                    .accessibilityIdentifier("rackDetailMenuButton")
                }
            }
            .sheet(isPresented: Binding(
                get: { uiState.showEditDialog },
                set: { if !$0 { viewModel.sharedVm.dismissEditDialog() } }
            )) {
                EditRackSheet(
                    name: uiState.editName,
                    description: uiState.editDescription,
                    location: uiState.editLocation,
                    onNameChange: viewModel.sharedVm.updateEditName,
                    onDescriptionChange: viewModel.sharedVm.updateEditDescription,
                    onLocationChange: viewModel.sharedVm.updateEditLocation,
                    onDismiss: viewModel.sharedVm.dismissEditDialog,
                    onSave: viewModel.sharedVm.saveRackEdits
                )
            }
            .alert("Remove rack?", isPresented: Binding(
                get: { uiState.showDeleteConfirm },
                set: { if !$0 { viewModel.sharedVm.dismissDeleteConfirm() } }
            )) {
                Button("Cancel", role: .cancel) {
                    viewModel.sharedVm.dismissDeleteConfirm()
                }
                Button("Remove", role: .destructive) {
                    viewModel.sharedVm.confirmDeleteRack()
                }
            } message: {
                Text("This will delete the rack and all its slots and items. This cannot be undone.")
            }
        }
    }

    private func handleEvent(_ event: RackDetailUiEvent?) {
        guard let event = event else { return }
        if event is RackDetailUiEventNavigateBack {
            onNavigateBack()
        }
    }
}

private struct RackImageView: View {
    let photoUri: String?
    let slots: [RackDetailSlotView]
    let selectedSlotId: String?
    let onTap: (Float, Float) -> Void

    var body: some View {
        Group {
            if let path = photoUri, let uiImage = loadImage(from: path) {
                Image(uiImage: uiImage)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: .infinity)
                    .overlay(alignment: .topLeading) {
                        GeometryReader { geo in
                            let w = max(geo.size.width, 1)
                            let h = max(geo.size.height, 1)
                            Color.clear
                                .contentShape(Rectangle())
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
            } else {
                Rectangle()
                    .fill(Color.gray.opacity(0.2))
                    .frame(height: 200)
                    .overlay(Text("No photo").foregroundColor(.secondary))
            }
        }
    }

    private func loadImage(from path: String) -> UIImage? {
        let url = URL(fileURLWithPath: path)
        guard let data = try? Data(contentsOf: url) else { return nil }
        return UIImage(data: data)
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
        NavigationView {
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
