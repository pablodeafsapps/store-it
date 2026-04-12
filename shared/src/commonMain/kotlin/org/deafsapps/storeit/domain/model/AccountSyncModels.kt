package org.deafsapps.storeit.domain.model

import kotlin.time.Clock

interface Account {
    val id: String
    val email: String
    val status: AccountStatus
    val createdAt: Long?
}

internal data class AccountModel(
    override val id: String,
    override val email: String,
    override val status: AccountStatus,
    override val createdAt: Long? = null,
) : Account

fun Account(
    id: String,
    email: String,
    status: AccountStatus,
    createdAt: Long? = null,
): Account = AccountModel(
    id = id,
    email = email,
    status = status,
    createdAt = createdAt,
)

internal fun Account.asModel(): AccountModel = when (this) {
    is AccountModel -> this
    else -> AccountModel(
        id = id,
        email = email,
        status = status,
        createdAt = createdAt,
    )
}

enum class AccountStatus {
    SignedOut,
    SignUpPending,
    SignInPending,
    SignedIn,
}

interface EmailPasswordCredentials {
    val email: String
    val password: String
}

internal data class EmailPasswordCredentialsModel(
    override val email: String,
    override val password: String,
) : EmailPasswordCredentials

fun EmailPasswordCredentials(
    email: String,
    password: String,
): EmailPasswordCredentials = EmailPasswordCredentialsModel(
    email = email,
    password = password,
)

internal fun EmailPasswordCredentials.asModel(): EmailPasswordCredentialsModel = when (this) {
    is EmailPasswordCredentialsModel -> this
    else -> EmailPasswordCredentialsModel(
        email = email,
        password = password,
    )
}

interface AccountSession {
    val accountId: String
    val email: String
    val sessionState: SessionState
    val lastAuthenticatedAt: Long?
}

internal data class AccountSessionModel(
    override val accountId: String,
    override val email: String,
    override val sessionState: SessionState,
    override val lastAuthenticatedAt: Long? = null,
) : AccountSession

fun AccountSession(
    accountId: String,
    email: String,
    sessionState: SessionState,
    lastAuthenticatedAt: Long? = null,
): AccountSession = AccountSessionModel(
    accountId = accountId,
    email = email,
    sessionState = sessionState,
    lastAuthenticatedAt = lastAuthenticatedAt,
)

internal fun AccountSession.asModel(): AccountSessionModel = when (this) {
    is AccountSessionModel -> this
    else -> AccountSessionModel(
        accountId = accountId,
        email = email,
        sessionState = sessionState,
        lastAuthenticatedAt = lastAuthenticatedAt,
    )
}

enum class SessionState {
    Active,
    Expired,
    SignedOut,
    Unavailable,
}

interface AccountDataset {
    val accountId: String
    val datasetVersion: String
    val lastSyncedAt: Long?
}

internal data class AccountDatasetModel(
    override val accountId: String,
    override val datasetVersion: String,
    override val lastSyncedAt: Long? = null,
) : AccountDataset

fun AccountDataset(
    accountId: String,
    datasetVersion: String,
    lastSyncedAt: Long? = null,
): AccountDataset = AccountDatasetModel(
    accountId = accountId,
    datasetVersion = datasetVersion,
    lastSyncedAt = lastSyncedAt,
)

internal fun AccountDataset.asModel(): AccountDatasetModel = when (this) {
    is AccountDatasetModel -> this
    else -> AccountDatasetModel(
        accountId = accountId,
        datasetVersion = datasetVersion,
        lastSyncedAt = lastSyncedAt,
    )
}

interface LocalDatasetState {
    val mode: DataMode
    val accountId: String?
    val lastLocalChangeAt: Long?
    val lastRemoteSyncAt: Long?
    val hasPendingChanges: Boolean
}

internal data class LocalDatasetStateModel(
    override val mode: DataMode,
    override val accountId: String? = null,
    override val lastLocalChangeAt: Long? = null,
    override val lastRemoteSyncAt: Long? = null,
    override val hasPendingChanges: Boolean = false,
) : LocalDatasetState

fun LocalDatasetState(
    mode: DataMode,
    accountId: String? = null,
    lastLocalChangeAt: Long? = null,
    lastRemoteSyncAt: Long? = null,
    hasPendingChanges: Boolean = false,
): LocalDatasetState = LocalDatasetStateModel(
    mode = mode,
    accountId = accountId,
    lastLocalChangeAt = lastLocalChangeAt,
    lastRemoteSyncAt = lastRemoteSyncAt,
    hasPendingChanges = hasPendingChanges,
)

internal fun LocalDatasetState.asModel(): LocalDatasetStateModel = when (this) {
    is LocalDatasetStateModel -> this
    else -> LocalDatasetStateModel(
        mode = mode,
        accountId = accountId,
        lastLocalChangeAt = lastLocalChangeAt,
        lastRemoteSyncAt = lastRemoteSyncAt,
        hasPendingChanges = hasPendingChanges,
    )
}

enum class DataMode {
    LocalOnly,
    AccountBackedSynchronized,
    AccountBackedPendingSync,
    ReconciliationRequired,
    SignedOutWithLocalCopy,
}

interface SyncState {
    val status: SyncStatus
    val failureReason: String?
    val lastAttemptAt: Long?
    val pendingOperationCount: Int
}

internal data class SyncStateModel(
    override val status: SyncStatus,
    override val failureReason: String? = null,
    override val lastAttemptAt: Long? = null,
    override val pendingOperationCount: Int = 0,
) : SyncState

fun SyncState(
    status: SyncStatus,
    failureReason: String? = null,
    lastAttemptAt: Long? = null,
    pendingOperationCount: Int = 0,
): SyncState = SyncStateModel(
    status = status,
    failureReason = failureReason,
    lastAttemptAt = lastAttemptAt,
    pendingOperationCount = pendingOperationCount,
)

internal fun SyncState.asModel(): SyncStateModel = when (this) {
    is SyncStateModel -> this
    else -> SyncStateModel(
        status = status,
        failureReason = failureReason,
        lastAttemptAt = lastAttemptAt,
        pendingOperationCount = pendingOperationCount,
    )
}

enum class SyncStatus {
    Idle,
    Syncing,
    Synchronized,
    PendingUpload,
    PendingDownload,
    Failed,
    RestorePending,
    BlockedByReconciliation,
}

interface SyncOperation {
    val id: String
    val accountId: String?
    val entityType: SyncEntityType
    val entityId: String
    val operationType: SyncOperationType
    val payloadJson: String?
    val syncStatus: SyncOperationStatus
    val recordedAt: Long
    val lastAttemptAt: Long?
    val failureReason: String?
}

internal data class SyncOperationModel(
    override val id: String,
    override val accountId: String? = null,
    override val entityType: SyncEntityType,
    override val entityId: String,
    override val operationType: SyncOperationType,
    override val payloadJson: String? = null,
    override val syncStatus: SyncOperationStatus,
    override val recordedAt: Long = Clock.System.now().toEpochMilliseconds(),
    override val lastAttemptAt: Long? = null,
    override val failureReason: String? = null,
) : SyncOperation

fun SyncOperation(
    id: String,
    accountId: String? = null,
    entityType: SyncEntityType,
    entityId: String,
    operationType: SyncOperationType,
    payloadJson: String? = null,
    syncStatus: SyncOperationStatus,
    recordedAt: Long = Clock.System.now().toEpochMilliseconds(),
    lastAttemptAt: Long? = null,
    failureReason: String? = null,
): SyncOperation = SyncOperationModel(
    id = id,
    accountId = accountId,
    entityType = entityType,
    entityId = entityId,
    operationType = operationType,
    payloadJson = payloadJson,
    syncStatus = syncStatus,
    recordedAt = recordedAt,
    lastAttemptAt = lastAttemptAt,
    failureReason = failureReason,
)

internal fun SyncOperation.asModel(): SyncOperationModel = when (this) {
    is SyncOperationModel -> this
    else -> SyncOperationModel(
        id = id,
        accountId = accountId,
        entityType = entityType,
        entityId = entityId,
        operationType = operationType,
        payloadJson = payloadJson,
        syncStatus = syncStatus,
        recordedAt = recordedAt,
        lastAttemptAt = lastAttemptAt,
        failureReason = failureReason,
    )
}

enum class SyncEntityType {
    Rack,
    ShelfSlot,
    Item,
    Dataset,
    Photo,
}

enum class SyncOperationType {
    Create,
    Update,
    Delete,
    Restore,
}

enum class SyncOperationStatus {
    Pending,
    Applied,
    Failed,
}

interface PhotoSyncScope {
    val photoId: String
    val ownerType: SyncEntityType
    val ownerId: String
    val localUri: String
    val remoteUrl: String?
    val checksum: String?
    val syncStatus: PhotoSyncStatus
    val lastSyncedAt: Long?
}

internal data class PhotoSyncScopeModel(
    override val photoId: String,
    override val ownerType: SyncEntityType,
    override val ownerId: String,
    override val localUri: String,
    override val remoteUrl: String? = null,
    override val checksum: String? = null,
    override val syncStatus: PhotoSyncStatus,
    override val lastSyncedAt: Long? = null,
) : PhotoSyncScope

fun PhotoSyncScope(
    photoId: String,
    ownerType: SyncEntityType,
    ownerId: String,
    localUri: String,
    remoteUrl: String? = null,
    checksum: String? = null,
    syncStatus: PhotoSyncStatus,
    lastSyncedAt: Long? = null,
): PhotoSyncScope = PhotoSyncScopeModel(
    photoId = photoId,
    ownerType = ownerType,
    ownerId = ownerId,
    localUri = localUri,
    remoteUrl = remoteUrl,
    checksum = checksum,
    syncStatus = syncStatus,
    lastSyncedAt = lastSyncedAt,
)

internal fun PhotoSyncScope.asModel(): PhotoSyncScopeModel = when (this) {
    is PhotoSyncScopeModel -> this
    else -> PhotoSyncScopeModel(
        photoId = photoId,
        ownerType = ownerType,
        ownerId = ownerId,
        localUri = localUri,
        remoteUrl = remoteUrl,
        checksum = checksum,
        syncStatus = syncStatus,
        lastSyncedAt = lastSyncedAt,
    )
}

enum class PhotoSyncStatus {
    PendingUpload,
    PendingDelete,
    Synced,
    Failed,
}

interface ReconciliationDecision {
    val accountId: String
    val decisionType: ReconciliationDecisionType
    val confirmedAt: Long
    val appliedAt: Long?
}

internal data class ReconciliationDecisionModel(
    override val accountId: String,
    override val decisionType: ReconciliationDecisionType,
    override val confirmedAt: Long = Clock.System.now().toEpochMilliseconds(),
    override val appliedAt: Long? = null,
) : ReconciliationDecision

fun ReconciliationDecision(
    accountId: String,
    decisionType: ReconciliationDecisionType,
    confirmedAt: Long = Clock.System.now().toEpochMilliseconds(),
    appliedAt: Long? = null,
): ReconciliationDecision = ReconciliationDecisionModel(
    accountId = accountId,
    decisionType = decisionType,
    confirmedAt = confirmedAt,
    appliedAt = appliedAt,
)

internal fun ReconciliationDecision.asModel(): ReconciliationDecisionModel = when (this) {
    is ReconciliationDecisionModel -> this
    else -> ReconciliationDecisionModel(
        accountId = accountId,
        decisionType = decisionType,
        confirmedAt = confirmedAt,
        appliedAt = appliedAt,
    )
}

enum class ReconciliationDecisionType {
    KeepLocal,
    KeepRemote,
}
