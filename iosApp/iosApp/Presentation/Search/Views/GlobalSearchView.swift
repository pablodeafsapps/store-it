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
                    Text("search_hint_type_to_search")
                        .foregroundColor(.secondary)
                        .padding(.top, 16)
                        .accessibilityIdentifier("searchScreenHint")
                    Spacer()
                } else if uiState.results.isEmpty {
                    Text("search_no_results")
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
                                Text(String(format: NSLocalizedString("search_result_rack_prefix", comment: ""), row.rackName))
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text(String(format: NSLocalizedString("search_result_slot_prefix", comment: ""), row.slotSummary))
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
        .navigationTitle("search_title")
        .navigationBarTitleDisplayMode(.inline)
        .searchable(
            text: Binding(
                get: { uiState.query },
                set: { onQueryChange($0) }
            ),
            placement: .navigationBarDrawer(displayMode: .automatic),
            prompt: "search_prompt"
        )
        .onSubmit(of: .search) {
            let trimmed = uiState.query.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed != uiState.query {
                onQueryChange(trimmed)
            }
        }
    }
}
