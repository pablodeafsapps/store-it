import SwiftUI
import PhotosUI
import Shared

struct ItemDetailView: View {
    let uiState: ItemDetailUiState

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
    @State private var photoLoadTask: Task<Void, Never>? = nil

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

                        Section(header: Text("details_section_title")) {
                            TextField("item_name_label", text: Binding(
                                get: { uiState.name },
                                set: onUpdateName
                            ))
                            .accessibilityIdentifier("itemDetailNameField")

                            TextField("item_description_label", text: Binding(
                                get: { uiState.description_ },
                                set: onUpdateDescription
                            ), axis: .vertical)
                            .lineLimit(3...6)
                            .accessibilityIdentifier("itemDetailDescriptionField")

                            TextField("item_quantity_label", text: Binding(
                                get: { uiState.quantity?.description ?? "" },
                                set: { newValue in onUpdateQuantity(Int(newValue)) }
                            ))
                            .keyboardType(.numberPad)
                            .accessibilityIdentifier("itemDetailQuantityField")

                            TextField("item_owner_label", text: Binding(
                                get: { uiState.owner },
                                set: onUpdateOwner
                            ))
                            .accessibilityIdentifier("itemDetailOwnerField")
                        }

                        Section(header: Text("tags_section_title")) {
                            HStack {
                                TextField("item_add_tag_placeholder", text: Binding(
                                    get: { uiState.tagInput },
                                    set: onUpdateTagInput
                                ))
                                .accessibilityIdentifier("itemDetailTagsInputField")
                                Button("common_add") {
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
                                    Text("common_save")
                                }
                                .frame(maxWidth: .infinity)
                            }
                            .disabled(uiState.isSaving)
                            .accessibilityIdentifier("itemDetailSaveButton")

                            Button(role: .destructive, action: onDelete) {
                                HStack {
                                    Text("item_remove_button")
                                }
                                .frame(maxWidth: .infinity)
                            }
                            .disabled(uiState.isSaving)
                            .accessibilityIdentifier("itemDetailDeleteButton")
                        }
                }
            }
        }
        .navigationTitle("item_detail_title")
        .navigationBarTitleDisplayMode(.inline)
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
            .alert("item_remove_confirm_title", isPresented: Binding(
                get: { uiState.showDeleteConfirm },
                set: { if !$0 { onDismissDeleteConfirm() } }
            )) {
                Button("common_cancel", role: .cancel) {
                    onDismissDeleteConfirm()
                }
                .accessibilityIdentifier("itemDetailDeleteCancelButton")
                Button("common_remove", role: .destructive) {
                    onConfirmDelete()
                }
                .accessibilityIdentifier("itemDetailDeleteConfirmButton")
            } message: {
                Text("item_remove_confirm_message")
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
            .accessibilityIdentifier("itemDetailPhotoPickerButton")
        }
    }
}
