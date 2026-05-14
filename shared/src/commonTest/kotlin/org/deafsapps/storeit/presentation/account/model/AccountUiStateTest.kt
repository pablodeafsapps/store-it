package org.deafsapps.storeit.presentation.account.model

import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.SyncStatus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountUiStateTest {
    private lateinit var sut: AccountUiState

    @BeforeTest
    fun setUp() {
        sut = AccountUiState.getDefault()
    }

    @Test
    fun `GIVEN authenticated synchronized account WHEN reading state THEN account is ready`() {
        sut = sut.copy(
            isAuthenticated = true,
            accountEmail = "user@example.com",
            dataMode = DataMode.AccountBackedSynchronized,
            syncStatus = SyncStatus.Synchronized,
        )

        assertEquals(expected = true, actual = sut.isAccountReady)
        assertEquals(expected = false, actual = sut.isRestoreInProgress)
    }

    @Test
    fun `GIVEN authenticated restore pending account WHEN reading state THEN restore is in progress`() {
        sut = sut.copy(
            isAuthenticated = true,
            accountEmail = "user@example.com",
            dataMode = DataMode.AccountBackedPendingSync,
            syncStatus = SyncStatus.RestorePending,
        )

        assertEquals(expected = false, actual = sut.isAccountReady)
        assertEquals(expected = true, actual = sut.isRestoreInProgress)
    }

    @Test
    fun `GIVEN local only account WHEN reading state THEN no remote ready or restore state is exposed`() {
        sut = sut.copy(
            isAuthenticated = false,
            accountEmail = null,
            dataMode = DataMode.LocalOnly,
            syncStatus = SyncStatus.Idle,
        )

        assertEquals(expected = false, actual = sut.isAccountReady)
        assertEquals(expected = false, actual = sut.isRestoreInProgress)
    }

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