package org.deafsapps.storeit.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.datasource.AccountDatasetDataSource
import org.deafsapps.storeit.data.datasource.AccountRemoteDataSource
import org.deafsapps.storeit.data.datasource.AccountSessionDataSource
import org.deafsapps.storeit.data.datasource.AuthRemoteDataSource
import org.deafsapps.storeit.data.datasource.AuthenticatedRemoteAccount
import org.deafsapps.storeit.data.datasource.EmailPasswordCredentials
import org.deafsapps.storeit.data.datasource.RemoteDatasetMutation
import org.deafsapps.storeit.data.datasource.RemotePhotoAsset
import org.deafsapps.storeit.data.datasource.RemotePhotoReference
import org.deafsapps.storeit.data.datasource.RemoteSyncCheckpoint
import org.deafsapps.storeit.data.datasource.SessionCredentialDataSource
import org.deafsapps.storeit.data.datasource.StoredSessionCredentials
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.repository.ItemRepository
import org.deafsapps.storeit.domain.repository.AccountRepository
import org.deafsapps.storeit.domain.repository.RackRepository
import org.deafsapps.storeit.domain.repository.SlotRepository

class FirebaseAccountRepositoryRestoreSessionTest {
    private lateinit var sut: AccountRepository
    private lateinit var fakeAuthRemoteDataSource: FakeAuthRemoteDataSource
    private lateinit var fakeSessionCredentialDataSource: FakeSessionCredentialDataSource
    private lateinit var fakeAccountSessionDataSource: FakeAccountSessionDataSource

    @BeforeTest
    fun setUp() {
        fakeAuthRemoteDataSource = FakeAuthRemoteDataSource()
        fakeSessionCredentialDataSource = FakeSessionCredentialDataSource()
        fakeAccountSessionDataSource = FakeAccountSessionDataSource()
        sut = FirebaseAccountRepository(
            authRemoteDataSource = fakeAuthRemoteDataSource,
            sessionCredentialDataSource = fakeSessionCredentialDataSource,
            accountSessionDataSource = fakeAccountSessionDataSource,
        )
    }

    @Test
    fun `GIVEN stored secure session WHEN restoreSession succeeds THEN returns active session and refreshes persisted state`() =
        runTest {
            val storedCredentials = storedSessionCredentials()
            val remoteAccount = authenticatedRemoteAccount()
            fakeSessionCredentialDataSource.restoreResult = storedCredentials.ok()
            fakeAuthRemoteDataSource.restoreSessionResult = remoteAccount.ok()

            val result: Result<DomainError, AccountSession?> = sut.restoreSession()

            assertTrue(actual = result.isOk)
            assertEquals(expected = storedCredentials, actual = fakeAuthRemoteDataSource.restoreSessionInput)
            assertEquals(expected = "account-1", actual = result.getOrNull()?.accountId)
            assertEquals(expected = SessionState.Active, actual = result.getOrNull()?.sessionState)
            assertEquals(expected = "account-1", actual = fakeAccountSessionDataSource.savedAccountSession?.accountId)
            assertEquals(expected = "account-1", actual = fakeSessionCredentialDataSource.savedSession?.accountId)
        }

    @Test
    fun `GIVEN no stored secure session WHEN restoreSession invoked THEN returns null without calling remote restore`() =
        runTest {
            fakeSessionCredentialDataSource.restoreResult = null.ok()

            val result: Result<DomainError, AccountSession?> = sut.restoreSession()

            assertTrue(actual = result.isOk)
            assertEquals(expected = null, actual = result.getOrNull())
            assertEquals(expected = null, actual = fakeAuthRemoteDataSource.restoreSessionInput)
            assertEquals(expected = null, actual = fakeAccountSessionDataSource.savedAccountSession)
        }
}

private fun authenticatedRemoteAccount(): AuthenticatedRemoteAccount = AuthenticatedRemoteAccount(
    accountId = "account-1",
    email = "user@example.com",
    session = storedSessionCredentials(),
    createdAt = 10L,
    lastAuthenticatedAt = 20L,
)

private fun storedSessionCredentials(): StoredSessionCredentials = StoredSessionCredentials(
    accountId = "account-1",
    email = "user@example.com",
    accessToken = "access-token",
    refreshToken = "refresh-token",
    lastAuthenticatedAt = 20L,
)

private class FakeAuthRemoteDataSource : AuthRemoteDataSource {
    var restoreSessionResult: Result<DomainError, AuthenticatedRemoteAccount> = DomainError.Unknown(
        message = "restoreSessionResult not configured",
    ).err()

    var restoreSessionInput: StoredSessionCredentials? = null

    override suspend fun signUp(credentials: EmailPasswordCredentials): Result<DomainError, AuthenticatedRemoteAccount> =
        DomainError.Unknown(message = "Not required for this test").err()

    override suspend fun signIn(credentials: EmailPasswordCredentials): Result<DomainError, AuthenticatedRemoteAccount> =
        DomainError.Unknown(message = "Not required for this test").err()

    override suspend fun restoreSession(session: StoredSessionCredentials): Result<DomainError, AuthenticatedRemoteAccount> {
        restoreSessionInput = session
        return restoreSessionResult
    }

    override suspend fun signOut(accountId: String): Result<DomainError, Unit> = Unit.ok()
}

private class FakeSessionCredentialDataSource : SessionCredentialDataSource {
    var restoreResult: Result<DomainError, StoredSessionCredentials?> = DomainError.Unknown(
        message = "restoreResult not configured",
    ).err()

    var savedSession: StoredSessionCredentials? = null

    override suspend fun save(session: StoredSessionCredentials): Result<DomainError, Unit> {
        savedSession = session
        return Unit.ok()
    }

    override suspend fun restore(): Result<DomainError, StoredSessionCredentials?> = restoreResult

    override suspend fun clear(): Result<DomainError, Unit> = Unit.ok()
}

private class FakeAccountSessionDataSource : AccountSessionDataSource {
    var savedAccountSession: AccountSession? = null

    override fun observeActiveAccountSession(): Flow<Result<DomainError, AccountSession?>> = flowOf(null.ok())

    override suspend fun getAccountSession(accountId: String): Result<DomainError, AccountSession?> = null.ok()

    override suspend fun saveAccountSession(accountSession: AccountSession): Result<DomainError, AccountSession> {
        savedAccountSession = accountSession
        return accountSession.ok()
    }

    override suspend fun clearActiveAccountSessions(): Result<DomainError, Long> = 0L.ok()

    override suspend fun deleteAccountSession(accountId: String): Result<DomainError, Long> = 0L.ok()
}
