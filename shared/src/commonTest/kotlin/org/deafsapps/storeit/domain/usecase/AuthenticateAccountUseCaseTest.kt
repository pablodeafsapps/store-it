package org.deafsapps.storeit.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.Account
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.EmailPasswordCredentials
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.repository.AccountRepository

class SignUpAccountUseCaseTest {
    private lateinit var sut: SignUpAccountUseCase
    private lateinit var fakeAccountRepository: FakeAccountRepository

    @BeforeTest
    fun setUp() {
        fakeAccountRepository = FakeAccountRepository()
        sut = SignUpAccountUseCase(accountRepository = fakeAccountRepository)
    }

    @Test
    fun `GIVEN account repository signs up session WHEN invoke THEN returns signed in account session`() = runTest {
        val credentials = EmailPasswordCredentials(
            email = "user@example.com",
            password = "password123",
        )
        val expectedSession = accountSession()
        fakeAccountRepository.signUpResult = expectedSession.ok()

        val result = sut.invoke(input = credentials)

        assertTrue(actual = result.isOk)
        assertEquals(expected = credentials, actual = fakeAccountRepository.signUpCredentials)
        assertEquals(expected = expectedSession, actual = result.getOrNull())
    }

    @Test
    fun `GIVEN account repository returns validation error WHEN invoke THEN returns validation error`() = runTest {
        val credentials = EmailPasswordCredentials(
            email = "invalid",
            password = "short",
        )
        fakeAccountRepository.signUpResult = DomainError.ValidationError(
            field = "email",
            reason = "Invalid email format",
        ).err()

        val result = sut.invoke(input = credentials)

        assertTrue(actual = result.isErr)
        assertEquals(expected = credentials, actual = fakeAccountRepository.signUpCredentials)
        assertTrue(actual = result.failureOrNull() is DomainError.ValidationError)
    }
}

class SignInAccountUseCaseTest {
    private lateinit var sut: SignInAccountUseCase
    private lateinit var fakeAccountRepository: FakeAccountRepository

    @BeforeTest
    fun setUp() {
        fakeAccountRepository = FakeAccountRepository()
        sut = SignInAccountUseCase(accountRepository = fakeAccountRepository)
    }

    @Test
    fun `GIVEN account repository signs in session WHEN invoke THEN returns authenticated account session`() = runTest {
        val credentials = EmailPasswordCredentials(
            email = "user@example.com",
            password = "password123",
        )
        val expectedSession = accountSession(lastAuthenticatedAt = 20L)
        fakeAccountRepository.signInResult = expectedSession.ok()

        val result = sut.invoke(input = credentials)

        assertTrue(actual = result.isOk)
        assertEquals(expected = credentials, actual = fakeAccountRepository.signInCredentials)
        assertEquals(expected = expectedSession, actual = result.getOrNull())
    }

    @Test
    fun `GIVEN account repository returns unknown error WHEN invoke THEN returns unknown error`() = runTest {
        val credentials = EmailPasswordCredentials(
            email = "user@example.com",
            password = "password123",
        )
        fakeAccountRepository.signInResult = DomainError.Unknown(message = "Provider unavailable").err()

        val result = sut.invoke(input = credentials)

        assertTrue(actual = result.isErr)
        assertEquals(expected = credentials, actual = fakeAccountRepository.signInCredentials)
        assertTrue(actual = result.failureOrNull() is DomainError.Unknown)
    }
}

class RestoreAccountSessionUseCaseTest {
    private lateinit var sut: RestoreAccountSessionUseCase
    private lateinit var fakeAccountRepository: FakeAccountRepository

    @BeforeTest
    fun setUp() {
        fakeAccountRepository = FakeAccountRepository()
        sut = RestoreAccountSessionUseCase(accountRepository = fakeAccountRepository)
    }

    @Test
    fun `GIVEN account repository restores session WHEN invoke THEN returns active account session`() = runTest {
        val expectedSession = accountSession(lastAuthenticatedAt = 30L)
        fakeAccountRepository.restoreSessionResult = expectedSession.ok()

        val result = sut.invoke()

        assertTrue(actual = result.isOk)
        assertEquals(expected = expectedSession, actual = result.getOrNull())
    }

    @Test
    fun `GIVEN account repository has no stored session WHEN invoke THEN returns null session`() = runTest {
        fakeAccountRepository.restoreSessionResult = null.ok()

        val result = sut.invoke()

        assertTrue(actual = result.isOk)
        assertEquals(expected = null, actual = result.getOrNull())
    }

    @Test
    fun `GIVEN account repository returns unknown error WHEN invoke THEN returns unknown error`() = runTest {
        fakeAccountRepository.restoreSessionResult = DomainError.Unknown(
            message = "Authenticated but restore pending",
        ).err()

        val result = sut.invoke()

        assertTrue(actual = result.isErr)
        assertTrue(actual = result.failureOrNull() is DomainError.Unknown)
    }
}

private fun accountSession(lastAuthenticatedAt: Long? = 10L): AccountSession = AccountSession(
    accountId = "account-1",
    email = "user@example.com",
    sessionState = SessionState.Active,
    lastAuthenticatedAt = lastAuthenticatedAt,
)

private class FakeAccountRepository : AccountRepository {
    var signUpResult: Result<DomainError, AccountSession> = DomainError.Unknown(
        message = "signUpResult not configured",
    ).err()
    var signInResult: Result<DomainError, AccountSession> = DomainError.Unknown(
        message = "signInResult not configured",
    ).err()
    var restoreSessionResult: Result<DomainError, AccountSession?> = DomainError.Unknown(
        message = "restoreSessionResult not configured",
    ).err()

    var signUpCredentials: EmailPasswordCredentials? = null
    var signInCredentials: EmailPasswordCredentials? = null

    override fun observeAccount(): Flow<Result<DomainError, Account?>> = flowOf(null.ok())

    override fun observeSession(): Flow<Result<DomainError, AccountSession?>> = flowOf(null.ok())

    override suspend fun signUp(credentials: EmailPasswordCredentials): Result<DomainError, AccountSession> {
        signUpCredentials = credentials
        return signUpResult
    }

    override suspend fun signIn(credentials: EmailPasswordCredentials): Result<DomainError, AccountSession> {
        signInCredentials = credentials
        return signInResult
    }

    override suspend fun restoreSession(): Result<DomainError, AccountSession?> = restoreSessionResult

    override suspend fun updateSessionState(
        accountId: String,
        sessionState: SessionState,
        lastAuthenticatedAt: Long?,
    ): Result<DomainError, AccountSession> = DomainError.Unknown(
        message = "Not required for this test",
    ).err()

    override suspend fun signOut(accountId: String): Result<DomainError, Unit> = Unit.ok()
}
