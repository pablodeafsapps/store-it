package org.deafsapps.storeit.presentation.account

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.SyncStatus
import org.deafsapps.storeit.presentation.account.model.AccountUiState

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
}
