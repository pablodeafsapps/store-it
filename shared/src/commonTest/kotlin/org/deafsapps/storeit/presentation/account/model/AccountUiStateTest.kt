package org.deafsapps.storeit.presentation.account.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AccountUiStateTest {
    @Test
    fun `GIVEN default state WHEN read THEN it is signed out and sign in mode`() {
        val sut = AccountUiState.getDefault()

        assertEquals(expected = false, actual = sut.isLoading)
        assertEquals(expected = false, actual = sut.isAuthenticated)
        assertEquals(expected = null, actual = sut.accountEmail)
        assertEquals(expected = true, actual = sut.isSignInMode)
    }

    @Test
    fun `GIVEN sign up mode WHEN read THEN mode flags are consistent`() {
        val sut = AccountUiState.getDefault().copy(authMode = AccountAuthMode.SignUp)

        assertEquals(expected = false, actual = sut.isSignInMode)
        assertEquals(expected = true, actual = sut.isSignUpMode)
    }

    @Test
    fun `GIVEN non blank credentials and idle state WHEN read THEN submit is enabled`() {
        val sut = AccountUiState.getDefault().copy(
            emailInput = "user@example.com",
            passwordInput = "passw0rd",
        )

        assertEquals(expected = true, actual = sut.canSubmitCredentials)
    }
}
