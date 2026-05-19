package org.deafsapps.storeit.domain.usecase

import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.failureOrNull
import org.deafsapps.storeit.base.getOrNull
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.ReconciliationDecision
import org.deafsapps.storeit.domain.model.ReconciliationDecisionType
import org.deafsapps.storeit.domain.model.ReconciliationSummary
import org.deafsapps.storeit.domain.repository.ReconciliationRepository

class ReconcileDatasetsUseCasesTest {
    private lateinit var fakeRepository: FakeReconciliationRepository
    private lateinit var keepLocalUseCase: KeepLocalReconciliationUseCaseType
    private lateinit var keepRemoteUseCase: KeepRemoteReconciliationUseCaseType

    @BeforeTest
    fun setUp() {
        fakeRepository = FakeReconciliationRepository()
        keepLocalUseCase = KeepLocalReconciliationUseCase(reconciliationRepository = fakeRepository)
        keepRemoteUseCase = KeepRemoteReconciliationUseCase(reconciliationRepository = fakeRepository)
    }

    @Test
    fun `GIVEN blank account id WHEN keep-local selected THEN validation error is returned`() = runTest {
        val result = keepLocalUseCase(input = "")

        assertTrue(actual = result.isErr)
        assertTrue(actual = result.failureOrNull() is DomainError.ValidationError)
    }

    @Test
    fun `GIVEN account id WHEN keep-local selected THEN decision is saved and applied`() = runTest {
        fakeRepository.applyDecisionResult = DataMode.AccountBackedPendingSync.ok()

        val result = keepLocalUseCase(input = "account-1")

        assertEquals(expected = DataMode.AccountBackedPendingSync, actual = result.getOrNull())
        assertEquals(expected = ReconciliationDecisionType.KeepLocal, actual = fakeRepository.savedDecision?.decisionType)
        assertEquals(expected = ReconciliationDecisionType.KeepLocal, actual = fakeRepository.appliedDecisionType)
    }

    @Test
    fun `GIVEN account id WHEN keep-remote selected THEN decision is saved and applied`() = runTest {
        fakeRepository.applyDecisionResult = DataMode.AccountBackedPendingSync.ok()

        val result = keepRemoteUseCase(input = "account-1")

        assertEquals(expected = DataMode.AccountBackedPendingSync, actual = result.getOrNull())
        assertEquals(expected = ReconciliationDecisionType.KeepRemote, actual = fakeRepository.savedDecision?.decisionType)
        assertEquals(expected = ReconciliationDecisionType.KeepRemote, actual = fakeRepository.appliedDecisionType)
    }
}

private class FakeReconciliationRepository : ReconciliationRepository {
    var savedDecision: ReconciliationDecision? = null
    var appliedDecisionType: ReconciliationDecisionType? = null
    var applyDecisionResult: Result<DomainError, DataMode> = DataMode.AccountBackedPendingSync.ok()

    override suspend fun requiresReconciliation(accountId: String): Result<DomainError, Boolean> = true.ok()

    override suspend fun getReconciliationSummary(accountId: String): Result<DomainError, ReconciliationSummary> =
        ReconciliationSummary(
            accountId = accountId,
            hasLocalData = true,
            hasRemoteData = true,
            localMode = DataMode.AccountBackedPendingSync,
            localPendingChanges = true,
            remoteDatasetVersion = "remote-v1",
        ).ok()

    override suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?> = null.ok()

    override suspend fun getRemoteDataset(accountId: String): Result<DomainError, AccountDataset?> = null.ok()

    override suspend fun saveDecision(
        decision: ReconciliationDecision,
    ): Result<DomainError, ReconciliationDecision> = decision.ok().also {
        savedDecision = decision
    }

    override suspend fun applyDecision(
        accountId: String,
        decisionType: ReconciliationDecisionType,
    ): Result<DomainError, DataMode> = applyDecisionResult.also {
        appliedDecisionType = decisionType
    }
}
