package org.deafsapps.storeit.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.flatMap
import org.deafsapps.storeit.base.fold
import org.deafsapps.storeit.base.foldFailure
import org.deafsapps.storeit.base.getOrElse
import org.deafsapps.storeit.base.map
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.repository.AccountRepository
import org.deafsapps.storeit.domain.repository.SyncRepository
import org.koin.core.annotation.Factory

internal interface SignOutAccountUseCaseType :
    UseCase<String, Result<DomainError, SignOutAccountOutcome>>

internal sealed interface SignOutAccountOutcome {
    data object SignedOut : SignOutAccountOutcome
    data class SignedOutWithLocalStateWarning(val error: DomainError) : SignOutAccountOutcome
}

@Factory(binds = [SignOutAccountUseCaseType::class])
internal class SignOutAccountUseCase(
    private val accountRepository: AccountRepository,
    private val syncRepository: SyncRepository,
) : SignOutAccountUseCaseType {
    override suspend fun invoke(input: String): Result<DomainError, SignOutAccountOutcome> {
        if (input.isBlank()) {
            return DomainError.ValidationError(
                field = "accountId",
                reason = "Account ID cannot be blank",
            ).err()
        }

        return loadSignOutSnapshot().flatMap { snapshot ->
            if (snapshot.hasPendingChanges) {
                return DomainError.ValidationError(
                    reason = "You still have pending local changes. Sync before signing out to avoid delaying backup.",
                ).err()
            }

            accountRepository
                .signOut(accountId = input)
                .flatMap {
                    syncRepository
                        .saveLocalDatasetState(localDatasetState = snapshot.toSignedOutWithLocalCopyState())
                        .map<DomainError, LocalDatasetState, SignOutAccountOutcome> {
                            SignOutAccountOutcome.SignedOut
                        }
                        .foldFailure { error ->
                            SignOutAccountOutcome.SignedOutWithLocalStateWarning(error = error).ok()
                        }
                }
        }
    }

    private suspend fun loadSignOutSnapshot(): Result<DomainError, SignOutSnapshot> {
        val localDatasetStateResult = firstResultOrUnknown(
            result = syncRepository.observeLocalDatasetState().firstOrNull(),
            missingEmissionMessage = "Local dataset state flow emitted no values.",
        )
        val pendingOperationsResult = firstResultOrUnknown(
            result = syncRepository.observePendingOperations().firstOrNull(),
            missingEmissionMessage = "Pending operations flow emitted no values.",
        )

        val localDatasetState = localDatasetStateResult.getOrElse { error -> return error }
        val pendingOperations = pendingOperationsResult.getOrElse { error -> return error }
        val pendingChanges = localDatasetState?.hasPendingChanges == true || pendingOperations.isNotEmpty()

        return SignOutSnapshot(
            localDatasetState = localDatasetState,
            hasPendingChanges = pendingChanges,
        ).ok()
    }
}

private data class SignOutSnapshot(
    val localDatasetState: LocalDatasetState?,
    val hasPendingChanges: Boolean,
) {
    fun toSignedOutWithLocalCopyState(): LocalDatasetState = LocalDatasetState(
        mode = DataMode.SignedOutWithLocalCopy,
        accountId = null,
        lastLocalChangeAt = localDatasetState?.lastLocalChangeAt,
        lastRemoteSyncAt = localDatasetState?.lastRemoteSyncAt,
        hasPendingChanges = false,
    )
}

private fun <T> firstResultOrUnknown(
    result: Result<DomainError, T>?,
    missingEmissionMessage: String,
): Result<DomainError, T> = result ?: DomainError.Unknown(message = missingEmissionMessage).err()
