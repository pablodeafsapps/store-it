package org.deafsapps.storeit.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.flatMap
import org.deafsapps.storeit.base.map
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.base.suspendFlatMap
import org.deafsapps.storeit.data.datasource.AccountSessionDataSource
import org.deafsapps.storeit.data.datasource.AuthRemoteDataSource
import org.deafsapps.storeit.data.datasource.AuthenticatedRemoteAccount
import org.deafsapps.storeit.data.datasource.EmailPasswordCredentials as DataSourceEmailPasswordCredentials
import org.deafsapps.storeit.data.datasource.SessionCredentialDataSource
import org.deafsapps.storeit.domain.model.Account
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.AccountStatus
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.EmailPasswordCredentials
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.model.asModel
import org.deafsapps.storeit.domain.repository.AccountRepository
import org.koin.core.annotation.Single

@Single(binds = [AccountRepository::class])
internal class FirebaseAccountRepository(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val sessionCredentialDataSource: SessionCredentialDataSource,
    private val accountSessionDataSource: AccountSessionDataSource,
) : AccountRepository {

    override fun observeAccount(): Flow<Result<DomainError, Account?>> =
        observeSession().map { sessionResult ->
            sessionResult.flatMap { accountSession -> accountSession?.toAccount().ok() }
        }

    override fun observeSession(): Flow<Result<DomainError, AccountSession?>> =
        accountSessionDataSource.observeActiveAccountSession()

    override suspend fun signUp(credentials: EmailPasswordCredentials): Result<DomainError, AccountSession> =
        authRemoteDataSource.signUp(credentials = credentials.toDataSourceModel())
            .suspendFlatMap(::persistAuthenticatedAccount)

    override suspend fun signIn(credentials: EmailPasswordCredentials): Result<DomainError, AccountSession> =
        authRemoteDataSource.signIn(credentials = credentials.toDataSourceModel())
            .suspendFlatMap(::persistAuthenticatedAccount)

    override suspend fun restoreSession(): Result<DomainError, AccountSession?> =
        sessionCredentialDataSource.restore()
            .suspendFlatMap { storedCredentials ->
                storedCredentials?.let { credentials ->
                    authRemoteDataSource.restoreSession(session = credentials)
                        .suspendFlatMap(::persistAuthenticatedAccount)
                        .map { accountSession -> accountSession as AccountSession? }
                } ?: null.ok()
            }

    override suspend fun updateSessionState(
        accountId: String,
        sessionState: SessionState,
        lastAuthenticatedAt: Long?,
    ): Result<DomainError, AccountSession> {
        if (accountId.isBlank()) {
            return DomainError.ValidationError(
                field = "accountId",
                reason = "Account ID cannot be blank",
            ).err()
        }

        return accountSessionDataSource.getAccountSession(accountId = accountId)
            .suspendFlatMap { existingSession ->
                val resolvedSession = existingSession ?: return@suspendFlatMap DomainError.NotFound(
                    resource = "AccountSession",
                    id = accountId,
                ).err()

                val updatedSession = AccountSession(
                    accountId = resolvedSession.accountId,
                    email = resolvedSession.email,
                    sessionState = sessionState,
                    lastAuthenticatedAt = lastAuthenticatedAt ?: resolvedSession.lastAuthenticatedAt,
                )
                accountSessionDataSource.saveAccountSession(accountSession = updatedSession)
            }
    }

    override suspend fun signOut(accountId: String): Result<DomainError, Unit> {
        if (accountId.isBlank()) {
            return DomainError.ValidationError(
                field = "accountId",
                reason = "Account ID cannot be blank",
            ).err()
        }

        return authRemoteDataSource.signOut(accountId = accountId)
            .suspendFlatMap { clearPersistedSession(accountId = accountId) }
    }

    private suspend fun persistAuthenticatedAccount(
        remoteAccount: AuthenticatedRemoteAccount,
    ): Result<DomainError, AccountSession> =
        sessionCredentialDataSource.save(session = remoteAccount.session)
            .suspendFlatMap {
                accountSessionDataSource.saveAccountSession(accountSession = remoteAccount.toAccountSession())
            }

    private suspend fun clearPersistedSession(accountId: String): Result<DomainError, Unit> =
        sessionCredentialDataSource.clear()
            .suspendFlatMap {
                accountSessionDataSource.deleteAccountSession(accountId = accountId)
            }
            .map { Unit }
}

private fun AuthenticatedRemoteAccount.toAccountSession(): AccountSession = AccountSession(
    accountId = accountId,
    email = email,
    sessionState = SessionState.Active,
    lastAuthenticatedAt = lastAuthenticatedAt ?: session.lastAuthenticatedAt,
)

private fun AccountSession.toAccount(): Account = Account(
    id = accountId,
    email = email,
    status = when (sessionState) {
        SessionState.Active -> AccountStatus.SignedIn
        SessionState.SignedOut -> AccountStatus.SignedOut
        SessionState.Expired, SessionState.Unavailable -> AccountStatus.SignedIn
    },
)

private fun EmailPasswordCredentials.toDataSourceModel(): DataSourceEmailPasswordCredentials =
    DataSourceEmailPasswordCredentials(
        email = asModel().email,
        password = asModel().password,
    )
