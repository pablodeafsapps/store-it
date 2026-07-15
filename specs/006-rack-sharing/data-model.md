# Data Model: Shared Rack Collaboration

**Feature**: 006-rack-sharing  
**Date**: 2026-07-14

## Entities

### Rack

- **Description**: The existing storage rack aggregate that remains the authoritative content container for shared collaboration.
- **Attributes**:
  - **id**: Stable rack identifier.
  - **ownerAccountId**: Account identifier of the rack owner.
  - **name**: Rack name visible in browse and detail flows.
  - **description**: User-maintained rack description.
  - **location**: Human-readable rack location.
  - **photoUri**: Optional rack photo reference.
  - **createdAt**: Rack creation timestamp.
  - **updatedAt**: Most recent rack-content update timestamp.
- **Validation**:
  - `id` and `ownerAccountId` must be non-empty.
  - A rack must have exactly one owner.
- **Relationships**:
  - Owns zero or more `RackMembership` records for non-owner participants.
  - Owns zero or more `CollaborationActivity` records.

### RackMembership

- **Description**: The collaboration link between a rack and a non-owner participant.
- **Attributes**:
  - **id**: Stable membership identifier.
  - **rackId**: Target rack identifier.
  - **accountId**: Participant account identifier.
  - **role**: Active `RackRole` assigned to that participant.
  - **status**: Invitation-pending, active, revoked, or withdrawn.
  - **invitedByAccountId**: Account that granted access.
  - **invitedAt**: Invitation timestamp.
  - **updatedAt**: Last membership or role-change timestamp.
- **Validation**:
  - One active membership per `(rackId, accountId)` pair.
  - `role` is required whenever status is active.
  - Owner account must not also appear as a non-owner active membership for the same rack.
- **Relationships**:
  - Belongs to one `Rack`.
  - Refers to one participant account.

### RackRole

- **Description**: The named collaboration role that limits what a rack participant may do.
- **Values**:
  - **OWNER**: Can manage sharing and perform all content actions.
  - **EDITOR**: Can edit rack content, comment, and suggest.
  - **COMMENTER**: Can comment and suggest but cannot directly edit rack content.
  - **SUGGESTER**: Can add suggestions only.
- **Validation**:
  - Every rack participant must resolve to exactly one effective role, either by ownership or active membership.
- **Relationships**:
  - Mapped to actions through `RackPermissionPolicy`.

### RackPermissionPolicy

- **Description**: Shared business policy that derives allowed actions from a participant’s effective rack role.
- **Attributes**:
  - **effectiveRole**: Resolved role for the active account on the rack.
  - **canManageAccess**: Whether the user may invite, revoke, or change collaborator access.
  - **canEditContent**: Whether the user may directly mutate rack content.
  - **canComment**: Whether the user may add comments.
  - **canSuggest**: Whether the user may add suggestions.
  - **canView**: Whether the user may open the rack.
- **Validation**:
  - `canView` is required for every active role.
  - `canManageAccess` is true only for `OWNER` in the first release.
- **Relationships**:
  - Derived from one `RackRole`.

### AccessibleRackSummary

- **Description**: Shared rack-browse projection combining ownership/access context with summary rack data.
- **Attributes**:
  - **rackId**: Rack identifier.
  - **name**: Rack name.
  - **location**: Rack location.
  - **accessKind**: Owned or invited.
  - **effectiveRole**: Owner role or collaborator role.
  - **ownerDisplayName**: Owner label when the rack is invited.
  - **updatedAt**: Last known content update timestamp.
- **Validation**:
  - `accessKind` and `effectiveRole` must always be present.
- **Relationships**:
  - Built from `Rack` plus either owner context or `RackMembership`.

### CollaborationActivity

- **Description**: Attributed collaboration record attached to a shared rack.
- **Attributes**:
  - **id**: Stable activity identifier.
  - **rackId**: Target rack identifier.
  - **actorAccountId**: Account that created the activity.
  - **activityType**: Suggestion, comment, or direct edit.
  - **targetEntityType**: Rack, shelf slot, item, or other rack-scoped content type.
  - **targetEntityId**: Identifier of the affected entity when applicable.
  - **message**: Free-form comment or suggestion text when applicable.
  - **createdAt**: Creation timestamp.
  - **supersededAt**: Optional timestamp when the activity is no longer the current suggestion state.
- **Validation**:
  - `actorAccountId`, `rackId`, and `activityType` are required.
  - Suggestions and comments require non-blank `message`.
- **Relationships**:
  - Belongs to one `Rack`.
  - Created by one account acting under one effective role.

### RackAccessRemoval

- **Description**: User-visible state describing why a previously accessible rack is no longer available.
- **Attributes**:
  - **rackId**: Rack identifier.
  - **reason**: Revoked, withdrawn, deleted, or unavailable.
  - **occurredAt**: Timestamp of removal detection.
  - **displayMessage**: User-facing explanation.
- **Validation**:
  - `reason` must be one of the supported removal categories.
- **Relationships**:
  - Refers to one previously accessible rack.

## Relationships Summary

- `Rack` 1:1 `ownerAccountId`
- `Rack` 1:N `RackMembership`
- `RackMembership` N:1 `Rack`
- `RackMembership` N:1 participant account
- `RackRole` 1:1 `RackPermissionPolicy` mapping at runtime
- `Rack` 1:N `CollaborationActivity`
- `AccessibleRackSummary` is a read projection derived from `Rack` and membership/owner context
- `RackAccessRemoval` is a transient or persisted presentation-support record derived from membership changes

## Validation Rules

- A signed-in user may access a rack only if they are the owner or have one active membership for that rack.
- Duplicate invitations for the same rack and user must resolve to a single membership identity rather than duplicate active rows.
- A role change updates the existing membership rather than creating a second active membership for the same rack and user.
- Revoked, withdrawn, or deleted rack access must remove the rack from future accessible-rack projections.
- Content mutations require an effective role whose permission policy allows direct editing.
- Comment creation requires an effective role whose permission policy allows comments.
- Suggestion creation requires an effective role whose permission policy allows suggestions.

## State Transitions

### Membership lifecycle

- No membership → Invitation-pending
- Invitation-pending → Active
- Active → Active with different role
- Active → Revoked
- Invitation-pending → Withdrawn

### Access lifecycle

- Not accessible → Accessible as owner
- Not accessible → Accessible as invited collaborator
- Accessible → Access removed

### Collaboration action lifecycle

- No activity → Suggestion created
- No activity → Comment created
- No activity → Direct edit applied
- Suggestion created → Suggestion superseded or resolved
- Active access → Action blocked when role no longer permits the attempted action

## Notes

- Existing `Rack`, `RackData`, slot, and item models should be extended only where ownership and attribution metadata are needed at shared boundaries.
- The first release models whole-rack collaboration, not per-item or per-slot access control.
- The role model is intentionally explicit and extensible so future roles can be added without reshaping membership identity.
