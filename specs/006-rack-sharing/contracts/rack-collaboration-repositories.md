# Contract: Rack Collaboration Repositories And Policies

**Feature**: 006-rack-sharing  
**Date**: 2026-07-14

This document defines the domain-facing contracts needed for rack sharing, rack membership, and role-based collaboration in shared code.

## RackAccessRepository

- **Purpose**: Expose the racks a signed-in user can access, whether through ownership or invitation.
- **Responsibilities**:
  - Observe the accessible rack set for the active account
  - Distinguish owned vs invited racks in shared projections
  - Resolve the active user’s effective role for a rack
  - Remove revoked or withdrawn access from future results
- **Output style**:
  - Read flows as `Flow<Result<DomainError, List<AccessibleRackSummary>>>`
  - Single-rack access resolution as suspend reads returning shared result type

## RackMembershipRepository

- **Purpose**: Manage collaborator invitations, activation, role changes, and revocation for a rack.
- **Responsibilities**:
  - Create or upsert a collaborator membership for a rack
  - Activate, revoke, or withdraw rack access
  - Change a collaborator’s role without changing rack ownership
  - Prevent duplicate active memberships for the same rack and user
  - Expose current collaborator list for owner-managed screens
- **Output style**:
  - Write operations as suspend functions returning shared result type
  - Membership lists and updates as `Flow` where ongoing observation is needed

## RackPermissionPolicyRepository

- **Purpose**: Resolve whether a user may perform a specific action on a specific rack according to their effective role.
- **Responsibilities**:
  - Resolve effective role from ownership or active membership
  - Map role to allowed actions
  - Provide a single shared source of truth for permission decisions
  - Surface access-denied results in domain terms
- **Output style**:
  - Pure reads or suspend reads returning shared result type or permission-policy value objects

## RackCollaborationRepository

- **Purpose**: Coordinate collaboration actions that are not ordinary rack persistence alone.
- **Responsibilities**:
  - Add comments to a shared rack
  - Add suggestions to a shared rack
  - Persist attributed collaboration activity
  - Enforce permission checks before collaboration activity is accepted
  - Surface collaboration history or summaries needed by shared presentation
- **Output style**:
  - Writes as suspend operations returning shared result type
  - Activity timelines or summaries as `Flow` or read operations

## RackRepository Integration Rule

- **Purpose**: Preserve the existing rack persistence contract while introducing role-aware gating.
- **Responsibilities**:
  - Direct rack save/delete flows remain centered on `RackRepository`
  - Shared use cases must consult effective permissions before allowing mutations
  - Rack ownership metadata must remain available where permission or list composition requires it

## Data Source Placement

- Collaboration and membership interfaces belong in `shared/src/commonMain`.
- Thin Firebase-backed adapters belong in `shared/src/commonMain` when GitLive APIs are already shared enough.
- If a provider edge requires target-specific code, only the narrow adapter belongs in `shared/src/androidMain` or `shared/src/iosMain`.

## Boundary Rules

- Domain and presentation code must depend on repository interfaces and permission-policy abstractions only.
- Role checks must not be duplicated ad hoc across view-models and repositories.
- Platform consumers must not own the role matrix or access rules.
- Ownership management remains a product rule enforced in shared code, not a UI-only convention.
