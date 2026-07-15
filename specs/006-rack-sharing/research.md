# Research: Shared Rack Collaboration

**Feature**: 006-rack-sharing  
**Date**: 2026-07-14

## 1. Shared-only planning boundary

- **Decision**: Keep the implementation plan and future task paths focused on `shared/`, even though Android and iOS consumer shells still exist in the repository.
- **Rationale**: The user explicitly requested post-AGP-9 planning that uses `shared/` paths only. This still aligns with the repo’s shared-first architecture because the core behavior for permissions, rack visibility, and collaboration belongs in common code.
- **Alternatives considered**:
  - Plan platform wiring in `androidApp/` and `iosApp/` immediately: rejected because it violates the requested planning boundary.
  - Treat the feature as platform-specific UI work first: rejected because the business rules are cross-platform.

## 2. Rack role model

- **Decision**: Introduce an explicit `RackRole` model per rack membership, with the first release supporting `OWNER`, `EDITOR`, `COMMENTER`, and `SUGGESTER`.
- **Rationale**: The user explicitly requires a role for each user on each rack and notes that the role may change in the future. A first-class role model satisfies that requirement and makes permission changes representable without mutating rack ownership or rewriting rack content records.
- **Alternatives considered**:
  - Boolean owner/collaborator flag only: rejected because it cannot represent future role changes cleanly.
  - Store direct per-action permissions only: rejected because it is less understandable for product flows and harder to evolve consistently.

## 3. Permission policy design

- **Decision**: Separate role identity from allowed actions through a shared permission policy layer. The policy derives allowed actions such as manage access, edit rack content, comment, and suggest from the active `RackRole`.
- **Rationale**: Roles may evolve over time. Centralizing the mapping in shared policy code keeps the persistence model stable while letting product rules change with minimal churn.
- **Alternatives considered**:
  - Scatter `if role == ...` checks across view-models and repositories: rejected because it invites inconsistent enforcement.
  - Persist a full permission matrix with every membership: rejected because it complicates storage and migration for no current benefit.

## 4. Membership persistence model

- **Decision**: Persist rack ownership separately from rack membership entries. Each membership ties a `rackId` and `userId` to one active role and status, while ownership remains a property of the rack aggregate.
- **Rationale**: Ownership and collaboration are different concepts. Keeping them separate makes revocation, role change, invitation idempotency, and owner-only management rules simpler to express and test.
- **Alternatives considered**:
  - Store collaborators as a loose list inside the rack record only: rejected because role changes and invitation lifecycle become difficult to model cleanly.
  - Treat the owner as just another collaborator row without ownership distinction: rejected because the spec reserves access management to the owner.

## 5. Rack list composition

- **Decision**: Build the authenticated rack list in shared presentation from a unified access view that can distinguish `owned` and `invited` sections while still exposing one browse flow.
- **Rationale**: The feature requires users to see owned and invited racks together after login, but the distinction must remain clear. A shared composition model keeps the behavior consistent across platforms.
- **Alternatives considered**:
  - Separate screens for owned and shared racks only: rejected because it weakens discoverability.
  - Flatten all racks with no ownership distinction: rejected because it hides important context from users.

## 6. Collaboration activity shape

- **Decision**: Model comments, suggestions, and direct edits as attributed collaboration activity linked to a rack and actor. Direct edits still update the authoritative rack data, while comments and suggestions remain explicit activity records.
- **Rationale**: The spec requires attribution and multiple contribution modes. Treating all three as first-class collaboration activity keeps author visibility and reviewability consistent.
- **Alternatives considered**:
  - Persist only final rack state with no activity attribution: rejected because it fails the attribution requirement.
  - Treat suggestions as ordinary comments only: rejected because suggestions are a named product action in scope.

## 7. Concurrent update policy

- **Decision**: Reuse the project’s existing remote-sync mental model and apply deterministic conflict handling for shared rack content, with last-confirmed-write-wins at the content record level plus retained activity attribution.
- **Rationale**: The feature needs a single recoverable authoritative rack state, but full real-time merge semantics would add disproportionate complexity for the first release.
- **Alternatives considered**:
  - Full CRDT or operational-transform collaboration: rejected as unnecessary for current scope.
  - Manual conflict prompts for every concurrent edit: rejected because it would degrade normal collaboration flow.

## 8. Enforcement boundaries

- **Decision**: Enforce access and action limits in shared domain/use-case and repository orchestration, not only in presentation state.
- **Rationale**: UI affordances should reflect permissions, but they must not be the only guardrail. Shared enforcement keeps Android and iOS behavior aligned and prevents accidental bypass through alternate entry points.
- **Alternatives considered**:
  - UI-only gating: rejected because it is insufficient for security and consistency.
  - Remote-provider-only authorization: rejected because local shared state still needs deterministic behavior before and after remote responses.

## 9. Testing strategy

- **Decision**: Concentrate tests in `shared/src/commonTest` for role policy mapping, access filtering, rack list composition, invitation idempotency, revocation handling, and collaboration activity attribution.
- **Rationale**: The feature’s core risk is in shared business rules, not platform-specific rendering. Shared tests best match the repo architecture and the user’s requested path constraints.
- **Alternatives considered**:
  - Primarily UI/instrumentation coverage: rejected because it would test consumers instead of the shared logic where most defects would originate.

## 10. Contract surface

- **Decision**: Document two contract groups for Phase 1: repository/domain contracts for collaboration and shared UI-state contracts for owned/shared rack browsing plus role-gated actions.
- **Rationale**: This feature changes both data boundaries and user-visible shared state. Capturing both keeps implementation and later tasks aligned.
- **Alternatives considered**:
  - Repository contract only: rejected because it would leave shared presentation expectations implicit.
  - UI contract only: rejected because repository responsibilities would be under-specified.
