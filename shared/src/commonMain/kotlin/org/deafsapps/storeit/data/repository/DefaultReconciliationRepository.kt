package org.deafsapps.storeit.data.repository

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.flatMap
import org.deafsapps.storeit.base.map
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.data.datasource.AccountDatasetDataSource
import org.deafsapps.storeit.data.datasource.LocalDatasetStateDataSource
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.ReconciliationDecision
import org.deafsapps.storeit.domain.model.ReconciliationDecisionType
import org.deafsapps.storeit.domain.model.ReconciliationSummary
import org.deafsapps.storeit.domain.repository.ReconciliationRepository
import org.koin.core.annotation.Single

@Single(binds = [ReconciliationRepository::class])
internal class DefaultReconciliationRepository(
    private val accountDatasetDataSource: AccountDatasetDataSource,
    private val localDatasetStateDataSource: LocalDatasetStateDataSource,
) : ReconciliationRepository {
    override suspend fun requiresReconciliation(accountId: String): Result<DomainError, Boolean> =
        getReconciliationSummary(accountId = accountId).map { summary ->
            summary.hasLocalData && summary.hasRemoteData
        }

    override suspend fun getReconciliationSummary(
        accountId: String,
    ): Result<DomainError, ReconciliationSummary> = localDatasetStateDataSource
        .getLocalDatasetState()
        .flatMap { localDatasetState ->
            accountDatasetDataSource
                .getAccountDataset(accountId = accountId)
                .map { accountDataset ->
                    buildSummary(
                        accountId = accountId,
                        localDatasetState = localDatasetState,
                        accountDataset = accountDataset,
                    )
                }
        }

    override suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?> =
        localDatasetStateDataSource.getLocalDatasetState()

    override suspend fun getRemoteDataset(accountId: String): Result<DomainError, AccountDataset?> =
        accountDatasetDataSource.getAccountDataset(accountId = accountId)

    override suspend fun saveDecision(
        decision: ReconciliationDecision,
    ): Result<DomainError, ReconciliationDecision> = decision.ok()

    override suspend fun applyDecision(
        accountId: String,
        decisionType: ReconciliationDecisionType,
    ): Result<DomainError, DataMode> = when (decisionType) {
        ReconciliationDecisionType.KeepLocal -> DataMode.AccountBackedPendingSync.ok()
        ReconciliationDecisionType.KeepRemote -> DataMode.AccountBackedPendingSync.ok()
    }
}

private fun buildSummary(
    accountId: String,
    localDatasetState: LocalDatasetState?,
    accountDataset: AccountDataset?,
): ReconciliationSummary = ReconciliationSummary(
    accountId = accountId,
    hasLocalData = hasLocalData(localDatasetState = localDatasetState),
    hasRemoteData = accountDataset != null,
    localMode = localDatasetState?.mode ?: DataMode.LocalOnly,
    localPendingChanges = localDatasetState?.hasPendingChanges ?: false,
    remoteDatasetVersion = accountDataset?.datasetVersion,
)

private fun hasLocalData(localDatasetState: LocalDatasetState?): Boolean {
    if (localDatasetState == null) {
        return false
    }

    return localDatasetState.hasPendingChanges ||
        localDatasetState.lastLocalChangeAt != null ||
        localDatasetState.mode != DataMode.LocalOnly
}
