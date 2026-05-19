package org.deafsapps.storeit.presentation.account.model

data class AccountUiState(
    val isLoading: Boolean,
    val isSubmitting: Boolean = false,
    val authMode: AccountAuthMode = AccountAuthMode.SignIn,
    val emailInput: String = "",
    val passwordInput: String = "",
    val isAuthenticated: Boolean,
    val accountEmail: String?,
    val failureMessage: String?,
) {
    val isSignInMode: Boolean
        get() = authMode == AccountAuthMode.SignIn

    val isSignUpMode: Boolean
        get() = authMode == AccountAuthMode.SignUp

    val canSubmitCredentials: Boolean
        get() = !isLoading &&
            !isSubmitting &&
            emailInput.isNotBlank() &&
            passwordInput.isNotBlank()

    val canSignOut: Boolean
        get() = isAuthenticated && !isLoading && !isSubmitting

    companion object {
        fun getDefault(): AccountUiState = AccountUiState(
            isLoading = false,
            isSubmitting = false,
            authMode = AccountAuthMode.SignIn,
            emailInput = "",
            passwordInput = "",
            isAuthenticated = false,
            accountEmail = null,
            failureMessage = null,
        )
    }
}

enum class AccountAuthMode {
    SignIn,
    SignUp,
}
