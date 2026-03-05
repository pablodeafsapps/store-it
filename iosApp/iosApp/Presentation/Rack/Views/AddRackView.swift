import SwiftUI
import UIKit
import ComposeApp
import PhotosUI

struct AddRackView: View {
    let uiState: AddRackUiState
    let uiEvent: Optional<AddRackUiEvent>
    let onUpdateName: (String) -> Void
    let onUpdateDescription: (String) -> Void
    let onUpdateLocation: (String) -> Void
    let onUpdatePhotoUri: (String) -> Void
    let onSaveRack: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var showImagePicker = false
    @State private var selectedPhoto: PhotosPickerItem? = nil
    @State private var selectedImageData: Data? = nil
    
    var body: some View {
        NavigationView {
            Form {
                Section {
                    photoPickerSection
                }
                
                Section {
                    TextField("Name *", text: Binding(
                        get: { uiState.name },
                        set: { onUpdateName($0) }
                    ))
                    
                    TextField("Description", text: Binding(
                        get: { uiState.description_},
                        set: { onUpdateDescription($0) }
                    ), axis: .vertical)
                    .lineLimit(3...6)
                    
                    TextField("Location", text: Binding(
                        get: { uiState.location },
                        set: { onUpdateLocation($0) }
                    ))
                }
                
                if let error = uiState.error {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                    }
                }
                
                Section {
                    Button(action: {
                        onSaveRack()
                    }) {
                        HStack {
                            if uiState.isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                            }
                            Text("Save Rack")
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .accessibilityIdentifier("saveRackButton")
                    .disabled(uiState.isLoading)
                }
            }
            .navigationTitle("Add Rack")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                    .accessibilityIdentifier("cancelButton")
                }
            }
            .photosPicker(
                isPresented: $showImagePicker,
                selection: $selectedPhoto,
                matching: .images
            )
            .onChange(of: selectedPhoto) { oldItem, newItem in
                Task {
                    guard let data = try? await newItem?.loadTransferable(type: Data.self),
                          let image = UIImage(data: data),
                          let imageData = image.jpegData(compressionQuality: 0.8) else { return }
                    selectedImageData = data
                    let tempURL = FileManager.default.temporaryDirectory
                        .appendingPathComponent(UUID().uuidString)
                        .appendingPathExtension("jpg")
                    try? imageData.write(to: tempURL)
                    onUpdatePhotoUri(tempURL.path)
                    
                }
            }
            .onChange(of: onEnum(of: uiEvent)) { _, _ in
                if uiEvent != nil {
                    dismiss()
                }
            }
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
                        Text("No photo selected")
                            .foregroundColor(.gray)
                    )
                    .cornerRadius(8)
            }
            
            Button(action: {
                showImagePicker = true
            }) {
                Text(uiState.photoUri != nil ? "Change Photo" : "Select Photo")
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
            uiState: AddRackUiState.getDefault,
            uiEvent: nil,
            onUpdateName: { _ in },
            onUpdateDescription: {_ in },
            onUpdateLocation: {_ in },
            onUpdatePhotoUri: {_ in },
            onSaveRack: {},
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
