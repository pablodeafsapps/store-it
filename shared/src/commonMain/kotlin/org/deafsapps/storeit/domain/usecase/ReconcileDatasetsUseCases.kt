package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.flatMap
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.ReconciliationDecision
import org.deafsapps.storeit.domain.model.ReconciliationDecisionType
import org.deafsapps.storeit.domain.repository.ReconciliationRepository
import org.koin.core.annotation.Factory

internal interface KeepLocalReconciliationUseCaseType : UseCase<String, Result<DomainError, DataMode>>

internal interface KeepRemoteReconciliationUseCaseType : UseCase<String, Result<DomainError, DataMode>>

@Factory(binds = [KeepLocalReconciliationUseCaseType::class])
internal class KeepLocalReconciliationUseCase(
    private val reconciliationRepository: ReconciliationRepository,
) : KeepLocalReconciliationUseCaseType {
    override suspend fun invoke(input: String): Result<DomainError, DataMode> =
        applyReconciliationDecision(
            accountId = input,
            decisionType = ReconciliationDecisionType.KeepLocal,
            reconciliationRepository = reconciliationRepository,
        )
}

@Factory(binds = [KeepRemoteReconciliationUseCaseType::class])
internal class KeepRemoteReconciliationUseCase(
    private val reconciliationRepository: ReconciliationRepository,
) : KeepRemoteReconciliationUseCaseType {
    override suspend fun invoke(input: String): Result<DomainError, DataMode> =
        applyReconciliationDecision(
            accountId = input,
            decisionType = ReconciliationDecisionType.KeepRemote,
            reconciliationRepository = reconciliationRepository,
        )
}

private suspend fun applyReconciliationDecision(
    accountId: String,
    decisionType: ReconciliationDecisionType,
    reconciliationRepository: ReconciliationRepository,
): Result<DomainError, DataMode> {
    if (accountId.isBlank()) {
        return DomainError.ValidationError(
            field = "accountId",
            reason = "Account ID cannot be blank",
        ).err()
    }

    return reconciliationRepository
        .saveDecision(
            decision = ReconciliationDecision(
                accountId = accountId,
                decisionType = decisionType,
            ),
        )
        .flatMap {
            reconciliationRepository.applyDecision(
                accountId = accountId,
                decisionType = decisionType,
            )
        }
}
