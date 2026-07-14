import SwiftUI
import Shared

struct RackListView: View {
    let uiState: RackListUiState
    let onAddRackSelected: () -> Void
    let onRackSelected: (RackSummaryVo) -> Void
    let onNavigateToSearch: () -> Void
    let onNavigateToAccount: () -> Void
    let isAccountAuthenticated: Bool
    let accountEmail: String?
    let isAccountReady: Bool
    let isRestoreInProgress: Bool
    let hasPendingSyncWork: Bool
    let hasAccountAttentionState: Bool
    let isDarkModeEnabled: Bool
    let onThemeModeToggle: () -> Void

    var body: some View {
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
        .navigationTitle("racks_title")
        .navigationBarTitleDisplayMode(.inline)
        .accessibilityIdentifier("racksListScreen")
        .toolbar {
            if isAccountAuthenticated {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onNavigateToAccount) {
                        AccountStatusAvatarView(
                            accountEmail: accountEmail,
                            isAuthenticated: isAccountAuthenticated,
                            isAccountReady: isAccountReady,
                            isRestoreInProgress: isRestoreInProgress,
                            hasPendingSyncWork: hasPendingSyncWork,
                            hasAttentionState: hasAccountAttentionState
                        )
                    }
                    .accessibilityIdentifier("rackListAccountStatusButton")
                }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(
                        "account_title",
                        action: onNavigateToAccount,
                    )
                    .accessibilityIdentifier("rackListAccountMenuItem")
                    Button(
                        isDarkModeEnabled ? "rack_list_switch_light_mode" : "rack_list_switch_dark_mode",
                        action: onThemeModeToggle,
                    )
                    .accessibilityIdentifier("rackListThemeMenuItem")
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
                .accessibilityIdentifier("rackListOverflowMenuButton")
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: onAddRackSelected) {
                    Image(systemName: "plus")
                }
                .accessibilityIdentifier("addRackToolbarButton")
            }
        }
    }

    private var searchEntryBar: some View {
        Button(action: onNavigateToSearch) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.secondary)
                Text("rack_search_placeholder")
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
            Text("rack_list_empty_title")
                .font(.title2)
                .foregroundColor(.secondary)
            Text("rack_list_empty_subtitle")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Button(action: onAddRackSelected) {
                Text("add_rack_button")
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

private struct AccountStatusAvatarView: View {
    let accountEmail: String?
    let isAuthenticated: Bool
    let isAccountReady: Bool
    let isRestoreInProgress: Bool
    let hasPendingSyncWork: Bool
    let hasAttentionState: Bool

    var body: some View {
        let symbol: String = {
            if !isAuthenticated { return "person.crop.circle" }
            if hasAttentionState { return "exclamationmark.triangle.fill" }
            if isRestoreInProgress { return "arrow.clockwise.circle.fill" }
            if hasPendingSyncWork { return "icloud.and.arrow.up.fill" }
            if isAccountReady { return "checkmark.seal.fill" }
            return "person.crop.circle.fill"
        }()

        Image(systemName: symbol)
            .foregroundColor(color)
            .accessibilityLabel(accessibilityTitle)
    }

    private var color: Color {
        if hasAttentionState { return .red }
        if isAccountReady { return .green }
        if isRestoreInProgress || hasPendingSyncWork { return .orange }
        return .primary
    }

    private var accessibilityTitle: String {
        if let accountEmail, isAuthenticated {
            return "Account \(accountEmail)"
        }
        return "Account status"
    }
}

private struct RackRowView: View {
    let rack: RackSummaryVo
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
