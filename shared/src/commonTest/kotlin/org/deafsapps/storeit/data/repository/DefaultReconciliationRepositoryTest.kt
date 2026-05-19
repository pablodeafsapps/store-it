package org.deafsapps.storeit.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.datasource.AccountDatasetDataSource
import org.deafsapps.storeit.data.datasource.LocalDatasetStateDataSource
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.ReconciliationDecision
import org.deafsapps.storeit.domain.model.ReconciliationDecisionType
import org.deafsapps.storeit.domain.repository.ReconciliationRepository

class DefaultReconciliationRepositoryTest {
    private lateinit var fakeReconciliationAccountDatasetDataSource: FakeReconciliationAccountDatasetDataSource
    private lateinit var fakeReconciliationLocalDatasetStateDataSource: FakeReconciliationLocalDatasetStateDataSource
    private lateinit var sut: ReconciliationRepository

    @BeforeTest
    fun setUp() {
        fakeReconciliationAccountDatasetDataSource = FakeReconciliationAccountDatasetDataSource()
        fakeReconciliationLocalDatasetStateDataSource = FakeReconciliationLocalDatasetStateDataSource()
        sut = DefaultReconciliationRepository(
            accountDatasetDataSource = fakeReconciliationAccountDatasetDataSource,
            localDatasetStateDataSource = fakeReconciliationLocalDatasetStateDataSource,
        )
    }

    @Test
    fun `GIVEN local pending changes and remote dataset WHEN requires reconciliation THEN returns true`() = runTest {
        fakeReconciliationLocalDatasetStateDataSource.localState = LocalDatasetState(
            mode = DataMode.AccountBackedPendingSync,
            accountId = "account-1",
            hasPendingChanges = true,
            lastLocalChangeAt = 100L,
        )
        fakeReconciliationAccountDatasetDataSource.dataset = AccountDataset(
            accountId = "account-1",
            datasetVersion = "remote-v1",
            lastSyncedAt = 90L,
        )

        val result = sut.requiresReconciliation(accountId = "account-1")

        assertEquals(expected = true, actual = result.getOrNull())
    }

    @Test
    fun `GIVEN local-only with no remote WHEN requires reconciliation THEN returns false`() = runTest {
        fakeReconciliationLocalDatasetStateDataSource.localState = LocalDatasetState(
            mode = DataMode.LocalOnly,
            hasPendingChanges = false,
            lastLocalChangeAt = null,
        )
        fakeReconciliationAccountDatasetDataSource.dataset = null

        val result = sut.requiresReconciliation(accountId = "account-1")

        assertEquals(expected = false, actual = result.getOrNull())
    }

    @Test
    fun `GIVEN local and remote state WHEN get reconciliation summary THEN includes keep-local vs keep-remote context`() = runTest {
        fakeReconciliationLocalDatasetStateDataSource.localState = LocalDatasetState(
            mode = DataMode.AccountBackedPendingSync,
            accountId = "account-1",
            hasPendingChanges = true,
            lastLocalChangeAt = 100L,
        )
        fakeReconciliationAccountDatasetDataSource.dataset = AccountDataset(
            accountId = "account-1",
            datasetVersion = "remote-v2",
            lastSyncedAt = 80L,
        )

        val result = sut.getReconciliationSummary(accountId = "account-1")

        val summary = result.getOrNull()
        assertEquals(expected = "account-1", actual = summary?.accountId)
        assertEquals(expected = true, actual = summary?.hasLocalData)
        assertEquals(expected = true, actual = summary?.hasRemoteData)
        assertEquals(expected = DataMode.AccountBackedPendingSync, actual = summary?.localMode)
        assertEquals(expected = true, actual = summary?.localPendingChanges)
        assertEquals(expected = "remote-v2", actual = summary?.remoteDatasetVersion)
    }

    @Test
    fun `GIVEN confirmed decision WHEN save decision THEN decision is returned`() = runTest {
        val decision = ReconciliationDecision(
            accountId = "account-1",
            decisionType = ReconciliationDecisionType.KeepLocal,
        )

        val result = sut.saveDecision(decision = decision)

        assertEquals(expected = decision, actual = result.getOrNull())
    }

    @Test
    fun `GIVEN keep-remote decision WHEN apply decision THEN moves account to pending sync mode`() = runTest {
        val result = sut.applyDecision(
            accountId = "account-1",
            decisionType = ReconciliationDecisionType.KeepRemote,
        )

        assertTrue(actual = result.isOk)
        assertEquals(expected = DataMode.AccountBackedPendingSync, actual = result.getOrNull())
    }
}

private class FakeReconciliationAccountDatasetDataSource : AccountDatasetDataSource {
    var dataset: AccountDataset? = null

    override suspend fun getAccountDataset(accountId: String): Result<DomainError, AccountDataset?> = dataset.ok()

    override suspend fun saveAccountDataset(
        accountDataset: AccountDataset,
    ): Result<DomainError, AccountDataset> = accountDataset.ok().also {
        dataset = accountDataset
    }

    override suspend fun deleteAccountDataset(accountId: String): Result<DomainError, Long> = 0L.ok()
}

private class FakeReconciliationLocalDatasetStateDataSource : LocalDatasetStateDataSource {
    var localState: LocalDatasetState? = null

    override fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>> =
        flowOf(localState.ok())

    override suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?> = localState.ok()

    override suspend fun saveLocalDatasetState(
        localDatasetState: LocalDatasetState,
    ): Result<DomainError, LocalDatasetState> = localDatasetState.ok().also {
        localState = localDatasetState
    }

    override suspend fun deleteLocalDatasetState(): Result<DomainError, Long> = 0L.ok()
}
