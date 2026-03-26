import SwiftUI
import ComposeApp

struct GlobalSearchView: View {
    let uiState: SearchUiState
    let onQueryChange: (String) -> Void
    let onItemSelected: (ItemWithPlacement) -> Void

    var body: some View {
        VStack(spacing: 0) {
            Group {
                if uiState.isLoading {
                    Spacer()
                    ProgressView()
                        .accessibilityIdentifier("searchScreenLoading")
                    Spacer()
                } else if let err = uiState.error {
                    Text(err)
                        .foregroundColor(.red)
                        .padding()
                        .accessibilityIdentifier("searchScreenError")
                    Spacer()
                } else if uiState.query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text("Type to search your items.")
                        .foregroundColor(.secondary)
                        .padding(.top, 16)
                        .accessibilityIdentifier("searchScreenHint")
                    Spacer()
                } else if uiState.results.isEmpty {
                    Text("No items match your search.")
                        .foregroundColor(.secondary)
                        .padding(.top, 16)
                        .accessibilityIdentifier("searchScreenNoResults")
                    Spacer()
                } else {
                    List(uiState.results, id: \.item.id) { row in
                        Button {
                            onItemSelected(row)
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(row.item.name)
                                    .font(.headline)
                                    .foregroundColor(.primary)
                                Text("Rack: \(row.rackName)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text("Slot: \(row.slotSummary)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .accessibilityIdentifier("searchResultRow_\(row.item.id)")
                    }
                    .accessibilityIdentifier("searchScreenResults")
                }
            }
        }
        .navigationTitle("Search items")
        .navigationBarTitleDisplayMode(.inline)
        .searchable(
            text: Binding(
                get: { uiState.query },
                set: { onQueryChange($0) }
            ),
            placement: .navigationBarDrawer(displayMode: .automatic),
            prompt: "Search by name or description"
        )
        .onSubmit(of: .search) {
            let trimmed = uiState.query.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed != uiState.query {
                onQueryChange(trimmed)
            }
        }
    }
}
