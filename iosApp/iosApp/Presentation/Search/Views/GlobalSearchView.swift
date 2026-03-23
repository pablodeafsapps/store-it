import SwiftUI
import ComposeApp

struct GlobalSearchView: View {
    let uiState: SearchUiState
    let onQueryChange: (String) -> Void
    let onItemSelected: (ItemWithPlacement) -> Void
    let onNavigateBack: () -> Void

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                TextField(
                    "Search by name or description",
                    text: Binding(
                        get: { uiState.query },
                        set: { onQueryChange($0) }
                    )
                )
                .textFieldStyle(.roundedBorder)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .accessibilityIdentifier("searchScreenQueryField")

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
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Back") {
                        onNavigateBack()
                    }
                    .accessibilityIdentifier("searchScreenBackButton")
                }
            }
        }
    }
}
