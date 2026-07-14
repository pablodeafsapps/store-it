# Data Model: Remote Account Sync And Backup

**Feature**: 005-remote-sync-auth  
**Date**: 2026-04-09

## Entities

### Account

- **Description**: A user identity that enables remote backup and restore.
- **Attributes**:
  - **id**: Stable account identifier.
  - **email** (or equivalent login identifier): User-facing sign-in identifier.
  - **status**: Authentication status such as signed-out, signed-in, or sign-in-pending.
  - **createdAt**: Account creation timestamp if available from the provider.
- **Validation**:
  - `id` must be non-empty for signed-in state.
  - Login identifier must match provider rules.
- **Relationships**:
  - Owns exactly one remote account dataset.

### AccountSession

- **Description**: The authenticated session state stored locally to restore account-backed access on later launches.
- **Attributes**:
  - **accountId**: Reference to the signed-in account.
  - **sessionState**: Active, expired, signed-out, or unavailable.
  - **lastAuthenticatedAt**: Timestamp of last successful authentication or session refresh.
- **Validation**:
  - Active session requires a valid `accountId`.
- **Relationships**:
  - Belongs to one `Account`.

### AccountDataset

- **Description**: The remotely backed-up dataset associated with one account.
- **Attributes**:
  - **accountId**: Owner of the dataset.
  - **datasetVersion**: Logical version or sync token representing the last known remote snapshot.
  - **lastSyncedAt**: Timestamp of last successful local/remote alignment.
- **Validation**:
  - `accountId` required.
- **Relationships**:
  - Contains the user’s racks, shelf slots, items, and related metadata already defined by the organizer domain.

### LocalDatasetState

- **Description**: Metadata about the device’s local copy and its relationship to the account-backed dataset.
- **Attributes**:
  - **mode**: Local-only, account-backed, reconciliation-required, or signed-out-with-local-copy.
  - **lastLocalChangeAt**: Timestamp of most recent confirmed local mutation.
  - **lastRemoteSyncAt**: Timestamp of last successful sync with remote dataset.
  - **hasPendingChanges**: Boolean indicating unsynchronized local edits.
- **Validation**:
  - `mode` must always be present.
- **Relationships**:
  - Refers to one current device-local dataset.

### SyncState

- **Description**: The current synchronization state shown to the user and used by orchestration logic.
- **Attributes**:
  - **status**: Idle, syncing, synchronized, pending-upload, pending-download, failed, reconciliation-required.
  - **failureReason**: Optional recoverable failure summary.
  - **lastAttemptAt**: Timestamp of most recent sync attempt.
  - **pendingOperationCount**: Number of local operations still waiting to synchronize.
- **Validation**:
  - `failureReason` only present when `status` is failed or blocked.
- **Relationships**:
  - Derived from local dataset state, remote dataset state, and session state.

### SyncOperation

- **Description**: A tracked local change that must be uploaded or reconciled with remote data.
- **Attributes**:
  - **id**: Unique operation identifier.
  - **entityType**: Rack, shelf slot, item, or dataset-level operation.
  - **entityId**: Identifier of the affected domain entity.
  - **operationType**: Create, update, delete.
  - **recordedAt**: Timestamp when the local change was recorded.
  - **syncStatus**: Pending, applied, failed.
- **Validation**:
  - `entityType`, `entityId`, and `operationType` required.
- **Relationships**:
  - Belongs to one local dataset and contributes to `SyncState`.

### ReconciliationDecision

- **Description**: A user-confirmed outcome for resolving non-empty local and remote datasets when they cannot be assumed identical.
- **Attributes**:
  - **id**: Unique decision identifier.
  - **accountId**: Account for which the decision applies.
  - **decisionType**: Merge, keep-local, keep-remote, or other approved policy chosen by product design.
  - **confirmedAt**: Timestamp of user confirmation.
  - **appliedAt**: Timestamp when the chosen reconciliation policy was executed.
- **Validation**:
  - Must be explicitly confirmed before application.
- **Relationships**:
  - Belongs to one `Account`.
  - Resolves one reconciliation-needed state between local and remote datasets.

## Relationships Summary

- `Account` 1:1 `AccountDataset`
- `Account` 1:N `AccountSession` history, with one active session per device
- `LocalDatasetState` 1:N `SyncOperation`
- `Account` 1:N `ReconciliationDecision`
- Existing organizer entities (`Rack`, `ShelfSlot`, `Item`) remain unchanged and are members of both the local dataset and remote account dataset

## Validation Rules

- Local-only mode must not require an `Account`.
- Signed-in synchronized mode requires both a valid `Account` and an active `AccountSession`.
- Reconciliation is required when both local and remote datasets are non-empty and the system cannot safely assume they are identical.
- Sign-out must not silently discard local unsynchronized data.
- Pending sync operations must survive app restarts until applied or explicitly resolved.

## State Transitions

### Account / Session

- Signed-out → Sign-up pending → Signed-in
- Signed-out → Sign-in pending → Signed-in
- Signed-in → Session unavailable → Signed-in with pending restore retry
- Signed-in → Sign-out pending → Signed-out or signed-out-with-local-copy

### Local / Sync

- Local-only → Account-backed pending reconciliation → Account-backed synchronized
- Account-backed synchronized → Pending-upload after local edit
- Pending-upload → Synchronizing → Synchronized
- Synchronizing → Failed
- Failed → Pending-upload or pending-download after retry
- Any signed-in mode → Reconciliation-required when local and remote datasets diverge beyond safe automatic handling

## Notes

- The existing organizer entities should not be redefined; this model adds account, sync, and reconciliation concepts around them.
- Record-level sync metadata should be sufficient for first implementation; full collaborative editing semantics are out of scope.
