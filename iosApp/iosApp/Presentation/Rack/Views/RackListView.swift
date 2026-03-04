import SwiftUI
import ComposeApp

struct RackListView: View {
    let uiState: RackListUiState
    let uiEvent: RackListUiEvent?
    let onAddRackSelect: () -> Void
    let onRackSelect: (Rack) -> Void

    var body: some View {
        NavigationView {
            ZStack {
                if uiState.isLoading {
                    ProgressView()
                        .scaleEffect(1.2)
                } else if uiState.racks.isEmpty {
                    emptyState
                } else {
                    listContent
                }
                if let error = uiState.error {
                    VStack {
                        Text(error)
                            .foregroundColor(.red)
                            .padding()
                        Spacer()
                    }
                }
            }
            .navigationTitle("Racks")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onAddRackSelect) {
                        Image(systemName: "plus")
                    }
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Text("No racks yet")
                .font(.title2)
                .foregroundColor(.secondary)
            Text("Add your first storage rack to get started")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Button(action: onAddRackSelect) {
                Text("Add Rack")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .padding(.top, 8)
        }
        .padding(32)
    }

    private var listContent: some View {
        List {
            ForEach(uiState.racks, id: \.id) { rack in
                RackRowView(rack: rack) {
                    onRackSelect(rack)
                }
            }
        }
    }
}

private struct RackRowView: View {
    let rack: Rack
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 8) {
                if let path = rack.photoUri,
                   let uiImage = loadImage(from: path) {
                    Image(uiImage: uiImage)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(height: 120)
                        .clipped()
                        .cornerRadius(8)
                }
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
        .buttonStyle(.plain)
    }

    private func loadImage(from path: String) -> UIImage? {
        let url = URL(fileURLWithPath: path)
        guard let data = try? Data(contentsOf: url) else { return nil }
        return UIImage(data: data)
    }
}
