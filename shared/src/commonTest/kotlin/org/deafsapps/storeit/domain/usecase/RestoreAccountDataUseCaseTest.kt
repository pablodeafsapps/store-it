package org.deafsapps.storeit.domain.usecase

import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.base.err
import org.deafsapps.storeit.base.ok
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.repository.AccountDataRestoreRepository

class RestoreAccountDataUseCaseTest {
    private lateinit var sut: RestoreAccountDataUseCase
    private lateinit var fakeAccountDataRestoreRepository: FakeAccountDataRestoreRepository

    @BeforeTest
    fun setUp() {
        fakeAccountDataRestoreRepository = FakeAccountDataRestoreRepository()
        sut = RestoreAccountDataUseCase(
            accountDataRestoreRepository = fakeAccountDataRestoreRepository,
        )
    }

    @Test
    fun `GIVEN active account session WHEN invoke THEN delegates restore to account data repository`() =
        runTest {
            val session = accountSession()
            fakeAccountDataRestoreRepository.restoreResult = Unit.ok()

            val result = sut.invoke(session)

            assertTrue(actual = result.isOk)
            assertEquals(expected = session, actual = fakeAccountDataRestoreRepository.restoreSession)
        }

    @Test
    fun `GIVEN inactive session WHEN invoke THEN returns validation error without restoring`() =
        runTest {
            val session = accountSession(sessionState = SessionState.Expired)

            val result = sut.invoke(session)

            assertTrue(actual = result.isErr)
            assertEquals(expected = null, actual = fakeAccountDataRestoreRepository.restoreSession)
        }
}

private fun accountSession(
    sessionState: SessionState = SessionState.Active,
): AccountSession = AccountSession(
    accountId = "account-1",
    email = "user@example.com",
    sessionState = sessionState,
    lastAuthenticatedAt = 20L,
)

private class FakeAccountDataRestoreRepository : AccountDataRestoreRepository {
    var restoreResult: Result<DomainError, Unit> = DomainError.Unknown(
        message = "restoreResult not configured",
    ).err()
    var restoreSession: AccountSession? = null

    override suspend fun restoreAccountData(session: AccountSession): Result<DomainError, Unit> {
        restoreSession = session
        return restoreResult
    }
}
