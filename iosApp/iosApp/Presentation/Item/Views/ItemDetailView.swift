import SwiftUI
import PhotosUI
import ComposeApp

struct ItemDetailView: View {
    let uiState: ItemDetailUiState
    let uiEvent: ItemDetailUiEvent?

    let onUpdateName: (String) -> Void
    let onUpdateDescription: (String) -> Void
    let onUpdateQuantity: (Int?) -> Void
    let onUpdateOwner: (String) -> Void
    let onUpdateTagInput: (String) -> Void
    let onAddTag: () -> Void
    let onRemoveTag: (String) -> Void
    let onUpdatePhotoUri: (String?) -> Void
    let onSave: () -> Void
    let onDelete: () -> Void
    let onDismissDeleteConfirm: () -> Void
    let onConfirmDelete: () -> Void
    let onNavigateBack: () -> Void

    @State private var showPhotoPicker = false
    @State private var selectedPhotoItem: PhotosPickerItem? = nil
    @State private var selectedImageData: Data? = nil

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .accessibilityIdentifier("itemDetailLoading")
            } else {
                Form {
                        Section {
                            photoSection
                        }

                        Section(header: Text("Details")) {
                            TextField("Name", text: Binding(
                                get: { uiState.name },
                                set: onUpdateName
                            ))
                            .accessibilityIdentifier("itemDetailNameField")

                            TextField("Description", text: Binding(
                                get: { uiState.description_ },
                                set: onUpdateDescription
                            ), axis: .vertical)
                            .lineLimit(3...6)
                            .accessibilityIdentifier("itemDetailDescriptionField")

                            TextField("Quantity", text: Binding(
                                get: { uiState.quantity?.description ?? "" },
                                set: { newValue in onUpdateQuantity(Int(newValue)) }
                            ))
                            .keyboardType(.numberPad)
                            .accessibilityIdentifier("itemDetailQuantityField")

                            TextField("Owner", text: Binding(
                                get: { uiState.owner },
                                set: onUpdateOwner
                            ))
                            .accessibilityIdentifier("itemDetailOwnerField")
                        }

                        Section(header: Text("Tags")) {
                            HStack {
                                TextField("Add tag", text: Binding(
                                    get: { uiState.tagInput },
                                    set: onUpdateTagInput
                                ))
                                .accessibilityIdentifier("itemDetailTagsInputField")
                                Button("Add") {
                                    onAddTag()
                                }
                                .accessibilityIdentifier("itemDetailAddTagButton")
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
                                            .accessibilityIdentifier("itemDetailTagChip")
                                        }
                                    }
                                }
                            }
                        }

                        if let error = uiState.error {
                            Section {
                                Text(error)
                                    .foregroundColor(.red)
                                    .accessibilityIdentifier("itemDetailErrorText")
                            }
                        }

                        Section {
                            Button(action: onSave) {
                                HStack {
                                    if uiState.isSaving {
                                        ProgressView()
                                            .accessibilityIdentifier("itemDetailSaveProgress")
                                    }
                                    Text("Save")
                                }
                                .frame(maxWidth: .infinity)
                            }
                            .disabled(uiState.isSaving)
                            .accessibilityIdentifier("itemDetailSaveButton")

                            Button(role: .destructive, action: onDelete) {
                                HStack {
                                    Text("Remove item")
                                }
                                .frame(maxWidth: .infinity)
                            }
                            .disabled(uiState.isSaving)
                            .accessibilityIdentifier("itemDetailDeleteButton")
                        }
                }
            }
        }
        .navigationTitle("Item details")
        .navigationBarTitleDisplayMode(.inline)
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
            .onChange(of: itemDetailEventKey(uiEvent)) { _, _ in
                guard let uiEvent else { return }
                if uiEvent is ItemDetailUiEventNavigateBack {
                    onNavigateBack()
                }
            }
            .alert("Remove item?", isPresented: Binding(
                get: { uiState.showDeleteConfirm },
                set: { if !$0 { onDismissDeleteConfirm() } }
            )) {
                Button("Cancel", role: .cancel) {
                    onDismissDeleteConfirm()
                }
                .accessibilityIdentifier("itemDetailDeleteCancelButton")
                Button("Remove", role: .destructive) {
                    onConfirmDelete()
                }
                .accessibilityIdentifier("itemDetailDeleteConfirmButton")
            } message: {
                Text("This cannot be undone.")
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
                Text(uiState.photoUri != nil ? "Change photo" : "Select photo")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .accessibilityIdentifier("itemDetailPhotoPickerButton")
        }
    }
}

private func itemDetailEventKey(_ event: ItemDetailUiEvent?) -> String {
    guard let event else { return "nil" }
    if event is ItemDetailUiEventNavigateBack {
        return "nav-back"
    }
    if let err = event as? ItemDetailUiEventShowError {
        return "error-\(err.message)"
    }
    return String(describing: type(of: event))
}
