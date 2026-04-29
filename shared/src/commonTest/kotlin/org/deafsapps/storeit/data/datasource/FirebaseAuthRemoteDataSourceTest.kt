package org.deafsapps.storeit.data.datasource

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.deafsapps.storeit.domain.model.DomainError

class FirebaseAuthRemoteDataSourceTest {
    @Test
    fun `GIVEN configuration not found code WHEN mapping sign in failure THEN returns ConfigurationError`() {
        val result = FirebaseAuthenticationFailure(
            kind = FirebaseAuthenticationFailureKind.Unknown,
            code = "CONFIGURATION_NOT_FOUND",
        ).toDomainError(
            operation = FirebaseAuthenticationOperation.SignIn,
            cause = null,
        )

        assertIs<DomainError.ConfigurationError>(value = result)
        assertEquals(
            expected = "Authentication is unavailable on this build. Check the Firebase configuration.",
            actual = result.message,
        )
    }

    @Test
    fun `GIVEN invalid credentials WHEN mapping sign in failure THEN returns AuthenticationFailed`() {
        val result = FirebaseAuthenticationFailure(
            kind = FirebaseAuthenticationFailureKind.InvalidCredentials,
            code = null,
        ).toDomainError(
            operation = FirebaseAuthenticationOperation.SignIn,
            cause = null,
        )

        assertIs<DomainError.AuthenticationFailed>(value = result)
        assertEquals(expected = "The email or password is incorrect.", actual = result.message)
    }

    @Test
    fun `GIVEN weak password WHEN mapping sign up failure THEN returns password ValidationError`() {
        val result = FirebaseAuthenticationFailure(
            kind = FirebaseAuthenticationFailureKind.WeakPassword,
            code = null,
        ).toDomainError(
            operation = FirebaseAuthenticationOperation.SignUp,
            cause = null,
        )

        assertIs<DomainError.ValidationError>(value = result)
        assertEquals(expected = "password", actual = result.field)
        assertEquals(expected = "Password is too weak", actual = result.reason)
    }

    @Test
    fun `GIVEN internal error code WHEN mapping sign in failure THEN returns ServiceUnavailable`() {
        val result = FirebaseAuthenticationFailure(
            kind = FirebaseAuthenticationFailureKind.Unknown,
            code = "INTERNAL_ERROR",
        ).toDomainError(
            operation = FirebaseAuthenticationOperation.SignIn,
            cause = null,
        )

        assertIs<DomainError.ServiceUnavailable>(value = result)
        assertEquals(
            expected = "Authentication is temporarily unavailable. Try again shortly.",
            actual = result.message,
        )
    }
}
