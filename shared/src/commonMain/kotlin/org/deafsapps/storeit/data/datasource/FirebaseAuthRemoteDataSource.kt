package org.deafsapps.storeit.data.datasource

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthResult
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.auth
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.koin.core.annotation.Single

@Single(binds = [AuthRemoteDataSource::class])
internal class FirebaseAuthRemoteDataSource : AuthRemoteDataSource {
    override suspend fun signUp(
        credentials: EmailPasswordCredentials,
    ): Result<DomainError, AuthenticatedRemoteAccount> {
        val validationError = credentials.validate()
        if (validationError != null) {
            return validationError.err()
        }

        return authenticate(
            operation = FirebaseAuthenticationOperation.SignUp,
            block = {
                Firebase.auth.createUserWithEmailAndPassword(
                    email = credentials.email,
                    password = credentials.password,
                )
            },
        )
    }

    override suspend fun signIn(
        credentials: EmailPasswordCredentials,
    ): Result<DomainError, AuthenticatedRemoteAccount> {
        val validationError = credentials.validate()
        if (validationError != null) {
            return validationError.err()
        }

        return authenticate(
            operation = FirebaseAuthenticationOperation.SignIn,
            block = {
                Firebase.auth.signInWithEmailAndPassword(
                    email = credentials.email,
                    password = credentials.password,
                )
            },
        )
    }

    override suspend fun restoreSession(
        session: StoredSessionCredentials,
    ): Result<DomainError, AuthenticatedRemoteAccount> {
        val user = Firebase.auth.currentUser
        if (user == null || user.uid != session.accountId) {
            return DomainError.Unknown(
                message = "Stored Firebase session is unavailable. Sign in again to restore account data.",
            ).err()
        }

        return try {
            user.reload()
            user.toAuthenticatedRemoteAccount().ok()
        } catch (exception: FirebaseAuthException) {
            exception.toDomainError(operation = FirebaseAuthenticationOperation.RestoreSession).err()
        }
    }

    override suspend fun signOut(accountId: String): Result<DomainError, Unit> {
        if (accountId.isBlank()) {
            return DomainError.ValidationError(
                field = "accountId",
                reason = "Account ID cannot be blank",
            ).err()
        }

        return try {
            Firebase.auth.signOut()
            Unit.ok()
        } catch (exception: FirebaseAuthException) {
            exception.toDomainError(operation = FirebaseAuthenticationOperation.SignOut).err()
        }
    }

    private suspend fun authenticate(
        operation: FirebaseAuthenticationOperation,
        block: suspend () -> AuthResult,
    ): Result<DomainError, AuthenticatedRemoteAccount> = try {
        val user = block().user
        user?.toAuthenticatedRemoteAccount()?.ok()
            ?: DomainError.Unknown(message = "Firebase ${operation.description} did not return an authenticated user")
                .err()
    } catch (exception: FirebaseAuthException) {
        exception.toDomainError(operation = operation).err()
    }

    private suspend fun FirebaseUser.toAuthenticatedRemoteAccount(): AuthenticatedRemoteAccount =
        AuthenticatedRemoteAccount(
            accountId = uid,
            email = email.orEmpty(),
            session = StoredSessionCredentials(
                accountId = uid,
                email = email.orEmpty(),
                accessToken = getIdToken(forceRefresh = false),
                lastAuthenticatedAt = metaData?.lastSignInTime?.toLong(),
            ),
            createdAt = metaData?.creationTime?.toLong(),
            lastAuthenticatedAt = metaData?.lastSignInTime?.toLong(),
        )

    private fun EmailPasswordCredentials.validate(): DomainError.ValidationError? = when {
        email.isBlank() -> DomainError.ValidationError(
            field = "email",
            reason = "Email cannot be blank",
        )

        password.isBlank() -> DomainError.ValidationError(
            field = "password",
            reason = "Password cannot be blank",
        )

        else -> null
    }
}

internal enum class FirebaseAuthenticationOperation(
    val description: String,
) {
    SignIn(description = "sign in"),
    SignUp(description = "sign up"),
    RestoreSession(description = "restore account session"),
    SignOut(description = "sign out"),
}

internal enum class FirebaseAuthenticationFailureKind {
    InvalidCredentials,
    InvalidUser,
    UserCollision,
    WeakPassword,
    Unknown,
}

internal data class FirebaseAuthenticationFailure(
    val kind: FirebaseAuthenticationFailureKind,
    val code: String?,
)

internal fun FirebaseAuthException.toDomainError(
    operation: FirebaseAuthenticationOperation,
): DomainError = toFirebaseAuthenticationFailure().toDomainError(
    operation = operation,
    cause = this,
)

internal fun FirebaseAuthenticationFailure.toDomainError(
    operation: FirebaseAuthenticationOperation,
    cause: Throwable?,
): DomainError {
    val normalizedCode = code?.uppercase()

    if (normalizedCode == "CONFIGURATION_NOT_FOUND") {
        return DomainError.ConfigurationError(
            message = "Authentication is unavailable on this build. Check the Firebase configuration.",
            cause = cause,
        )
    }

    return when {
        kind == FirebaseAuthenticationFailureKind.WeakPassword ->
            DomainError.ValidationError(
                field = "password",
                reason = "Password is too weak",
            )

        kind == FirebaseAuthenticationFailureKind.UserCollision ->
            DomainError.AuthenticationFailed(
                message = "An account already exists for this email address.",
                cause = cause,
            )

        operation == FirebaseAuthenticationOperation.SignIn &&
            (kind == FirebaseAuthenticationFailureKind.InvalidCredentials ||
                kind == FirebaseAuthenticationFailureKind.InvalidUser ||
                normalizedCode == "INVALID_LOGIN_CREDENTIALS" ||
                normalizedCode == "EMAIL_NOT_FOUND" ||
                normalizedCode == "USER_NOT_FOUND") ->
            DomainError.AuthenticationFailed(
                message = "The email or password is incorrect.",
                cause = cause,
            )

        operation == FirebaseAuthenticationOperation.SignUp &&
            kind == FirebaseAuthenticationFailureKind.InvalidCredentials ->
            DomainError.ValidationError(
                field = "email",
                reason = "Enter a valid email address",
            )

        normalizedCode == "NETWORK_REQUEST_FAILED" ||
            normalizedCode == "TOO_MANY_ATTEMPTS_TRY_LATER" ||
            normalizedCode == "TOO_MANY_REQUESTS" ||
            normalizedCode == "INTERNAL_ERROR" ->
            DomainError.ServiceUnavailable(
                message = "Authentication is temporarily unavailable. Try again shortly.",
                cause = cause,
            )

        else ->
            DomainError.Unknown(
                message = "Unable to ${operation.description} with Firebase account",
                cause = cause,
            )
    }
}

private fun FirebaseAuthException.toFirebaseAuthenticationFailure(): FirebaseAuthenticationFailure =
    FirebaseAuthenticationFailure(
        kind = when (this) {
            is FirebaseAuthWeakPasswordException -> FirebaseAuthenticationFailureKind.WeakPassword
            is FirebaseAuthUserCollisionException -> FirebaseAuthenticationFailureKind.UserCollision
            is FirebaseAuthInvalidUserException -> FirebaseAuthenticationFailureKind.InvalidUser
            is FirebaseAuthInvalidCredentialsException -> FirebaseAuthenticationFailureKind.InvalidCredentials
            else -> FirebaseAuthenticationFailureKind.Unknown
        },
        code = message.extractFirebaseAuthenticationCode(),
    )

private fun String?.extractFirebaseAuthenticationCode(): String? =
    this
        ?.substringAfterLast(delimiter = "[", missingDelimiterValue = "")
        ?.substringBefore(delimiter = "]")
        ?.trim()
        ?.takeIf { code -> code.isNotEmpty() }
