package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.flatMap
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.repository.AccountDataRestoreRepository
import org.koin.core.annotation.Factory

internal interface RestoreAccountDataUseCaseType : UseCase<AccountSession, Result<DomainError, Unit>>

@Factory(binds = [RestoreAccountDataUseCaseType::class])
internal class RestoreAccountDataUseCase(
    private val accountDataRestoreRepository: AccountDataRestoreRepository,
) : RestoreAccountDataUseCaseType {

    override suspend fun invoke(input: AccountSession): Result<DomainError, Unit> =
        validateSession(session = input)
            .flatMap { session ->
                accountDataRestoreRepository.restoreAccountData(session = session)
            }

    private fun validateSession(session: AccountSession): Result<DomainError, AccountSession> = when {
        session.accountId.isBlank() -> DomainError.ValidationError(
            field = "accountId",
            reason = "Active session must include an account identifier",
        ).err()

        session.sessionState != SessionState.Active -> DomainError.ValidationError(
            field = "sessionState",
            reason = "Only active account sessions can restore remote account data",
        ).err()

        else -> session.ok()
    }
}
