# Feature Specification: Remote Account Sync And Backup

**Feature Branch**: `005-remote-sync-auth`  
**Created**: 2026-04-09  
**Status**: Draft  
**Input**: User description: "Allow \"Store it!\" to connect to a remote infrastructure to provide persistence across sessions.

The idea is that the user can work locally if he/she wants, but also allow him/her to log in/sign up and back-up his/her data using a remote infraestructure. Doing so, there'll always be a source of truth, but also a local copy to work with until the next synchronization."

## User Scenarios & Testing *(mandatory)*

### Authentication Scope

- Sign-up and sign-in are in scope for this feature.
- Password recovery, account deletion, profile editing, and multi-factor authentication are out of scope unless later promoted into a separate feature.
- The initial sign-up/sign-in credential set for this feature is email plus password only.

### User Story 1 - Back Up With An Account (Priority: P1)

A user creates an account or signs in so their racks, slots, and items are backed up remotely and restored when they return in a later session or use another device. Once signed in, the remote account data becomes the long-term source of truth and the app keeps a local copy for day-to-day use.

**Why this priority**: This is the core value of the feature. Without account-backed persistence, the app remains device-local only and cannot provide reliable cross-session or cross-device continuity.

**Independent Test**: A user can sign up or sign in, create or edit data, close the app, reopen it, and see the same data restored from their account on a fresh session.

**Acceptance Scenarios**:

1. **Given** a user does not yet have an account, **When** they choose to sign up and complete the required account details, **Then** the app creates their account and enables remote backup for their data.
2. **Given** a user already has an account, **When** they sign in on a new session or another device, **Then** the app restores their previously backed-up racks, slots, and items into the local app copy.
3. **Given** a signed-in user changes their stored data, **When** synchronization completes and the app shows the synchronized state, **Then** the remote account reflects the same confirmed dataset and remains available for later restoration.

---

### User Story 2 - Keep Working Offline (Priority: P2)

A signed-in user continues working from the local copy even when connectivity is unavailable. Their changes stay on the device and are synchronized the next time the app can reach the remote service.

**Why this priority**: Backup alone is not enough if the user loses access to their working copy whenever the network is unavailable. The local-first experience protects the product’s usefulness in real environments such as garages, storage rooms, and basements.

**Independent Test**: A signed-in user opens the app without connectivity, can still browse and update their stored data locally, and sees those changes synchronized after connectivity returns.

**Acceptance Scenarios**:

1. **Given** a user is signed in and already has synchronized data, **When** the app is opened without connectivity, **Then** the local copy is still available for browsing and editing.
2. **Given** a signed-in user makes changes while offline, **When** connectivity returns and synchronization runs, **Then** those pending changes are uploaded and reflected in the remote account.
3. **Given** the app cannot complete synchronization, **When** the user continues working locally, **Then** the app preserves their unsynchronized changes and shows that sync is still pending instead of discarding data.

---

### User Story 3 - Stay Local By Choice (Priority: P3)

A user who does not want an account can continue using the app in local-only mode. Later, they can decide to sign up or sign in and connect that local dataset to remote backup without losing their existing data.

**Why this priority**: The current MVP does not require login, so this feature must extend the product without turning account creation into a mandatory gate.

**Independent Test**: A user can use the app without signing in, keep data only on the device, and later connect an account so that the existing local dataset becomes backed up remotely.

**Acceptance Scenarios**:

1. **Given** a user launches the app for the first time, **When** they skip account creation, **Then** they can continue in local-only mode and still use the core organizer flows.
2. **Given** a user has existing local-only data, **When** they later sign up or sign in and choose to back up their data, **Then** the app MUST either upload the local dataset into an empty remote account dataset or require an explicit reconciliation decision before any overwrite or merge is applied.
3. **Given** a user signs out of an account-backed session, **When** the sign-out completes, **Then** the app clearly indicates whether the device is now in local-only mode and which local data remains available on the device.

---

### Edge Cases

- What happens when a user tries to sign in while offline and there is no previously synchronized local copy? The system MUST explain that sign-in cannot complete yet and must not imply that account data has been downloaded.
- What happens when an upload is interrupted by connectivity loss or app termination? The system MUST keep the last confirmed local data, preserve pending upload operations, and retry later without requiring the user to recreate data.
- What happens when a download or restore is interrupted by connectivity loss or app termination? The system MUST keep the last confirmed local data already present on the device, must not present a partial restore as complete, and MUST retry the restore later.
- What happens when reconciliation is interrupted by connectivity loss or app termination? The system MUST preserve the unresolved reconciliation-required state and must not silently apply a partial outcome.
- What happens when the same account changes data on more than one device before synchronization completes? The system MUST resolve the dataset consistently and must not silently lose confirmed user data.
- What happens when a user with existing local-only data signs in to an account that already contains remote data? The system MUST present a clear, recoverable way to decide how local and remote data are reconciled.
- What happens when sign-up fails because the account already exists or the credentials are invalid? The system MUST show a clear error and keep the user’s local data untouched.
- What happens when a signed-in user signs out while local changes are still waiting to sync? The system MUST warn the user before sign-out if doing so could delay or prevent remote backup of those changes.
- What happens when local account-backed data is missing, outdated, or corrupted relative to the remote account? The system MUST prefer the last confirmed remote account dataset for recovery, MUST preserve any still-readable unsynchronized local changes for explicit handling where possible, and MUST avoid presenting corrupted local data as authoritative.
- What happens when authentication succeeds but restore cannot complete? The system MUST show that the user is authenticated but not yet fully restored, and MUST provide a retry path without erasing still-valid local data.

## Operational Definitions

### Data Modes

- **Local-only mode**: no signed-in account is active on the device; organizer data exists only on-device.
- **Account-backed synchronized mode**: a signed-in account is active; local and remote datasets are aligned and the app shows data as safely backed up.
- **Account-backed pending-sync mode**: a signed-in account is active; local changes or remote recovery work are still pending.
- **Reconciliation-required mode**: a signed-in account is active, but local and remote datasets both contain meaningful data and the app requires an explicit user decision before continuing.
- **Signed-out-with-local-copy mode**: the previously signed-in account session has ended; the device retains a defined local dataset state and the app explains whether that dataset remains only local.

### Authority Rules

- While the user is actively editing, the local database is the working copy used for reads and writes.
- After a successful synchronization, the remote account dataset becomes the long-term recovery source for signed-in users.
- While offline or while sync is pending, the remote dataset is not treated as immediately available for reads, but it remains the authoritative recovery target once synchronization can complete.
- Reconciliation-required state temporarily suspends automatic authority assumptions until the user confirms how local and remote data should be handled.

### Synchronization Completed

Synchronization is considered complete only when all of the following are true:

- the local queue of pending sync operations for the active account is empty
- the latest confirmed local dataset has been applied to or restored from the remote account as required
- the app exposes a user-visible synchronized state
- no reconciliation decision is still required for the active account/device state

### Failure Categories

- **Recoverable authentication failure**: invalid credentials, account already exists, session expired, offline sign-in without cached session, or provider temporarily unavailable.
- **Recoverable synchronization failure**: network interruption, provider timeout, interrupted upload/download, or a retryable remote write/read failure.
- **Blocking reconciliation state**: both local and remote datasets exist and no safe automatic outcome is permitted.
- **Unrecoverable failure**: a state where the app cannot currently restore local access or remote recovery without explicit user intervention.

## Data Scope

### Account Dataset Boundary

The account dataset for this feature includes:

- all racks
- all shelf slots
- all items
- the relationships between racks, slots, and items
- user-entered organizer metadata already supported by the app for those entities
- sync metadata needed to determine whether the local device is aligned with the remote account

The account dataset does not automatically include:

- platform-specific UI preferences unless explicitly added later
- account-management profile fields beyond the chosen sign-up/sign-in credentials
- analytics, diagnostics, or ephemeral client-only state

Photos are part of the mandatory backed-up dataset for MVP wherever the current app already treats them as part of rack or item data.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow users to continue using the app without creating an account.
- **FR-002**: System MUST allow users to create an account and sign in using the credential set defined for this feature to enable remote backup of their account dataset.
- **FR-003**: System MUST restore a signed-in user’s previously backed-up data into the local app copy when they return in a later session or sign in on another device.
- **FR-004**: System MUST keep a local copy of account-backed data so signed-in users can continue browsing and editing while offline or when the remote service is unavailable.
- **FR-005**: System MUST synchronize local changes made by a signed-in user to the remote account when synchronization becomes possible again.
- **FR-006**: System MUST treat the remote account dataset as the long-term source of truth for signed-in users after synchronization completes.
- **FR-007**: System MUST preserve pending local changes if synchronization cannot complete and MUST retry synchronization later without forcing the user to re-enter data.
- **FR-008**: System MUST make the user’s current data mode visible, including whether they are using local-only mode, signed-in synchronized mode, or signed-in mode with pending synchronization.
- **FR-009**: System MUST let a local-only user connect an account later without silently discarding the data already stored on the device, and MUST require an explicit reconciliation decision whenever both local and remote non-empty datasets exist.
- **FR-010**: System MUST provide a clear reconciliation flow when local device data and remote account data both exist and cannot be safely assumed to be identical.
- **FR-011**: System MUST allow signed-in users to sign out and MUST explain whether the device keeps a local copy, removes account-backed local data, or preserves only unsynchronized data according to the product rule chosen for sign-out behavior.
- **FR-012**: System MUST prevent silent data loss during sign-up, sign-in, sign-out, synchronization failure, or reconciliation.
- **FR-013**: System MUST provide user-visible status for synchronization success, pending changes, and recoverable synchronization failures.
- **FR-014**: System MUST distinguish at least the following user-visible sync states: local-only, synchronized, pending upload, pending restore/download, failed with retry available, and reconciliation required.
- **FR-015**: System MUST make it clear to the user whether their current dataset is only local, pending backup, or safely backed up.
- **FR-016**: System MUST keep local-only mode available both for first-time users and for users who previously signed out of an account-backed session.
- **FR-017**: System MUST define a sign-out outcome for each of these cases: no pending changes, pending unsynchronized changes, and corrupted or incomplete local account-backed data.
- **FR-018**: System MUST show when authentication has succeeded but restore is still pending or failed.
- **FR-019**: System MUST indicate, after a multi-device conflict is resolved, which dataset outcome was applied to the local device.

### Key Entities

- **Account**: A user identity that can be created, authenticated, and associated with a remotely backed-up dataset.
- **Account Dataset**: The full set of racks, shelf slots, items, and related metadata belonging to one signed-in user and stored remotely as their long-term backed-up data.
- **Local Copy**: The on-device dataset used for browsing and editing. In local-only mode it is the only dataset. In account-backed mode it is the working copy of the signed-in account dataset. After sign-out it may become either a retained local-only dataset or be removed, depending on the sign-out rule applied.
- **Sync State**: The current state of data alignment between the local copy and the remote account dataset, including synchronized, pending upload, pending download, failed, or reconciliation required.
- **Reconciliation Decision**: A user-confirmed outcome that determines how existing local data and existing remote data are merged, replaced, or otherwise preserved when both are present.

## Reconciliation Rules

- Reconciliation is required when both local and remote datasets are non-empty and the system cannot safely prove that they are already aligned.
- The user, not the system, chooses the reconciliation outcome when reconciliation is required.
- The app MUST require that choice before it overwrites or merges meaningful local or remote data.
- The minimum supported reconciliation outcomes are:
  - keep local as the basis for the resulting dataset
  - keep remote as the basis for the resulting dataset
- The first release supports only the following explicit reconciliation outcomes:
  - keep local
  - keep remote
- Merge is out of scope for the first release.
- The app MUST show when reconciliation is required and MUST make clear when the chosen outcome has been applied.

## Assumptions

- Local-only mode remains part of the product and is not deprecated by this feature.
- The feature covers one active signed-in account at a time on a device.
- Existing organizer data types stay the same; this feature extends persistence and account behavior rather than redefining racks, slots, or items.
- Users expect device-local continuity even before they adopt account-backed backup.
- Password recovery, profile editing, and account deletion may be delivered later unless they are required to complete sign-up, sign-in, sign-out, backup, and restore flows.
- Remote account availability and identity-provider availability are external dependencies and may fail temporarily; the feature must degrade into explicit pending or failure states rather than silent data loss.
- On sign-out, the app keeps the last readable local dataset on the device and switches that dataset into local-only mode.

## Non-Functional Requirements

- **NFR-001**: Account and synchronization messaging MUST be understandable enough for a user to distinguish local-only data, pending backup, and confirmed backup states.
- **NFR-002**: The app MUST store session material only in platform-secure storage, not in plain local app storage intended for organizer data.
- **NFR-003**: The app MUST avoid exposing account-backed organizer data to another signed-in account on the same device.
- **NFR-004**: User-visible sync status changes SHOULD appear promptly after a status transition is detected.
- For MVP, “promptly” is a best-effort requirement with no explicit timing commitment.
- Privacy wording, data retention wording, and account-recovery wording will live in a separate security/privacy document referenced by this spec.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: At least 90% of users who choose account-backed backup can complete sign-up or sign-in and reach the user-visible synchronized state in under 3 minutes.
- **SC-002**: At least 95% of signed-in users who reopen the app after a prior successful synchronization see their stored data restored without manual re-entry.
- **SC-003**: At least 90% of signed-in users can continue completing the primary organizer flows while offline using the local copy.
- **SC-004**: At least 90% of offline changes made by signed-in users are synchronized successfully on the next available synchronization attempt without requiring manual recreation of data.
- **SC-005**: Fewer than 2% of sign-up, sign-in, sign-out, or synchronization attempts end in an unrecoverable error that blocks the user from either accessing their local data or restoring their backed-up data.

### Measurement Notes

- A **prior successful synchronization** means the app previously reached the synchronized state after authentication for the same account on that device or another device tied to that account.
- A **next available synchronization attempt** means the first retry opportunity after connectivity and provider availability return, excluding cases blocked by reconciliation-required state or explicit sign-out.
- During MVP, these success criteria will be validated through a mixed approach: manual validation for early feature acceptance and analytics or telemetry later where available.
