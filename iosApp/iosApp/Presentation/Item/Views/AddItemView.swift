import SwiftUI
import PhotosUI
import ComposeApp

struct AddItemView: View {
    let uiState: AddItemUiState
    let uiEvent: AddItemUiEvent?
    let onUpdateName: (String) -> Void
    let onUpdateDescription: (String) -> Void
    let onUpdateQuantity: (Int?) -> Void
    let onUpdateOwner: (String) -> Void
    let onUpdateTagInput: (String) -> Void
    let onAddTag: () -> Void
    let onRemoveTag: (String) -> Void
    let onUpdatePhotoUri: (String?) -> Void
    let onSelectRackAndSlotSelected: () -> Void
    let onSaveItem: () -> Void
    let onRackSelected: (Rack) -> Void
    let onBackFromSelectRack: () -> Void
    let onBackFromSelectSlot: () -> Void
    let onSlotSelectedForItem: (String, String) -> Void
    let onNavigateToAddRack: () -> Void
    let onNavigateBack: () -> Void

    @State private var showPhotoPicker = false
    @State private var selectedPhotoItem: PhotosPickerItem? = nil
    @State private var selectedImageData: Data? = nil

    var body: some View {
        Group {
            switch uiState.step {
            case .form:
                formContent
            case .selectRack:
                selectRackContent
            case .selectSlot:
                selectSlotContent
            @unknown default:
                formContent
            }
        }
        .onChange(of: onEnum(of: uiEvent)) { _, _ in
            if uiEvent != nil {
                onNavigateBack()
            }
        }
    }

    private var formContent: some View {
        Form {
                Section {
                    photoSection
                }

                Section(header: Text("Details")) {
                    TextField("Name", text: Binding(
                        get: { uiState.name },
                        set: onUpdateName
                    ))
                    TextField("Description", text: Binding(
                        get: { uiState.description_ },
                        set: onUpdateDescription
                    ), axis: .vertical)
                    .lineLimit(3...6)

                    TextField("Quantity", text: Binding(
                        get: { uiState.quantity?.description ?? "" },
                        set: { newValue in onUpdateQuantity(Int(newValue)) }
                    ))
                    .keyboardType(.numberPad)

                    TextField("Owner", text: Binding(
                        get: { uiState.owner },
                        set: onUpdateOwner
                    ))
                }

                Section(header: Text("Tags")) {
                    HStack {
                        TextField("Add tag", text: Binding(
                            get: { uiState.tagInput },
                            set: onUpdateTagInput
                        ))
                        Button("Add") {
                            onAddTag()
                        }
                        .disabled(uiState.tagInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }

                    if !uiState.tags.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(uiState.tags, id: \.self) { tag in
                                    Button(action: { onRemoveTag(tag) }) {
                                        Text(tag)
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 4)
                                            .background(Capsule().fill(Color.accentColor.opacity(0.2)))
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }
                }

                Section {
                    Button(action: { onSelectRackAndSlotSelected() }) {
                        Text(
                            uiState.selectedRackId != nil && uiState.selectedSlotId != nil
                            ? "Rack & slot selected"
                            : "Select rack & slot"
                        )
                        .frame(maxWidth: .infinity)
                    }
                }

                if let error = uiState.error {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                    }
                }

                Section {
                    Button(action: { onSaveItem() }) {
                        HStack {
                            if uiState.isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                            }
                            Text("Save Item")
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .disabled(uiState.isLoading)
                }
        }
        .navigationTitle("Add Item")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Cancel") {
                    onNavigateBack()
                }
            }
        }
        .photosPicker(
            isPresented: $showPhotoPicker,
            selection: $selectedPhotoItem,
            matching: .images
        )
        .onChange(of: selectedPhotoItem) { _, newValue in
            Task {
                guard
                    let data = try? await newValue?.loadTransferable(type: Data.self),
                    let image = UIImage(data: data),
                    let jpegData = image.jpegData(compressionQuality: 0.8)
                else { return }

                selectedImageData = data

                let tempURL = FileManager.default.temporaryDirectory
                    .appendingPathComponent(UUID().uuidString)
                    .appendingPathExtension("jpg")
                try? jpegData.write(to: tempURL)
                onUpdatePhotoUri(tempURL.path)
            }
        }
    }

    private var photoSection: some View {
        VStack(spacing: 12) {
            if let imageData = selectedImageData,
               let uiImage = UIImage(data: imageData) {
                Image(uiImage: uiImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(height: 200)
                    .clipped()
                    .cornerRadius(8)
            } else if let path = uiState.photoUri,
                      let uiImage = UIImage(contentsOfFile: path) {
                Image(uiImage: uiImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(height: 200)
                    .clipped()
                    .cornerRadius(8)
            } else {
                Rectangle()
                    .fill(Color.gray.opacity(0.2))
                    .frame(height: 200)
                    .overlay(
                        Text("No photo selected")
                            .foregroundColor(.gray)
                    )
                    .cornerRadius(8)
            }

            Button(action: { showPhotoPicker = true }) {
                Text(uiState.photoUri != nil ? "Change Photo" : "Select Photo")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
    }

    private var selectRackContent: some View {
        List {
            if uiState.racks.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text("No racks. Add a rack first to place items.")
                        .foregroundColor(.secondary)
                    Button(action: onNavigateToAddRack) {
                        Text("Add Rack")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                }
                .listRowInsets(EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16))
            } else {
                ForEach(uiState.racks, id: \.id) { rack in
                    Button(action: { onRackSelected(rack) }) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(rack.name)
                                .font(.headline)
                            if !rack.location.isEmpty {
                                Text(rack.location)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
            }
        }
        .navigationTitle("Select Rack")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Back") {
                    onBackFromSelectRack()
                }
            }
        }
    }

    private var selectSlotContent: some View {
        guard let rackId = uiState.selectedRackId else {
            return AnyView(
                VStack {
                    Text("Select a rack first")
                    Button("Back") {
                        onBackFromSelectSlot()
                    }
                }
            )
        }

        return AnyView(
            RackDetailView(
                rackId: rackId,
                onNavigateBack: { onBackFromSelectSlot() },
                forItemPlacement: true,
                onSlotSelectedForItem: { selectedRackId, slotId in
                    onSlotSelectedForItem(selectedRackId, slotId)
                }
            )
        )
    }
}

