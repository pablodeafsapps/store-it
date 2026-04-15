package org.deafsapps.storeit.domain.usecase

import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.UseCase
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.EmailPasswordCredentials
import org.deafsapps.storeit.domain.repository.AccountRepository

internal interface SignUpAccountUseCaseType :
    UseCase<EmailPasswordCredentials, Result<DomainError, AccountSession>>

internal class SignUpAccountUseCase(
    private val accountRepository: AccountRepository,
) : SignUpAccountUseCaseType {
    override suspend fun invoke(input: EmailPasswordCredentials): Result<DomainError, AccountSession> =
        accountRepository.signUp(credentials = input)
}

internal interface SignInAccountUseCaseType :
    UseCase<EmailPasswordCredentials, Result<DomainError, AccountSession>>

internal class SignInAccountUseCase(
    private val accountRepository: AccountRepository,
) : SignInAccountUseCaseType {
    override suspend fun invoke(input: EmailPasswordCredentials): Result<DomainError, AccountSession> =
        accountRepository.signIn(credentials = input)
}

internal interface RestoreAccountSessionUseCaseType :
    UseCase<Unit, Result<DomainError, AccountSession?>>

internal class RestoreAccountSessionUseCase(
    private val accountRepository: AccountRepository,
) : RestoreAccountSessionUseCaseType {
    override suspend fun invoke(input: Unit): Result<DomainError, AccountSession?> =
        accountRepository.restoreSession()

    suspend operator fun invoke(): Result<DomainError, AccountSession?> = invoke(input = Unit)
}
