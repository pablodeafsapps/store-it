# Quickstart: Shared Rack Collaboration

**Branch**: `006-rack-sharing`  
**Date**: 2026-07-14

## Objective

Validate that authenticated users can browse both owned and invited racks, that each rack participant has a role limiting allowed actions, and that collaboration activity remains attributed and access-controlled through shared logic.

## Prerequisites

- JDK 17+
- Android SDK
- Xcode and iOS toolchain for downstream consumer validation when shared integration changes are exercised
- Existing remote account sync/auth flow already configured for development builds
- Shared Firebase-backed data flow working for rack persistence

## Shared-Only Planning Scope

For this feature, plan and later tasks should prefer paths under `shared/` for:

- rack role and membership domain models
- permission-policy and access-resolution use cases
- collaboration repositories and data sources
- rack list and rack detail shared presentation state
- collaboration activity mapping and attribution
- unit tests in `shared/src/commonTest`

Existing `androidApp/` and `iosApp/` consumers may later need wiring, but they are intentionally outside this plan’s file-target scope.

## Validation Scenarios

### 1. Owned and invited rack visibility

1. Sign in as a user who owns at least one rack and has at least one active invitation.
2. Load the authenticated rack browse flow.
3. Confirm that both owned and invited racks appear within the same shared browse experience.
4. Confirm that owned and invited racks remain distinguishable by access context.

### 2. Role-gated collaboration

1. Assign a collaborator role that permits suggestions but not direct edits.
2. Open the shared rack as that user.
3. Confirm that the shared state allows suggestions and blocks unsupported actions.
4. Change the collaborator role to one with broader permissions.
5. Confirm the available actions update accordingly.

### 3. Owner-managed access lifecycle

1. Sign in as a rack owner.
2. Invite a collaborator to a rack and assign an initial role.
3. Change that collaborator’s role.
4. Revoke the collaborator’s access.
5. Confirm that shared state for the invitee no longer exposes the rack as accessible.

### 4. Collaboration attribution

1. Perform a comment, suggestion, and direct edit on a shared rack as different authorised users.
2. Load the rack detail state for another authorised user.
3. Confirm that each visible activity carries actor attribution and the correct activity type.

### 5. Concurrent change safety

1. Have two authorised users update the same rack content in separate sessions.
2. Allow synchronization to complete according to the shared collaboration policy.
3. Confirm that the resulting rack state is recoverable, deterministic, and not duplicated in the accessible-rack list.

## Verification Commands

Shared and Android verification:

```bash
./gradlew detekt :shared:allTests :androidApp:testDebugUnitTest :androidApp:assembleDebug --no-daemon
```

iOS validation after shared integration changes:

- Build `iosApp/iosApp.xcodeproj` in Xcode against an available simulator, or
- Run the approved `xcodebuild` project command already configured for this repository.

## Expected Design Outcome

- Every accessible rack resolves to one effective role for the active user.
- Shared browse state can distinguish owned vs invited racks without duplicating rack entries.
- Action availability is derived from a central shared permission policy.
- Revoked or stale access is removed cleanly from shared state.
- Collaboration activity remains attributed and consistent with the final rack state.
