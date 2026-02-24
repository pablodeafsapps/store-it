import SwiftUI
import UIKit
import ComposeApp
import PhotosUI

struct AddRackView: View {
    @StateObject private var viewModel = AddRackViewModelProvider.shared.createViewModel()
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
                        get: { viewModel.uiState.name },
                        set: { viewModel.updateName(name: $0) }
                    ))
                    
                    TextField("Description", text: Binding(
                        get: { viewModel.uiState.description },
                        set: { viewModel.updateDescription(description: $0) }
                    ), axis: .vertical)
                    .lineLimit(3...6)
                    
                    TextField("Location", text: Binding(
                        get: { viewModel.uiState.location },
                        set: { viewModel.updateLocation(location: $0) }
                    ))
                }
                
                if let error = viewModel.uiState.error {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                    }
                }
                
                Section {
                    Button(action: {
                        viewModel.saveRack()
                    }) {
                        HStack {
                            if viewModel.uiState.isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                            }
                            Text("Save Rack")
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .disabled(viewModel.uiState.isLoading)
                }
            }
            .navigationTitle("Add Rack")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .photosPicker(
                isPresented: $showImagePicker,
                selection: $selectedPhoto,
                matching: .images
            )
            // .onChange(of: selectedPhoto) { newItem in
            //     Task {
            //         if let data = try? await newItem?.loadTransferable(type: Data.self) {
            //             selectedImageData = data
            //             if let data = data,
            //                let image = UIImage(data: data),
            //                let imageData = image.jpegData(compressionQuality: 0.8) {
            //                 let tempURL = FileManager.default.temporaryDirectory
            //                     .appendingPathComponent(UUID().uuidString)
            //                     .appendingPathExtension("jpg")
            //                 try? imageData.write(to: tempURL)
            //                 viewModel.updatePhotoUri(uri: tempURL.path)
            //             }
            //         }
            //     }
            // }
            // .onChange(of: viewModel.uiEvent) { event in
            //     if event != nil {
            //         dismiss()
            //         viewModel.clearEvent()
            //     }
            // }
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
                Text(viewModel.uiState.photoUri != nil ? "Change Photo" : "Select Photo")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
    }
}

struct AddRackView_Previews: PreviewProvider {
    static var previews: some View {
        AddRackView()
    }
}
