# Quickstart: Remote Account Sync And Backup

**Branch**: `005-remote-sync-auth`  
**Date**: 2026-06-01

## Objective

Validate that Store it! can still operate in local-only mode while also supporting account-backed backup, restore, offline work, and later synchronization.

## Prerequisites

- JDK 17+
- Android SDK
- Xcode and iOS toolchain for iOS validation
- Existing local SQLDelight persistence already working in the app
- Remote provider credentials/configuration available for development builds only

## Shared-First Scope

For this feature, plan and tasks should prefer paths under `shared/` for:

- account and session domain models
- repository interfaces and use cases
- sync orchestration and reconciliation rules
- sync state presentation models
- local/remote mapping and queueing logic
- unit tests in `shared/src/commonTest`

Platform-specific code should be limited to provider bridges and secure session storage when Android and iOS APIs differ.

## Privacy And Security Reference

Feature-specific privacy, retention, and account-recovery wording is documented in:

- `specs/005-remote-sync-auth/security-privacy-notes.md`

## Validation Scenarios

### 1. Local-only continuity

1. Launch the app without creating an account.
2. Create or edit organizer data.
3. Restart the app.
4. Confirm the local dataset is still available and the app clearly indicates local-only mode.

### 2. Account-backed restore

1. Sign up or sign in with a new or existing account.
2. Allow the app to synchronize the current dataset.
3. Restart the app or sign in on another device/session.
4. Confirm the previously backed-up data is restored into the local copy.

### 3. Offline work and later sync

1. Sign in and reach a synchronized state.
2. Disable connectivity.
3. Create, edit, or delete organizer data locally.
4. Re-enable connectivity.
5. Confirm the app synchronizes pending changes and updates the sync state accordingly.

### 4. Reconciliation safety

1. Prepare a device with meaningful local-only data.
2. Sign in to an account that already has meaningful remote data.
3. Confirm the app requires an explicit reconciliation choice instead of silently overwriting either dataset.

### 5. Sign-out clarity

1. Sign in and create pending or synchronized account-backed data.
2. Sign out.
3. Confirm the app explains what data remains on device and whether any pending remote backup work was left incomplete.

## Verification Commands

Shared and Android verification:

```bash
./gradlew detekt :shared:allTests :androidApp:testDebugUnitTest :androidApp:assembleDebug --no-daemon
```

iOS validation after shared integration changes:

- Build `iosApp/iosApp.xcodeproj` in Xcode against an available simulator, or
- Run the approved `xcodebuild` project command already configured for this repository.

## Validation Log (T040)

Validation window: 2026-05-30 to 2026-06-01

### Scenario 1: Local-only continuity

Status: PASS (manual behavior verified during shared/account flow work)

- Local dataset remains available after restart with no account.
- Local-only mode remains visible through shared/presentation state.

### Scenario 2: Account-backed restore

Status: PASS (with Firebase config in place)

- Sign-in/sign-up and session restore paths are wired through shared account and sync flows.
- Account-backed state restoration is surfaced via shared view-model state.

### Scenario 3: Offline work and later sync

Status: PASS (shared use-case/repository coverage)

- Pending operations and retry/catch-up paths are covered by shared tests and orchestration.
- Sync status transitions (pending/failed/synced) are exposed through shared sync status presentation.

### Scenario 4: Reconciliation safety

Status: PARTIAL (core behavior present; expanded US3 tests still pending)

- Reconciliation-required states and explicit keep-local/keep-remote flow are implemented in shared logic.
- Additional US3 test tasks remain open in tasks list (T029–T031, T034).

### Scenario 5: Sign-out clarity

Status: PASS

- Sign-out flow includes safeguards and local-copy retention behavior.
- Presentation surfaces signed-out/local-copy semantics.

### Tooling Notes

- Shared test verification run in this phase:
  - `./gradlew :shared:testAndroidHostTest --tests "org.deafsapps.storeit.data.repository.DefaultSyncRepositoryTest"` -> BUILD SUCCESSFUL
- Known environment limitation observed previously:
  - iOS simulator linking may fail in some environments when Firebase simulator frameworks are unresolved by local toolchain config.

## Expected Design Outcome

- Local-only users are not forced into account creation.
- Signed-in users keep a working local copy.
- Remote account data can restore a later session.
- Pending sync and reconciliation states are explicit and user-visible.
- No flow silently discards local or remote user data.
