package org.deafsapps.storeit.data.datasource

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthResult
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.toUnknownDomainError
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
            operationName = "sign up",
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
            operationName = "sign in",
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
            exception.toUnknownDomainError(message = "Unable to restore Firebase account session").err()
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
            exception.toUnknownDomainError(message = "Unable to sign out from Firebase account").err()
        }
    }

    private suspend fun authenticate(
        operationName: String,
        block: suspend () -> AuthResult,
    ): Result<DomainError, AuthenticatedRemoteAccount> = try {
        val user = block().user
        if (user == null) {
            DomainError.Unknown(message = "Firebase $operationName did not return an authenticated user").err()
        } else {
            user.toAuthenticatedRemoteAccount().ok()
        }
    } catch (exception: FirebaseAuthException) {
        exception.toUnknownDomainError(message = "Unable to $operationName with Firebase account").err()
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
