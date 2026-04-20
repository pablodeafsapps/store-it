package org.deafsapps.storeit.domain.repository

import kotlinx.coroutines.flow.Flow
import org.deafsapps.storeit.base.Result
import org.deafsapps.storeit.domain.model.Account
import org.deafsapps.storeit.domain.model.AccountDataset
import org.deafsapps.storeit.domain.model.AccountSession
import org.deafsapps.storeit.domain.model.DataMode
import org.deafsapps.storeit.domain.model.DomainError
import org.deafsapps.storeit.domain.model.EmailPasswordCredentials
import org.deafsapps.storeit.domain.model.LocalDatasetState
import org.deafsapps.storeit.domain.model.PhotoSyncScope
import org.deafsapps.storeit.domain.model.ReconciliationDecision
import org.deafsapps.storeit.domain.model.ReconciliationDecisionType
import org.deafsapps.storeit.domain.model.SessionState
import org.deafsapps.storeit.domain.model.SyncOperation
import org.deafsapps.storeit.domain.model.SyncState

/**
 * Defines account authentication, session restoration, and sign-out operations.
 */
interface AccountRepository {
    /**
     * Observes the current account identity, if one is available.
     */
    fun observeAccount(): Flow<Result<DomainError, Account?>>

    /**
     * Observes the current persisted session state, if one is available.
     */
    fun observeSession(): Flow<Result<DomainError, AccountSession?>>

    /**
     * Creates a new account using email/password credentials.
     */
    suspend fun signUp(credentials: EmailPasswordCredentials): Result<DomainError, AccountSession>

    /**
     * Signs an existing account in with email/password credentials.
     */
    suspend fun signIn(credentials: EmailPasswordCredentials): Result<DomainError, AccountSession>

    /**
     * Restores the last persisted session if one exists.
     */
    suspend fun restoreSession(): Result<DomainError, AccountSession?>

    /**
     * Updates the stored session state after provider-side invalidation or refresh.
     */
    suspend fun updateSessionState(
        accountId: String,
        sessionState: SessionState,
        lastAuthenticatedAt: Long? = null,
    ): Result<DomainError, AccountSession>

    /**
     * Signs the current account out.
     */
    suspend fun signOut(accountId: String): Result<DomainError, Unit>
}

/**
 * Defines synchronization state, queued operations, and dataset checkpoint operations.
 */
interface SyncRepository {
    /**
     * Observes the current local dataset mode and state.
     */
    fun observeLocalDatasetState(): Flow<Result<DomainError, LocalDatasetState?>>

    /**
     * Observes the current synchronization state.
     */
    fun observeSyncState(): Flow<Result<DomainError, SyncState?>>

    /**
     * Observes the queued sync operations in recorded order.
     */
    fun observePendingOperations(): Flow<Result<DomainError, List<SyncOperation>>>

    /**
     * Returns the last known remote dataset checkpoint for an account.
     */
    suspend fun getAccountDataset(accountId: String): Result<DomainError, AccountDataset?>

    /**
     * Persists the last known remote dataset checkpoint for an account.
     */
    suspend fun saveAccountDataset(accountDataset: AccountDataset): Result<DomainError, AccountDataset>

    /**
     * Persists the current local dataset mode and sync relationship metadata.
     */
    suspend fun saveLocalDatasetState(localDatasetState: LocalDatasetState): Result<DomainError, LocalDatasetState>

    /**
     * Persists the current user-visible synchronization state.
     */
    suspend fun saveSyncState(syncState: SyncState): Result<DomainError, SyncState>

    /**
     * Adds or updates a queued sync operation.
     */
    suspend fun saveSyncOperation(syncOperation: SyncOperation): Result<DomainError, SyncOperation>

    /**
     * Removes a queued sync operation after it has been applied or discarded.
     */
    suspend fun deleteSyncOperation(operationId: String): Result<DomainError, Unit>

    /**
     * Clears all queued sync operations.
     */
    suspend fun clearSyncOperations(): Result<DomainError, Unit>
}

/**
 * Defines the account-backed restore operation that hydrates local data from the remote account dataset.
 */
interface AccountDataRestoreRepository {
    /**
     * Restores the remote account dataset into the local store and updates synchronization metadata.
     */
    suspend fun restoreAccountData(session: AccountSession): Result<DomainError, Unit>
}

/**
 * Defines user-controlled reconciliation operations for divergent local and remote datasets.
 */
interface ReconciliationRepository {
    /**
     * Returns whether the current device state requires reconciliation.
     */
    suspend fun requiresReconciliation(accountId: String): Result<DomainError, Boolean>

    /**
     * Returns the current local dataset state that drove reconciliation.
     */
    suspend fun getLocalDatasetState(): Result<DomainError, LocalDatasetState?>

    /**
     * Returns the last known remote dataset checkpoint that drove reconciliation.
     */
    suspend fun getRemoteDataset(accountId: String): Result<DomainError, AccountDataset?>

    /**
     * Persists a confirmed reconciliation decision.
     */
    suspend fun saveDecision(decision: ReconciliationDecision): Result<DomainError, ReconciliationDecision>

    /**
     * Applies the confirmed keep-local or keep-remote outcome.
     */
    suspend fun applyDecision(
        accountId: String,
        decisionType: ReconciliationDecisionType,
    ): Result<DomainError, DataMode>
}

/**
 * Defines local tracking for photo backup work associated with racks and items.
 */
interface PhotoBackupRepository {
    /**
     * Observes the current photo sync scope entries.
     */
    fun observePhotoSyncScope(): Flow<Result<DomainError, List<PhotoSyncScope>>>

    /**
     * Persists or updates a photo sync scope entry.
     */
    suspend fun savePhotoSyncScope(photoSyncScope: PhotoSyncScope): Result<DomainError, PhotoSyncScope>

    /**
     * Removes a photo sync scope entry.
     */
    suspend fun deletePhotoSyncScope(photoId: String): Result<DomainError, Unit>
}

/**
 * Defines secure storage for the minimum credential/session material needed for restore.
 */
interface SessionCredentialRepository {
    /**
     * Persists authenticated session material securely.
     */
    suspend fun save(session: AccountSession): Result<DomainError, Unit>

    /**
     * Restores authenticated session material if available.
     */
    suspend fun restore(): Result<DomainError, AccountSession?>

    /**
     * Removes any stored session material.
     */
    suspend fun clear(): Result<DomainError, Unit>
}
