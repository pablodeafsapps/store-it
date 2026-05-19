package org.deafsapps.storeit.presentation.account.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.EmailPasswordCredentials
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.usecase.RestoreAccountSessionUseCaseType
import org.deafsapps.storeit.domain.usecase.SignInAccountUseCaseType
import org.deafsapps.storeit.domain.usecase.SignOutAccountUseCaseType
import org.deafsapps.storeit.domain.usecase.SignUpAccountUseCaseType
import org.deafsapps.storeit.presentation.collectUiState

@OptIn(ExperimentalCoroutinesApi::class)
internal class AccountViewModelTest {
    private lateinit var fakeSignUpAccountUseCase: FakeSignUpAccountUseCase
    private lateinit var fakeSignInAccountUseCase: FakeSignInAccountUseCase
    private lateinit var fakeSignOutAccountUseCase: FakeSignOutAccountUseCase
    private lateinit var fakeRestoreAccountSessionUseCase: FakeRestoreAccountSessionUseCase

    @BeforeTest
    fun setUp() {
        fakeSignUpAccountUseCase = FakeSignUpAccountUseCase()
        fakeSignInAccountUseCase = FakeSignInAccountUseCase()
        fakeSignOutAccountUseCase = FakeSignOutAccountUseCase()
        fakeRestoreAccountSessionUseCase = FakeRestoreAccountSessionUseCase()
    }

    @Test
    fun `GIVEN restored active session WHEN AccountViewModel initializes THEN state is authenticated`() = runTest {
        val restoredSession = getAccountSession()
        fakeRestoreAccountSessionUseCase.result = restoredSession.ok()
        val sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)

        advanceUntilIdle()

        val state = states.last()
        assertEquals(expected = true, actual = fakeRestoreAccountSessionUseCase.wasInvoked)
        assertEquals(expected = false, actual = state.isLoading)
        assertEquals(expected = true, actual = state.isAuthenticated)
        assertEquals(expected = restoredSession.email, actual = state.accountEmail)
    }

    @Test
    fun `GIVEN sign in credentials WHEN submit THEN authenticates account`() = runTest {
        val signedInSession = getAccountSession(lastAuthenticatedAt = 20L)
        fakeSignInAccountUseCase.result = signedInSession.ok()
        val sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)

        sut.selectSignInMode()
        sut.onEmailInputChanged(email = "user@example.com")
        sut.onPasswordInputChanged(password = "passw0rd")
        sut.submitCredentials()
        advanceUntilIdle()

        val state = states.last()
        assertEquals(expected = "user@example.com", actual = fakeSignInAccountUseCase.lastCredentials?.email)
        assertEquals(expected = "passw0rd", actual = fakeSignInAccountUseCase.lastCredentials?.password)
        assertEquals(expected = false, actual = state.isSubmitting)
        assertEquals(expected = true, actual = state.isAuthenticated)
        assertEquals(expected = signedInSession.email, actual = state.accountEmail)
    }

    @Test
    fun `GIVEN sign up mode WHEN submit THEN authenticates created account`() = runTest {
        val signedUpSession = getAccountSession(lastAuthenticatedAt = 30L)
        fakeSignUpAccountUseCase.result = signedUpSession.ok()
        val sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)

        sut.selectSignUpMode()
        sut.onEmailInputChanged(email = "new@example.com")
        sut.onPasswordInputChanged(password = "new-pass")
        sut.submitCredentials()
        advanceUntilIdle()

        val state = states.last()
        assertEquals(expected = "new@example.com", actual = fakeSignUpAccountUseCase.lastCredentials?.email)
        assertEquals(expected = "new-pass", actual = fakeSignUpAccountUseCase.lastCredentials?.password)
        assertEquals(expected = true, actual = state.isAuthenticated)
    }

    @Test
    fun `GIVEN auth failure WHEN submit THEN exposes failure message`() = runTest {
        fakeSignInAccountUseCase.result = DomainError.AuthenticationFailed(
            message = "The email or password is incorrect.",
        ).err()
        val sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)

        sut.onEmailInputChanged(email = "bad")
        sut.onPasswordInputChanged(password = "passw0rd")
        sut.submitCredentials()
        advanceUntilIdle()

        val state = states.last()
        assertEquals(expected = false, actual = state.isSubmitting)
        assertEquals(expected = false, actual = state.isAuthenticated)
        assertEquals(expected = "The email or password is incorrect.", actual = state.failureMessage)
    }

    @Test
    fun `GIVEN authenticated account WHEN sign out THEN clears authenticated state`() = runTest {
        val restoredSession = getAccountSession()
        fakeRestoreAccountSessionUseCase.result = restoredSession.ok()
        val sut = createSut(testScope = this)
        val states = collectUiState(uiState = sut.uiState)
        advanceUntilIdle()

        sut.signOut()
        advanceUntilIdle()

        val state = states.last()
        assertEquals(expected = restoredSession.accountId, actual = fakeSignOutAccountUseCase.lastAccountId)
        assertEquals(expected = false, actual = state.isAuthenticated)
        assertEquals(expected = null, actual = state.accountEmail)
    }

    private fun createSut(testScope: TestScope): AccountViewModel = AccountViewModel(
        coroutineScope = CoroutineScope(UnconfinedTestDispatcher(testScope.testScheduler)),
        signUpAccountUseCase = fakeSignUpAccountUseCase,
        signInAccountUseCase = fakeSignInAccountUseCase,
        signOutAccountUseCase = fakeSignOutAccountUseCase,
        restoreAccountSessionUseCase = fakeRestoreAccountSessionUseCase,
    )
}

private fun getAccountSession(lastAuthenticatedAt: Long? = 10L): AccountSession = AccountSession(
    accountId = "account-1",
    email = "user@example.com",
    sessionState = SessionState.Active,
    lastAuthenticatedAt = lastAuthenticatedAt,
)

private class FakeSignUpAccountUseCase : SignUpAccountUseCaseType {
    var result: Result<DomainError, AccountSession> = getAccountSession().ok()
    var lastCredentials: EmailPasswordCredentials? = null

    override suspend fun invoke(input: EmailPasswordCredentials): Result<DomainError, AccountSession> {
        lastCredentials = input
        return result
    }
}

private class FakeSignInAccountUseCase : SignInAccountUseCaseType {
    var result: Result<DomainError, AccountSession> = getAccountSession().ok()
    var lastCredentials: EmailPasswordCredentials? = null

    override suspend fun invoke(input: EmailPasswordCredentials): Result<DomainError, AccountSession> {
        lastCredentials = input
        return result
    }
}

private class FakeSignOutAccountUseCase : SignOutAccountUseCaseType {
    var result: Result<DomainError, Unit> = Unit.ok()
    var lastAccountId: String? = null

    override suspend fun invoke(input: String): Result<DomainError, Unit> {
        lastAccountId = input
        return result
    }
}

private class FakeRestoreAccountSessionUseCase : RestoreAccountSessionUseCaseType {
    var result: Result<DomainError, AccountSession?> = null.ok()
    var wasInvoked: Boolean = false

    override suspend fun invoke(input: Unit): Result<DomainError, AccountSession?> {
        wasInvoked = true
        return result
    }
}
