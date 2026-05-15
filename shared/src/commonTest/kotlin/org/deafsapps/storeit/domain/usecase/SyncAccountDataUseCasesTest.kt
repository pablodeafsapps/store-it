package org.deafsapps.storeit.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.model.SyncEntityType
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncOperationStatus
import org.deafsapps.storeit.domain.model.SyncOperationType
import org.deafsapps.storeit.domain.model.SyncState
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.domain.repository.AccountDataRestoreRepository
import org.deafsapps.storeit.domain.repository.SyncRepository

class SyncAccountDataUseCasesTest {
    private lateinit var resolveAccountSyncStageUseCase: ResolveAccountSyncStageUseCaseType
    private lateinit var retryPendingSyncUseCase: RetryPendingSyncUseCaseType
    private lateinit var catchUpSignedInSyncUseCase: CatchUpSignedInSyncUseCaseType
    private lateinit var fakeSyncRepository: FakeSyncRepository
    private lateinit var fakeAccountDataRestoreRepository: FakeRetryAccountDataRestoreRepository
    private lateinit var fakeUploadPendingAccountDataUseCase: FakeUploadPendingAccountDataUseCase

    @BeforeTest
    fun setUp() {
        fakeSyncRepository = FakeSyncRepository()
        fakeAccountDataRestoreRepository = FakeRetryAccountDataRestoreRepository()
        fakeUploadPendingAccountDataUseCase = FakeUploadPendingAccountDataUseCase()
        resolveAccountSyncStageUseCase = ResolveAccountSyncStageUseCase(
            syncRepository = fakeSyncRepository,
            getSyncStageUseCase = GetSyncStageUseCase(),
        )
        retryPendingSyncUseCase = RetryPendingSyncUseCase(
            syncRepository = fakeSyncRepository,
            accountDataRestoreRepository = fakeAccountDataRestoreRepository,
            uploadPendingAccountDataUseCase = fakeUploadPendingAccountDataUseCase,
            getSyncStageUseCase = GetSyncStageUseCase(),
        )
        catchUpSignedInSyncUseCase = CatchUpSignedInSyncUseCase(
            syncRepository = fakeSyncRepository,
            accountDataRestoreRepository = fakeAccountDataRestoreRepository,
            uploadPendingAccountDataUseCase = fakeUploadPendingAccountDataUseCase,
            getSyncStageUseCase = GetSyncStageUseCase(),
        )
    }

    @Test
    fun `GIVEN queued organizer mutation WHEN resolve account sync stage THEN returns pending-upload stage with queued count`() =
        runTest {
            val sut = resolveAccountSyncStageUseCase
            fakeSyncRepository.localDatasetState = LocalDatasetState(
                mode = DataMode.AccountBackedPendingSync,
                accountId = "account-1",
                hasPendingChanges = true,
            )
            fakeSyncRepository.pendingOperations = listOf(
                queuedMutationOperation(),
            )

            val result = sut(input = activeSession())

            assertTrue(actual = result.isOk)
            assertEquals(expected = SyncStageAction.UploadPendingChanges, actual = result.getOrNull()?.nextAction)
            assertEquals(expected = SyncStatus.PendingUpload, actual = result.getOrNull()?.syncState?.status)
            assertEquals(expected = 1, actual = result.getOrNull()?.syncState?.pendingOperationCount)
        }

    @Test
    fun `GIVEN restore-pending operation WHEN retry pending sync THEN retries account restore instead of upload`() =
        runTest {
            val sut = retryPendingSyncUseCase
            fakeSyncRepository.localDatasetState = LocalDatasetState(
                mode = DataMode.AccountBackedPendingSync,
                accountId = "account-1",
                hasPendingChanges = false,
            )
            fakeSyncRepository.pendingOperations = listOf(
                restoreOperation(),
            )

            val result = sut(input = activeSession())

            assertTrue(actual = result.isOk)
            assertEquals(expected = 1, actual = fakeAccountDataRestoreRepository.restoreCalls.size)
            assertEquals(expected = 0, actual = fakeUploadPendingAccountDataUseCase.invocations.size)
            assertEquals(expected = SyncStageAction.RestoreRemoteDataset, actual = result.getOrNull()?.nextAction)
        }

    @Test
    fun `GIVEN failed sync with queued organizer mutation WHEN retry pending sync THEN retries pending upload`() =
        runTest {
            val sut = retryPendingSyncUseCase
            fakeSyncRepository.localDatasetState = LocalDatasetState(
                mode = DataMode.AccountBackedPendingSync,
                accountId = "account-1",
                hasPendingChanges = true,
            )
            fakeSyncRepository.persistedSyncState = SyncState(
                status = SyncStatus.Failed,
                failureReason = "Upload failed earlier",
                pendingOperationCount = 1,
            )
            fakeSyncRepository.pendingOperations = listOf(
                queuedMutationOperation(),
            )

            val result = sut(input = activeSession())

            assertTrue(actual = result.isOk)
            assertEquals(expected = 0, actual = fakeAccountDataRestoreRepository.restoreCalls.size)
            assertEquals(expected = listOf("account-1"), actual = fakeUploadPendingAccountDataUseCase.invocations)
            assertEquals(expected = SyncStageAction.RetryFailedSync, actual = result.getOrNull()?.nextAction)
        }

    @Test
    fun `GIVEN pending upload stage WHEN catch-up signed-in sync THEN triggers pending upload`() = runTest {
        val sut = catchUpSignedInSyncUseCase
        fakeSyncRepository.localDatasetState = LocalDatasetState(
            mode = DataMode.AccountBackedPendingSync,
            accountId = "account-1",
            hasPendingChanges = true,
        )
        fakeSyncRepository.pendingOperations = listOf(queuedMutationOperation())

        val result = sut(input = activeSession())

        assertTrue(actual = result.isOk)
        assertEquals(expected = listOf("account-1"), actual = fakeUploadPendingAccountDataUseCase.invocations)
    }

    @Test
    fun `GIVEN failed stage WHEN catch-up signed-in sync THEN does not trigger retry action`() = runTest {
        val sut = catchUpSignedInSyncUseCase
        fakeSyncRepository.localDatasetState = LocalDatasetState(
            mode = DataMode.AccountBackedPendingSync,
            accountId = "account-1",
            hasPendingChanges = true,
        )
        fakeSyncRepository.persistedSyncState = SyncState(
            status = SyncStatus.Failed,
            failureReason = "Upload failed earlier",
            pendingOperationCount = 1,
        )
        fakeSyncRepository.pendingOperations = listOf(queuedMutationOperation())

        val result = sut(input = activeSession())

        assertTrue(actual = result.isOk)
        assertEquals(expected = 0, actual = fakeAccountDataRestoreRepository.restoreCalls.size)
        assertEquals(expected = 0, actual = fakeUploadPendingAccountDataUseCase.invocations.size)
        assertEquals(expected = SyncStageAction.RetryFailedSync, actual = result.getOrNull()?.nextAction)
    }
}

private fun activeSession(): AccountSession = AccountSession(
    accountId = "account-1",
    email = "user@example.com",
    sessionState = SessionState.Active,
    lastAuthenticatedAt = 10L,
)

private fun queuedMutationOperation(): SyncOperation = SyncOperation(
    id = "operation-1",
    accountId = "account-1",
    entityType = SyncEntityType.Item,
    entityId = "item-1",
    operationType = SyncOperationType.Update,
    syncStatus = SyncOperationStatus.Pending,
)

private fun restoreOperation(): SyncOperation = SyncOperation(
    id = "restore-1",
    accountId = "account-1",
    entityType = SyncEntityType.Dataset,
    entityId = "dataset-1",
    operationType = SyncOperationType.Restore,
    syncStatus = SyncOperationStatus.Pending,
)

private class FakeSyncRepository : SyncRepository {
    var localDatasetState: LocalDatasetState? = null
    var persistedSyncState: SyncState? = null
    var pendingOperations: List<SyncOperation> = emptyList()
    var remoteDataset: AccountDataset? = null

    override fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>> =
        flowOf(value = localDatasetState.ok())

    override fun observeSyncState(): Flow<Result<DomainError, SyncState?>> =
        flowOf(value = persistedSyncState.ok())

    override fun observePendingOperations(): Flow<Result<DomainError, List<SyncOperation>>> =
        flowOf(value = pendingOperations.ok())

    override suspend fun getAccountDataset(accountId: String): Result<DomainError, AccountDataset?> =
        remoteDataset.ok()

    override suspend fun saveAccountDataset(accountDataset: AccountDataset): Result<DomainError, AccountDataset> =
        accountDataset.ok()

    override suspend fun saveLocalDatasetState(
        localDatasetState: LocalDatasetState,
    ): Result<DomainError, LocalDatasetState> = localDatasetState.ok()

    override suspend fun saveSyncState(syncState: SyncState): Result<DomainError, SyncState> =
        syncState.ok()

    override suspend fun saveSyncOperation(syncOperation: SyncOperation): Result<DomainError, SyncOperation> =
        syncOperation.ok()

    override suspend fun deleteSyncOperation(operationId: String): Result<DomainError, Unit> = Unit.ok()

    override suspend fun clearSyncOperations(): Result<DomainError, Unit> = Unit.ok()
}

private class FakeRetryAccountDataRestoreRepository : AccountDataRestoreRepository {
    var restoreCalls: MutableList<AccountSession> = mutableListOf()
    var restoreResult: Result<DomainError, Unit> = Unit.ok()

    override suspend fun restoreAccountData(session: AccountSession): Result<DomainError, Unit> {
        restoreCalls += session
        return restoreResult
    }
}

private class FakeUploadPendingAccountDataUseCase : UploadPendingAccountDataUseCaseType {
    var invocations: MutableList<String> = mutableListOf()
    var result: Result<DomainError, Unit> = Unit.ok()

    override suspend fun invoke(input: String): Result<DomainError, Unit> {
        invocations += input
        return result
    }
}
