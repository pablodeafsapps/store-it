# Contract: Shared Rack Role UI State

**Feature**: 006-rack-sharing  
**Date**: 2026-07-14

This document defines the shared presentation contract for showing owned/shared racks and role-gated collaboration actions.

## Rack List State Contract

- **Purpose**: Present one authenticated browsing experience that includes both owned and invited racks.
- **Required state fields**:
  - List of accessible rack summaries
  - Ownership/access classification for each rack
  - Effective role label for each invited rack
  - Loading state
  - Empty state when no owned or invited racks are available
  - Error state when rack access cannot be loaded
- **Behavioral expectations**:
  - Owned and invited racks must be visually distinguishable in state, even if rendered in one list or grouped sections.
  - A rack must appear at most once per user in the accessible-rack state.
  - A rack removed by revocation, withdrawal, or deletion must disappear from subsequent steady-state lists and may surface one user-facing removal message.

## Rack Detail Permission Contract

- **Purpose**: Ensure shared rack detail state exposes what actions the active user is allowed to take.
- **Required state fields**:
  - Effective role for the active user
  - Permission flags for view, suggest, comment, edit, and manage access
  - User-facing access-denied message when an attempted action is not permitted
  - Attribution data for visible comments, suggestions, and recent edits
- **Behavioral expectations**:
  - Shared state must disable or hide unsupported actions consistently with the shared permission policy.
  - Shared state must not advertise access-management actions to non-owner roles in the first release.
  - Shared state must remain renderable when access is revoked while the rack detail is already open.

## Collaboration Activity Contract

- **Purpose**: Present comments, suggestions, and direct-edit attribution in a consistent shared shape.
- **Required state fields**:
  - Activity identifier
  - Actor display label
  - Activity type
  - Timestamp
  - Free-form message when applicable
  - Optional target entity reference when the activity affects a specific slot or item
- **Behavioral expectations**:
  - Suggestions and comments remain distinct activity types in presentation state.
  - Direct edits must retain actor attribution even when the rack content itself is updated.

## Access Removal Contract

- **Purpose**: Surface clear shared state when a previously accessible rack is no longer available.
- **Required state fields**:
  - Removal reason
  - User-facing explanation
  - Recovery action or dismissal path if applicable
- **Behavioral expectations**:
  - Revoked access must block further edit/comment/suggest actions immediately in shared state.
  - Stale cached content must not be presented as active access after removal is detected.

## Boundary Rules

- Shared presentation models remain platform-agnostic and should use immutable state/value objects.
- Platform UI layers consume these contracts and do not redefine permission semantics.
- Navigation side effects may differ by platform, but state meaning must remain the same across Android and iOS.
