package org.deafsapps.storeit.androidapp.presentation.account.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import org.deafsapps.storeit.androidapp.R
import org.deafsapps.storeit.androidapp.design.Dimens
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.presentation.sync.model.AccountSyncHeaderState
import org.deafsapps.storeit.presentation.sync.model.AccountSyncStatusState
import org.deafsapps.storeit.presentation.account.model.AccountUiState
import org.deafsapps.storeit.presentation.sync.model.SyncStatusUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountScreen(
    accountUiState: AccountUiState,
    syncUiState: SyncStatusUiState,
    onSelectSignIn: () -> Unit,
    onSelectSignUp: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmitCredentials: () -> Unit,
    onSignOut: () -> Unit,
    onRetryRestore: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.account_title)) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(text = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (accountUiState.isAuthenticated) {
                        TextButton(
                            onClick = onSignOut,
                            enabled = accountUiState.canSignOut,
                            modifier = Modifier.testTag("accountSignOutButton"),
                        ) {
                            Text(text = stringResource(R.string.account_sign_out))
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.listItemSpacing),
        ) {
            AccountStatusContent(
                accountStatusState = syncUiState.accountStatusState,
                accountHeaderState = syncUiState.accountHeaderState,
                isAuthenticated = accountUiState.isAuthenticated,
                accountEmail = accountUiState.accountEmail,
            )
            if (!accountUiState.isAuthenticated) {
                AccountAuthenticationForm(
                    isSignInMode = accountUiState.isSignInMode,
                    isSignUpMode = accountUiState.isSignUpMode,
                    emailInput = accountUiState.emailInput,
                    passwordInput = accountUiState.passwordInput,
                    canSubmitCredentials = accountUiState.canSubmitCredentials,
                    onSelectSignIn = onSelectSignIn,
                    onSelectSignUp = onSelectSignUp,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onSubmitCredentials = onSubmitCredentials,
                )
            }
            if (syncUiState.canRetry) {
                TextButton(
                    onClick = onRetryRestore,
                    modifier = Modifier.testTag("accountRetryRestoreButton"),
                ) {
                    Text(text = stringResource(R.string.account_retry_restore))
                }
            }
            val failureMessage = accountUiState.failureMessage ?: syncUiState.userMessage
            failureMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("accountFailureMessage"),
                )
            }
        }
    }
}

@Composable
private fun AccountAuthenticationForm(
    isSignInMode: Boolean,
    isSignUpMode: Boolean,
    emailInput: String,
    passwordInput: String,
    canSubmitCredentials: Boolean,
    onSelectSignIn: () -> Unit,
    onSelectSignUp: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmitCredentials: () -> Unit,
) {
    AuthModeSelector(
        isSignInMode = isSignInMode,
        isSignUpMode = isSignUpMode,
        onSelectSignIn = onSelectSignIn,
        onSelectSignUp = onSelectSignUp,
    )
    OutlinedTextField(
        value = emailInput,
        onValueChange = onEmailChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("accountEmailField"),
        label = { Text(text = stringResource(R.string.account_email_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
    )
    OutlinedTextField(
        value = passwordInput,
        onValueChange = onPasswordChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("accountPasswordField"),
        label = { Text(text = stringResource(R.string.account_password_label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
    Button(
        onClick = onSubmitCredentials,
        enabled = canSubmitCredentials,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("accountSubmitButton"),
    ) {
        Text(
            text = stringResource(
                if (isSignInMode) {
                    R.string.account_submit_sign_in
                } else {
                    R.string.account_submit_sign_up
                },
            ),
        )
    }
}

@Composable
private fun AuthModeSelector(
    isSignInMode: Boolean,
    isSignUpMode: Boolean,
    onSelectSignIn: () -> Unit,
    onSelectSignUp: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.listItemSpacing),
        modifier = Modifier.fillMaxWidth(),
    ) {
        FilterChip(
            selected = isSignInMode,
            onClick = onSelectSignIn,
            label = { Text(text = stringResource(R.string.account_sign_in)) },
            modifier = Modifier
                .weight(1f)
                .testTag("accountSignInModeButton"),
        )
        FilterChip(
            selected = isSignUpMode,
            onClick = onSelectSignUp,
            label = { Text(text = stringResource(R.string.account_sign_up)) },
            modifier = Modifier
                .weight(1f)
                .testTag("accountSignUpModeButton"),
        )
    }
}

@Composable
private fun AccountStatusContent(
    accountStatusState: AccountSyncStatusState,
    accountHeaderState: AccountSyncHeaderState,
    isAuthenticated: Boolean,
    accountEmail: String?,
) {
    val statusText = when (accountStatusState) {
        AccountSyncStatusState.ReconciliationRequired -> stringResource(R.string.account_reconciliation_required)
        AccountSyncStatusState.RestorePending -> stringResource(R.string.account_restore_pending)
        AccountSyncStatusState.Failed -> stringResource(R.string.account_failed)
        AccountSyncStatusState.BackedUp -> stringResource(R.string.account_backed_up)
        AccountSyncStatusState.Pending -> stringResource(R.string.account_pending)
        AccountSyncStatusState.SignedIn -> stringResource(
            R.string.account_signed_in,
            accountEmail.orEmpty(),
        )
        AccountSyncStatusState.LocalOnly -> stringResource(R.string.account_local_only)
    }
    val headerTitle = when (accountHeaderState) {
        AccountSyncHeaderState.Reconciliation -> stringResource(R.string.account_status_title_reconciliation)
        AccountSyncHeaderState.Attention -> stringResource(R.string.account_status_title_attention)
        AccountSyncHeaderState.Restoring -> stringResource(R.string.account_status_title_restoring)
        AccountSyncHeaderState.Ready -> stringResource(R.string.account_status_title_ready)
        AccountSyncHeaderState.Pending -> stringResource(R.string.account_status_title_pending)
        AccountSyncHeaderState.Connected -> stringResource(R.string.account_status_title_connected)
        AccountSyncHeaderState.LocalOnly -> stringResource(R.string.account_status_title_local_only)
    }
    val isAccountReady = accountHeaderState == AccountSyncHeaderState.Ready
    val isRestoreInProgress = accountHeaderState == AccountSyncHeaderState.Restoring
    val hasPendingSyncWork = accountHeaderState == AccountSyncHeaderState.Pending
    val hasAttentionState = accountHeaderState == AccountSyncHeaderState.Attention ||
        accountHeaderState == AccountSyncHeaderState.Reconciliation
    Surface(
        tonalElevation = Dimens.cardElevation,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("accountStatusCard"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.listItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountStatusAvatar(
                accountEmail = accountEmail,
                isAuthenticated = isAuthenticated,
                isAccountReady = isAccountReady,
                isRestoreInProgress = isRestoreInProgress,
                hasPendingSyncWork = hasPendingSyncWork,
                hasAttentionState = hasAttentionState,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("accountStatusText"),
                )
            }
            if (isAccountReady) {
                AccountReadyIndicator(
                    modifier = Modifier.testTag("accountReadyIndicator"),
                )
            }
        }
    }
}

@Composable
private fun AccountReadyIndicator(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Dimens.spacingSmall + Dimens.spacingMedium)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
            ),
    ) {
        Spacer(
            modifier = Modifier
                .align(Alignment.Center)
                .size(Dimens.spacingSmall)
                .background(
                    color = Color.White,
                    shape = CircleShape,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountScreenSignedOutPreview() {
    MaterialTheme {
        AccountScreen(
            accountUiState = AccountUiState.getDefault().copy(
                isLoading = false,
                authMode = org.deafsapps.storeit.presentation.account.model.AccountAuthMode.SignIn,
                emailInput = "user@example.com",
                passwordInput = "passw0rd",
            ),
            syncUiState = SyncStatusUiState.getDefault(),
            onSelectSignIn = {},
            onSelectSignUp = {},
            onEmailChange = {},
            onPasswordChange = {},
            onSubmitCredentials = {},
            onSignOut = {},
            onRetryRestore = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountScreenReadyPreview() {
    MaterialTheme {
        AccountScreen(
            accountUiState = AccountUiState.getDefault().copy(
                isLoading = false,
                isAuthenticated = true,
                accountEmail = "user@example.com",
            ),
            syncUiState = SyncStatusUiState.getDefault().copy(
                isAuthenticated = true,
                dataMode = DataMode.AccountBackedSynchronized,
                syncStatus = SyncStatus.Synchronized,
            ),
            onSelectSignIn = {},
            onSelectSignUp = {},
            onEmailChange = {},
            onPasswordChange = {},
            onSubmitCredentials = {},
            onSignOut = {},
            onRetryRestore = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountScreenRestorePendingPreview() {
    MaterialTheme {
        AccountScreen(
            accountUiState = AccountUiState.getDefault().copy(
                isLoading = false,
                isAuthenticated = true,
                accountEmail = "user@example.com",
            ),
            syncUiState = SyncStatusUiState.getDefault().copy(
                isAuthenticated = true,
                dataMode = DataMode.AccountBackedPendingSync,
                syncStatus = SyncStatus.RestorePending,
                pendingOperationCount = 2,
            ),
            onSelectSignIn = {},
            onSelectSignUp = {},
            onEmailChange = {},
            onPasswordChange = {},
            onSubmitCredentials = {},
            onSignOut = {},
            onRetryRestore = {},
            onNavigateBack = {},
        )
    }
}
