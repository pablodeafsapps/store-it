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
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.repository.AccountRepository
import org.deafsapps.storeit.domain.repository.SyncRepository

class SignOutAccountUseCaseTest {
    private lateinit var fakeAccountRepository: FakeSignOutAccountRepository
    private lateinit var fakeSyncRepository: FakeSignOutSyncRepository
    private lateinit var sut: SignOutAccountUseCase

    @BeforeTest
    fun setUp() {
        fakeAccountRepository = FakeSignOutAccountRepository()
        fakeSyncRepository = FakeSignOutSyncRepository()
        sut = SignOutAccountUseCase(
            accountRepository = fakeAccountRepository,
            syncRepository = fakeSyncRepository,
        )
    }

    @Test
    fun `GIVEN pending local account-backed changes WHEN sign out THEN returns validation error and skips repository sign out`() = runTest {
        fakeSyncRepository.localDatasetState = LocalDatasetState(
            mode = DataMode.AccountBackedPendingSync,
            accountId = "account-1",
            hasPendingChanges = true,
            lastLocalChangeAt = 100L,
            lastRemoteSyncAt = 50L,
        )

        val result = sut(input = "account-1")

        assertTrue(actual = result.isErr)
        assertTrue(actual = result.failureOrNull() is DomainError.ValidationError)
        assertEquals(expected = null, actual = fakeAccountRepository.signOutAccountId)
    }

    @Test
    fun `GIVEN no pending local account-backed changes WHEN sign out THEN signs out and persists signed-out-with-local-copy mode`() = runTest {
        fakeSyncRepository.localDatasetState = LocalDatasetState(
            mode = DataMode.AccountBackedSynchronized,
            accountId = "account-1",
            hasPendingChanges = false,
            lastLocalChangeAt = 100L,
            lastRemoteSyncAt = 90L,
        )
        fakeSyncRepository.pendingOperations = emptyList()

        val result = sut(input = "account-1")

        assertTrue(actual = result.isOk)
        assertEquals(expected = SignOutAccountOutcome.SignedOut, actual = result.getOrNull())
        assertEquals(expected = "account-1", actual = fakeAccountRepository.signOutAccountId)
        assertEquals(expected = DataMode.SignedOutWithLocalCopy, actual = fakeSyncRepository.savedLocalDatasetState?.mode)
        assertEquals(expected = null, actual = fakeSyncRepository.savedLocalDatasetState?.accountId)
        assertEquals(expected = false, actual = fakeSyncRepository.savedLocalDatasetState?.hasPendingChanges)
    }

    @Test
    fun `GIVEN sign out succeeds and local state save fails WHEN sign out THEN returns partial success warning`() = runTest {
        fakeSyncRepository.localDatasetState = LocalDatasetState(
            mode = DataMode.AccountBackedSynchronized,
            accountId = "account-1",
            hasPendingChanges = false,
        )
        fakeSyncRepository.pendingOperations = emptyList()
        fakeSyncRepository.saveLocalDatasetStateResult = DomainError.Unknown(
            message = "Could not persist signed-out mode",
        ).err()

        val result = sut(input = "account-1")

        assertTrue(actual = result.isOk)
        val outcome = result.getOrNull()
        assertTrue(actual = outcome is SignOutAccountOutcome.SignedOutWithLocalStateWarning)
        assertEquals(expected = "account-1", actual = fakeAccountRepository.signOutAccountId)
    }

    @Test
    fun `GIVEN blank account id WHEN sign out THEN returns validation error`() = runTest {
        val result = sut(input = "")

        assertTrue(actual = result.isErr)
        assertTrue(actual = result.failureOrNull() is DomainError.ValidationError)
    }
}

private class FakeSignOutAccountRepository : AccountRepository {
    var signOutAccountId: String? = null

    override fun observeAccount(): Flow<Result<DomainError, Account?>> = flowOf(null.ok())

    override fun observeSession(): Flow<Result<DomainError, AccountSession?>> = flowOf(null.ok())

    override suspend fun signUp(credentials: org.deafsapps.storeit.domain.model.EmailPasswordCredentials): Result<DomainError, AccountSession> =
        DomainError.Unknown(message = "Not required for this test").err()

    override suspend fun signIn(credentials: org.deafsapps.storeit.domain.model.EmailPasswordCredentials): Result<DomainError, AccountSession> =
        DomainError.Unknown(message = "Not required for this test").err()

    override suspend fun restoreSession(): Result<DomainError, AccountSession?> = null.ok()

    override suspend fun updateSessionState(
        accountId: String,
        sessionState: SessionState,
        lastAuthenticatedAt: Long?,
    ): Result<DomainError, AccountSession> = DomainError.Unknown(message = "Not required for this test").err()

    override suspend fun signOut(accountId: String): Result<DomainError, Unit> = Unit.ok().also {
        signOutAccountId = accountId
    }
}

private class FakeSignOutSyncRepository : SyncRepository {
    var localDatasetState: LocalDatasetState? = null
    var pendingOperations: List<SyncOperation> = emptyList()
    var savedLocalDatasetState: LocalDatasetState? = null
    var saveLocalDatasetStateResult: Result<DomainError, LocalDatasetState>? = null

    override fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>> =
        flowOf(localDatasetState.ok())

    override fun observeSyncState(): Flow<Result<DomainError, SyncState?>> = flowOf(null.ok())

    override fun observePendingOperations(): Flow<Result<DomainError, List<SyncOperation>>> =
        flowOf(pendingOperations.ok())

    override suspend fun getAccountDataset(accountId: String): Result<DomainError, AccountDataset?> = null.ok()

    override suspend fun saveAccountDataset(accountDataset: AccountDataset): Result<DomainError, AccountDataset> =
        accountDataset.ok()

    override suspend fun saveLocalDatasetState(
        localDatasetState: LocalDatasetState,
    ): Result<DomainError, LocalDatasetState> =
        saveLocalDatasetStateResult ?: localDatasetState.ok().also {
            savedLocalDatasetState = localDatasetState
        }

    override suspend fun saveSyncState(syncState: SyncState): Result<DomainError, SyncState> = syncState.ok()

    override suspend fun saveSyncOperation(syncOperation: SyncOperation): Result<DomainError, SyncOperation> =
        syncOperation.ok()

    override suspend fun deleteSyncOperation(operationId: String): Result<DomainError, Unit> = Unit.ok()

    override suspend fun clearSyncOperations(): Result<DomainError, Unit> = Unit.ok()
}
