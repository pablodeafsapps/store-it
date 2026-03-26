import SwiftUI
import UIKit
import ComposeApp
import PhotosUI

struct AddRackView: View {
    @StateObject private var addRackViewModel: ViewModelHolder<AddRackViewModel>
    let onNavigateBack: () -> Void

    @State private var showImagePicker = false
    @State private var selectedPhoto: PhotosPickerItem? = nil
    @State private var selectedImageData: Data? = nil
    @State private var photoLoadTask: Task<Void, Never>? = nil
    
    init(onNavigateBack: @escaping () -> Void) {
        _addRackViewModel = StateObject(wrappedValue: ViewModelHolder(IosKoinHelper().getAddRackViewModel()))
        self.onNavigateBack = onNavigateBack
    }
    
    var body: some View {
        Observing(addRackViewModel.sharedVm.uiState) { state in
            content(state: state)
        }
        .task {
            for await event in addRackViewModel.sharedVm.uiEvent {
                if event != nil {
                    onNavigateBack()
                }
            }
        }
    }

    private func content(state: AddRackUiState) -> some View {
        Form {
            Section {
                photoPickerSection
            }
            detailsSection(state: state)
            errorSection(error: state.error)
            saveSection(isLoading: state.isLoading)
        }
        .navigationTitle("add_rack_title")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("common_cancel") {
                    onNavigateBack()
                }
                .accessibilityIdentifier("cancelButton")
            }
        }
        .photosPicker(
            isPresented: $showImagePicker,
            selection: $selectedPhoto,
            matching: .images
        )
        .onChange(of: selectedPhoto) { _, newItem in
            persistSelectedPhoto(newItem)
        }
        .onDisappear {
            photoLoadTask?.cancel()
            photoLoadTask = nil
        }
        .navigationBarBackButtonHidden(true)
    }

    @ViewBuilder
    private func detailsSection(state: AddRackUiState) -> some View {
        Section {
            TextField("rack_name_required_label", text: Binding(
                get: { state.name },
                set: { addRackViewModel.sharedVm.onUpdateName(name: $0) }
            ))
            .accessibilityIdentifier("addRackNameField")

            TextField("rack_description_label", text: Binding(
                get: { state.description_ },
                set: { addRackViewModel.sharedVm.onUpdateDescription(description: $0) }
            ), axis: .vertical)
            .lineLimit(3...6)

            TextField("rack_location_label", text: Binding(
                get: { state.location },
                set: { addRackViewModel.sharedVm.onUpdateLocation(location: $0) }
            ))
        }
    }

    @ViewBuilder
    private func errorSection(error: String?) -> some View {
        if let error {
            Section {
                Text(error)
                    .foregroundColor(.red)
            }
        }
    }

    private func saveSection(isLoading: Bool) -> some View {
        Section {
            Button(action: {
                addRackViewModel.sharedVm.onSaveRack()
            }) {
                HStack {
                    if isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle())
                    }
                    Text("rack_save_button")
                }
                .frame(maxWidth: .infinity)
            }
            .accessibilityIdentifier("saveRackButton")
            .disabled(isLoading)
        }
    }

    private func persistSelectedPhoto(_ newItem: PhotosPickerItem?) {
        photoLoadTask?.cancel()
        photoLoadTask = Task {
            guard let data = try? await newItem?.loadTransferable(type: Data.self),
                  let image = UIImage(data: data),
                  let imageData = image.jpegData(compressionQuality: 0.8) else { return }
            selectedImageData = data
            let tempURL = FileManager.default.temporaryDirectory
                .appendingPathComponent(UUID().uuidString)
                .appendingPathExtension("jpg")
            try? imageData.write(to: tempURL)
            addRackViewModel.sharedVm.onUpdatePhotoUri(uri: tempURL.path)
        }
    }
    
    private var photoPickerSection: some View {
        VStack(spacing: 12) {
            if let imageData = selectedImageData,
               let uiImage = UIImage(data: imageData) {
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
            
            Button(action: {
                showImagePicker = true
            }) {
                Text(addRackViewModel.sharedVm.uiState.value.photoUri != nil ? "photo_change" : "photo_select")
                    .frame(maxWidth: .infinity)
            }
            .accessibilityIdentifier("changeOrSelectPhotoButton")
            .buttonStyle(.borderedProminent)
        }
    }
}

private struct AddRackView_Previews: PreviewProvider {
    static var previews: some View {
        AddRackView(
            onNavigateBack: {},
        )
    }
}


private extension AddRackUiState {
    static let getDefault: AddRackUiState =
        AddRackUiState(
            name: "",
            description: "",
            location: "",
            photoUri: nil,
            isLoading: false,
            error: nil,
            isSuccess: true,
        )
}
