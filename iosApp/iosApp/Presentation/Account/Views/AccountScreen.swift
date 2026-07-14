import SwiftUI
import Shared

struct AccountScreen: View {
    @StateObject private var viewModel: ViewModelHolder<AccountViewModel> = ViewModelHolder(IosKoinHelper().getAccountViewModel())
    @StateObject private var syncStatusViewModel: ViewModelHolder<SyncStatusViewModel> = ViewModelHolder(IosKoinHelper().getSyncStatusViewModel())
    let onNavigateBack: () -> Void

    var body: some View {
        Observing(viewModel.sharedVm.uiState) { accountState in
            Observing(syncStatusViewModel.sharedVm.uiState) { syncState in
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        statusText(accountState, syncState)
                            .font(.body)
                            .accessibilityIdentifier("accountStatusText")

                        if !accountState.isAuthenticated {
                            authenticationForm(accountState)
                        }

                        if syncState.canRetry {
                            Button("account_retry_restore") {
                                syncStatusViewModel.sharedVm.retry()
                            }
                            .accessibilityIdentifier("accountRetryRestoreButton")
                        }

                        if let failure = accountState.failureMessage ?? syncState.userMessage {
                            Text(failure)
                                .foregroundColor(.red)
                                .accessibilityIdentifier("accountFailureMessage")
                        }
                    }
                    .padding(16)
                }
                .navigationTitle("account_title")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button("common_back", action: onNavigateBack)
                    }
                    if accountState.isAuthenticated {
                        ToolbarItem(placement: .navigationBarTrailing) {
                            Button("account_sign_out") {
                                viewModel.sharedVm.signOut()
                            }
                            .disabled(!accountState.canSignOut)
                            .accessibilityIdentifier("accountSignOutButton")
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func authenticationForm(_ state: AccountUiState) -> some View {
        Picker("account_title", selection: authModeBinding(state)) {
            Text("account_sign_in").tag(AuthModeSelection.signIn)
            Text("account_sign_up").tag(AuthModeSelection.signUp)
        }
        .pickerStyle(.segmented)
        .accessibilityIdentifier("accountModePicker")

        TextField(
            "account_email_label",
            text: Binding(
                get: { state.emailInput },
                set: { viewModel.sharedVm.onEmailInputChanged(email: $0) }
            )
        )
        .textContentType(.emailAddress)
        .keyboardType(.emailAddress)
        .textInputAutocapitalization(.never)
        .autocorrectionDisabled()
        .textFieldStyle(.roundedBorder)
        .accessibilityIdentifier("accountEmailField")

        SecureField(
            "account_password_label",
            text: Binding(
                get: { state.passwordInput },
                set: { viewModel.sharedVm.onPasswordInputChanged(password: $0) }
            )
        )
        .textContentType(.password)
        .textFieldStyle(.roundedBorder)
        .accessibilityIdentifier("accountPasswordField")

        Button(action: { viewModel.sharedVm.submitCredentials() }) {
            Text(state.isSignInMode ? "account_submit_sign_in" : "account_submit_sign_up")
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .disabled(!state.canSubmitCredentials)
        .accessibilityIdentifier("accountSubmitButton")
    }

    private func authModeBinding(_ state: AccountUiState) -> Binding<AuthModeSelection> {
        Binding(
            get: { state.isSignInMode ? .signIn : .signUp },
            set: { selection in
                switch selection {
                case .signIn:
                    viewModel.sharedVm.selectSignInMode()
                case .signUp:
                    viewModel.sharedVm.selectSignUpMode()
                }
            }
        )
    }

    @ViewBuilder
    private func statusText(_ accountState: AccountUiState, _ syncState: SyncStatusUiState) -> some View {
        if syncState.accountStatusState == AccountSyncStatusState.reconciliationRequired {
            Text("account_reconciliation_required")
        } else if syncState.accountStatusState == AccountSyncStatusState.restorePending {
            Text("account_restore_pending")
        } else if syncState.accountStatusState == AccountSyncStatusState.failed {
            Text("account_failed")
        } else if syncState.accountStatusState == AccountSyncStatusState.backedUp {
            Text("account_backed_up")
        } else if syncState.accountStatusState == AccountSyncStatusState.pending {
            Text("account_pending")
        } else if syncState.accountStatusState == AccountSyncStatusState.signedIn {
            Text(String(format: NSLocalizedString("account_signed_in", comment: ""), accountState.accountEmail ?? ""))
        } else {
            Text("account_local_only")
        }
    }
}

private enum AuthModeSelection: Hashable {
    case signIn
    case signUp
}
