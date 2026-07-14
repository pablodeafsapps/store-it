package org.deafsapps.storeit.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
import org.deafsapps.storeit.domain.model.EmailPasswordCredentials
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.repository.AccountRepository
import org.deafsapps.storeit.domain.repository.SyncRepository

internal class SignOutAccountUseCaseTest {

    private lateinit var sut: SignOutAccountUseCase
    private lateinit var fakeAccountRepository: FakeSignOutAccountRepository
    private lateinit var fakeSyncRepository: FakeSignOutSyncRepository

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
    fun `GIVEN pending local account-backed changes WHEN sign out THEN returns validation error and skips repository sign out`() =
        runTest {
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
    fun `GIVEN no pending local account-backed changes WHEN sign out THEN signs out and persists signed-out-with-local-copy mode`() =
        runTest {
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
            assertEquals(
                expected = DataMode.SignedOutWithLocalCopy,
                actual = fakeSyncRepository.savedLocalDatasetState?.mode
            )
            assertEquals(
                expected = null,
                actual = fakeSyncRepository.savedLocalDatasetState?.accountId
            )
            assertEquals(
                expected = 100L,
                actual = fakeSyncRepository.savedLocalDatasetState?.lastLocalChangeAt
            )
            assertEquals(
                expected = 90L,
                actual = fakeSyncRepository.savedLocalDatasetState?.lastRemoteSyncAt
            )
            assertEquals(
                expected = false,
                actual = fakeSyncRepository.savedLocalDatasetState?.hasPendingChanges
            )
        }

    @Test
    fun `GIVEN local-only mode with no pending work WHEN sign out THEN signs out and keeps local state as signed-out local copy`() =
        runTest {
            fakeSyncRepository.localDatasetState = LocalDatasetState(
                mode = DataMode.LocalOnly,
                accountId = null,
                hasPendingChanges = false,
                lastLocalChangeAt = 44L,
                lastRemoteSyncAt = null,
            )
            fakeSyncRepository.pendingOperations = emptyList()

            val result = sut(input = "account-1")

            assertTrue(actual = result.isOk)
            assertEquals(expected = SignOutAccountOutcome.SignedOut, actual = result.getOrNull())
            assertEquals(expected = "account-1", actual = fakeAccountRepository.signOutAccountId)
            assertEquals(
                expected = DataMode.SignedOutWithLocalCopy,
                actual = fakeSyncRepository.savedLocalDatasetState?.mode
            )
            assertEquals(
                expected = 44L,
                actual = fakeSyncRepository.savedLocalDatasetState?.lastLocalChangeAt
            )
            assertEquals(
                expected = null,
                actual = fakeSyncRepository.savedLocalDatasetState?.lastRemoteSyncAt
            )
        }

    @Test
    fun `GIVEN sign out succeeds and local state save fails WHEN sign out THEN returns partial success warning`() =
        runTest {
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
    fun `GIVEN queued pending operations and no local pending flag WHEN sign out THEN returns validation error and skips repository sign out`() =
        runTest {
            fakeSyncRepository.localDatasetState = LocalDatasetState(
                mode = DataMode.AccountBackedSynchronized,
                accountId = "account-1",
                hasPendingChanges = false,
            )
            fakeSyncRepository.pendingOperations = listOf(
                SyncOperation(
                    id = "operation-1",
                    accountId = "account-1",
                    entityType = org.deafsapps.storeit.domain.model.SyncEntityType.Item,
                    entityId = "item-1",
                    operationType = org.deafsapps.storeit.domain.model.SyncOperationType.Update,
                    payloadJson = """{"id":"item-1"}""",
                    syncStatus = org.deafsapps.storeit.domain.model.SyncOperationStatus.Pending,
                    recordedAt = 10L,
                    lastAttemptAt = null,
                    failureReason = null,
                ),
            )

            val result = sut(input = "account-1")

            assertTrue(actual = result.isErr)
            assertTrue(actual = result.failureOrNull() is DomainError.ValidationError)
            assertEquals(expected = null, actual = fakeAccountRepository.signOutAccountId)
        }

    @Test
    fun `GIVEN account repository sign out fails WHEN sign out THEN error is returned and signed-out local state is not saved`() =
        runTest {
            val expectedError = DomainError.Unknown(message = "sign out failed")
            fakeSyncRepository.localDatasetState = LocalDatasetState(
                mode = DataMode.AccountBackedSynchronized,
                accountId = "account-1",
                hasPendingChanges = false,
            )
            fakeSyncRepository.pendingOperations = emptyList()
            fakeAccountRepository.signOutResult = expectedError.err()

            val result = sut(input = "account-1")

            assertEquals(expected = expectedError, actual = result.failureOrNull())
            assertEquals(expected = null, actual = fakeSyncRepository.savedLocalDatasetState)
        }

    @Test
    fun `GIVEN local dataset state flow emits no value WHEN sign out THEN unknown error is returned`() =
        runTest {
            fakeSyncRepository.emitLocalDatasetState = false

            val result = sut(input = "account-1")

            assertTrue(actual = result.isErr)
            assertEquals(
                expected = DomainError.Unknown(message = "Local dataset state flow emitted no values."),
                actual = result.failureOrNull(),
            )
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
    var signOutResult: Result<DomainError, Unit> = Unit.ok()

    override fun observeAccount(): Flow<Result<DomainError, Account?>> = flowOf(null.ok())

    override fun observeSession(): Flow<Result<DomainError, AccountSession?>> = flowOf(null.ok())

    override suspend fun signUp(credentials: EmailPasswordCredentials): Result<DomainError, AccountSession> =
        DomainError.Unknown(message = "Not required for this test").err()

    override suspend fun signIn(credentials: EmailPasswordCredentials): Result<DomainError, AccountSession> =
        DomainError.Unknown(message = "Not required for this test").err()

    override suspend fun restoreSession(): Result<DomainError, AccountSession?> = null.ok()

    override suspend fun updateSessionState(
        accountId: String,
        sessionState: SessionState,
        lastAuthenticatedAt: Long?,
    ): Result<DomainError, AccountSession> =
        DomainError.Unknown(message = "Not required for this test").err()

    override suspend fun signOut(accountId: String): Result<DomainError, Unit> =
        signOutResult.also {
            signOutAccountId = accountId
        }
}

private class FakeSignOutSyncRepository : SyncRepository {
    var localDatasetState: LocalDatasetState? = null
    var pendingOperations: List<SyncOperation> = emptyList()
    var savedLocalDatasetState: LocalDatasetState? = null
    var saveLocalDatasetStateResult: Result<DomainError, LocalDatasetState>? = null
    var emitLocalDatasetState: Boolean = true

    override fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>> =
        if (emitLocalDatasetState) {
            flowOf(localDatasetState.ok())
        } else {
            emptyFlow()
        }

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
