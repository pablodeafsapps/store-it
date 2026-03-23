import SwiftUI
import ComposeApp

struct RackListView: View {
    let uiState: RackListUiState
    let uiEvent: RackListUiEvent?
    let onAddRackSelected: () -> Void
    let onRackSelected: (Rack) -> Void
    let onNavigateToSearch: () -> Void

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                searchEntryBar
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
            }
            .navigationTitle("Racks")
            .navigationBarTitleDisplayMode(.inline)
            .accessibilityIdentifier("racksListScreen")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onAddRackSelected) {
                        Image(systemName: "plus")
                    }
                    .accessibilityIdentifier("addRackToolbarButton")
                }
            }
        }
    }

    private var searchEntryBar: some View {
        Button(action: onNavigateToSearch) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.secondary)
                Text("Search items by name or description")
                    .foregroundColor(.secondary)
                Spacer(minLength: 0)
            }
            .padding(12)
            .frame(maxWidth: .infinity)
            .background(Color(.secondarySystemBackground))
            .cornerRadius(8)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("rackListSearchField")
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
            Button(action: onAddRackSelected) {
                Text("Add Rack")
                    .frame(maxWidth: .infinity)
            }
            .accessibilityIdentifier("addRackButton")
            .buttonStyle(.borderedProminent)
            .padding(.top, 8)
        }
        .padding(32)
    }

    private var listContent: some View {
        List {
            ForEach(uiState.racks, id: \.id) { rack in
                RackRowView(rack: rack) {
                    onRackSelected(rack)
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
        .accessibilityIdentifier("rackRowViewButton")
        .buttonStyle(.plain)
    }

    private func loadImage(from path: String) -> UIImage? {
        let url = URL(fileURLWithPath: path)
        guard let data = try? Data(contentsOf: url) else { return nil }
        return UIImage(data: data)
    }
}
