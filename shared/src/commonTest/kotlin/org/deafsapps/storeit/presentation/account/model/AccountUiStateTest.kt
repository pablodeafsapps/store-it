package org.deafsapps.storeit.presentation.account.model

import kotlin.test.Test
import kotlin.test.assertEquals
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.SyncStatus

class AccountUiStateTest {

    @Test
    fun `GIVEN default account UI state WHEN read THEN exposes local unauthenticated idle mode`() {

        val sut = AccountUiState.getDefault()

        assertEquals(expected = false, actual = sut.isLoading)
        assertEquals(expected = false, actual = sut.isAuthenticated)
        assertEquals(expected = null, actual = sut.accountEmail)
        assertEquals(expected = DataMode.LocalOnly, actual = sut.dataMode)
        assertEquals(expected = SyncStatus.Idle, actual = sut.syncStatus)
        assertEquals(expected = true, actual = sut.isLocalOnly)
        assertEquals(expected = false, actual = sut.isDataBackedUp)
        assertEquals(expected = false, actual = sut.canRetryRestore)
    }

    @Test
    fun `GIVEN synchronized account UI state WHEN read THEN exposes backed up account mode`() {

        val sut = AccountUiState(
            isLoading = false,
            isAuthenticated = true,
            accountEmail = "user@example.com",
            dataMode = DataMode.AccountBackedSynchronized,
            syncStatus = SyncStatus.Synchronized,
            pendingOperationCount = 0,
            failureMessage = null,
        )

        assertEquals(expected = true, actual = sut.isDataBackedUp)
        assertEquals(expected = false, actual = sut.hasPendingSyncWork)
        assertEquals(expected = false, actual = sut.canRetryRestore)
    }

    @Test
    fun `GIVEN restore pending account UI state WHEN read THEN exposes retry restore state`() {

        val sut = AccountUiState(
            isLoading = false,
            isAuthenticated = true,
            accountEmail = "user@example.com",
            dataMode = DataMode.AccountBackedPendingSync,
            syncStatus = SyncStatus.RestorePending,
            pendingOperationCount = 1,
            failureMessage = "Restore failed",
        )

        assertEquals(expected = false, actual = sut.isDataBackedUp)
        assertEquals(expected = true, actual = sut.hasPendingSyncWork)
        assertEquals(expected = true, actual = sut.canRetryRestore)
        assertEquals(expected = "Restore failed", actual = sut.failureMessage)
    }
}
