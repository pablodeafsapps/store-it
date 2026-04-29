package org.deafsapps.storeit.androidapp.presentation.account.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import org.deafsapps.storeit.androidapp.R
import org.deafsapps.storeit.androidapp.design.Dimens
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.presentation.account.model.AccountUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountScreen(
    uiState: AccountUiState,
    onSelectSignIn: () -> Unit,
    onSelectSignUp: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmitCredentials: () -> Unit,
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
                requiresReconciliation = uiState.requiresReconciliation,
                canRetryRestore = uiState.canRetryRestore,
                syncStatus = uiState.syncStatus,
                isDataBackedUp = uiState.isDataBackedUp,
                hasPendingSyncWork = uiState.hasPendingSyncWork,
                isAuthenticated = uiState.isAuthenticated,
                accountEmail = uiState.accountEmail,
            )
            AuthModeSelector(
                isSignInMode = uiState.isSignInMode,
                isSignUpMode = uiState.isSignUpMode,
                onSelectSignIn = onSelectSignIn,
                onSelectSignUp = onSelectSignUp,
            )
            OutlinedTextField(
                value = uiState.emailInput,
                onValueChange = onEmailChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("accountEmailField"),
                label = { Text(text = stringResource(R.string.account_email_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            OutlinedTextField(
                value = uiState.passwordInput,
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
                enabled = uiState.canSubmitCredentials,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("accountSubmitButton"),
            ) {
                Text(
                    text = stringResource(
                        if (uiState.isSignInMode) {
                            R.string.account_submit_sign_in
                        } else {
                            R.string.account_submit_sign_up
                        },
                    ),
                )
            }
            if (uiState.canRetryRestore) {
                TextButton(
                    onClick = onRetryRestore,
                    modifier = Modifier.testTag("accountRetryRestoreButton"),
                ) {
                    Text(text = stringResource(R.string.account_retry_restore))
                }
            }
            uiState.failureMessage?.let { failureMessage ->
                Text(
                    text = failureMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("accountFailureMessage"),
                )
            }
        }
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
    requiresReconciliation: Boolean,
    canRetryRestore: Boolean,
    syncStatus: SyncStatus,
    isDataBackedUp: Boolean,
    hasPendingSyncWork: Boolean,
    isAuthenticated: Boolean,
    accountEmail: String?,
) {
    val statusText = when {
        requiresReconciliation -> stringResource(R.string.account_reconciliation_required)
        canRetryRestore -> stringResource(R.string.account_restore_pending)
        syncStatus == SyncStatus.Failed -> stringResource(R.string.account_failed)
        isDataBackedUp -> stringResource(R.string.account_backed_up)
        hasPendingSyncWork -> stringResource(R.string.account_pending)
        isAuthenticated -> stringResource(
            R.string.account_signed_in,
            accountEmail.orEmpty(),
        )
        else -> stringResource(R.string.account_local_only)
    }
    Text(
        text = statusText,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.testTag("accountStatusText"),
    )
}
