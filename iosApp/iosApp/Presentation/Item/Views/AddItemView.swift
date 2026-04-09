import SwiftUI
import PhotosUI
import Shared

struct AddItemView: View {
    let uiState: AddItemUiState
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
    let onSlotSelectedForItem: (String, AddItemSlotVo) -> Void
    let onNavigateToAddRack: () -> Void
    let onNavigateBack: () -> Void

    @State private var showPhotoPicker = false
    @State private var selectedPhotoItem: PhotosPickerItem? = nil
    @State private var selectedImageData: Data? = nil
    @State private var photoLoadTask: Task<Void, Never>? = nil

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
    }

    private var formContent: some View {
        Form {
                Section {
                    photoSection
                }

                Section(header: Text("details_section_title")) {
                    TextField("item_name_label", text: Binding(
                        get: { uiState.name },
                        set: onUpdateName
                    ))
                    TextField("item_description_label", text: Binding(
                        get: { uiState.description_ },
                        set: onUpdateDescription
                    ), axis: .vertical)
                    .lineLimit(3...6)

                    TextField("item_quantity_label", text: Binding(
                        get: { uiState.quantity?.description ?? "" },
                        set: { newValue in onUpdateQuantity(Int(newValue)) }
                    ))
                    .keyboardType(.numberPad)

                    TextField("item_owner_label", text: Binding(
                        get: { uiState.owner },
                        set: onUpdateOwner
                    ))
                }

                Section(header: Text("tags_section_title")) {
                    HStack {
                        TextField("item_add_tag_placeholder", text: Binding(
                            get: { uiState.tagInput },
                            set: onUpdateTagInput
                        ))
                        Button("common_add") {
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
                            ? "item_place_selected"
                            : "item_place_select"
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
                            Text("item_save_button")
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .disabled(uiState.isLoading)
                }
        }
        .navigationTitle("add_item_title")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("common_cancel") {
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
            photoLoadTask?.cancel()
            photoLoadTask = Task {
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
        .onDisappear {
            photoLoadTask?.cancel()
            photoLoadTask = nil
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
                        Text("photo_none_selected")
                            .foregroundColor(.gray)
                    )
                    .cornerRadius(8)
            }

            Button(action: { showPhotoPicker = true }) {
                Text(uiState.photoUri != nil ? "photo_change" : "photo_select")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
    }

    private var selectRackContent: some View {
        List {
            if uiState.racks.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text("select_rack_empty_message")
                        .foregroundColor(.secondary)
                    Button(action: onNavigateToAddRack) {
                        Text("select_rack_add_button")
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
        .navigationTitle("select_rack_title")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("common_back") {
                    onBackFromSelectRack()
                }
            }
        }
    }

    private var selectSlotContent: some View {
        guard let rackId = uiState.selectedRackId else {
            return AnyView(
                VStack {
                    Text("select_rack_first_message")
                    Button("common_back") {
                        onBackFromSelectSlot()
                    }
                }
            )
        }

        return AnyView(
            RackSlotPickerView(
                rackId: rackId,
                onNavigateBack: { onBackFromSelectSlot() },
                onSlotSelectedForItem: { selectedRackId, slot in
                    onSlotSelectedForItem(selectedRackId, slot)
                }
            )
        )
    }
}

