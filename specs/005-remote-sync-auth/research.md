# Research: Remote Account Sync And Backup

**Feature**: 005-remote-sync-auth  
**Date**: 2026-04-09

## 1. Remote provider strategy

- **Decision**: Use Firebase as the first remote infrastructure, specifically Firebase Authentication for account identity, Cloud Firestore for structured rack/slot/item metadata, and Cloud Storage for rack and item images.
- **Rationale**: The project already anticipated Firebase-backed persistence, and this combination matches the feature scope: user identity, structured synchronized data, and image backup.
- **Alternatives considered**:
  - Custom backend from day one: rejected because it adds infrastructure scope without additional product value for this feature.
  - Remote database without dedicated auth: rejected because account identity is part of the feature itself.
  - Authentication only, no remote data source: rejected because it would not satisfy remote backup and restore.

## 2. Shared-first architecture

- **Decision**: Keep synchronization policy, reconciliation rules, repository interfaces, sync state modelling, and use cases in `shared/src/commonMain`. Use platform-specific code only for provider SDK bridges and secure session/token storage when the APIs are not meaningfully identical across Android and iOS.
- **Rationale**: This aligns with `AGENTS.md` and `.ai/CONVENTIONS.md`: maximize shared code, keep platform layers thin, and preserve a single business policy for both mobile targets.
- **Alternatives considered**:
  - Separate Android and iOS sync implementations: rejected because it duplicates business rules and increases reconciliation risk.
  - Full `expect`/`actual` sync implementation surface in common code: rejected because only the provider bridge should differ, not the business flow.

## 3. Local-first synchronization model

- **Decision**: Continue using SQLDelight as the on-device working store. Signed-in users operate on the local database first; synchronization reads and writes between the local database and the remote dataset. Local-only users continue using the same local store without an account.
- **Rationale**: The feature explicitly requires local usability and later synchronization. Reusing the existing local database avoids a second local representation and keeps offline behavior deterministic.
- **Alternatives considered**:
  - Remote-first reads with local cache only: rejected because it weakens offline guarantees and increases UX fragility.
  - Separate storage engines for local-only and signed-in users: rejected because it complicates migration from local-only mode to account-backed mode.

## 4. Source-of-truth and sync semantics

- **Decision**: For signed-in users, the remote dataset is the long-term source of truth after successful synchronization, but the local database remains the working copy. Sync state explicitly tracks whether local data is fully synchronized, has pending uploads, requires download, or needs reconciliation.
- **Rationale**: This resolves the product requirement that remote data remain authoritative while preserving fast offline work on the device.
- **Alternatives considered**:
  - Pure local authority with best-effort remote backup: rejected because it makes cross-device restore ambiguous.
  - Pure remote authority at all times: rejected because it breaks offline-first behavior.

## 5. Reconciliation policy

- **Decision**: Introduce an explicit reconciliation decision whenever non-empty local data and non-empty remote data both exist and cannot be assumed identical. The user must confirm the chosen outcome rather than relying on silent merge or overwrite.
- **Rationale**: The feature spec forbids silent data loss. Explicit user-controlled reconciliation is safer than inferred behavior when local-only data meets previously backed-up account data.
- **Alternatives considered**:
  - Always merge automatically: rejected because conflicting edits and duplicate entities can create surprising results.
  - Always replace local with remote: rejected because it can silently destroy unsynced local-only data.
  - Always upload local over remote: rejected because it can silently destroy already backed-up remote data.

## 6. Conflict resolution within synchronized accounts

- **Decision**: Model record-level sync metadata so deterministic conflict handling is possible. Default to last-confirmed-write-wins at the record level for the first implementation, while surfacing sync and reconciliation problems when records cannot be safely reconciled.
- **Rationale**: The feature needs a consistent rule for multi-device divergence without expanding into a full collaborative editing model.
- **Alternatives considered**:
  - Full operational transform or CRDT-style merge: rejected as unnecessary complexity for a single-user organizer.
  - Manual review for every conflict: rejected because it would degrade normal sync usability.

## 7. Authentication surface

- **Decision**: Plan for sign-up, sign-in, sign-out, and persisted authenticated session restoration only. Password recovery, account deletion, and profile editing remain outside this feature unless required to complete the basic lifecycle.
- **Rationale**: This keeps the feature bounded around backup and restore instead of turning it into a complete account-management initiative.
- **Alternatives considered**:
  - Full account-management suite now: rejected because it dilutes the feature scope.

## 8. Session and credential handling

- **Decision**: Store only the minimum session material required for authenticated restore, and keep it behind shared interfaces with platform-secure implementations in `androidMain` and `iosMain`.
- **Rationale**: The constitution requires platform best practices for sensitive data and forbids secrets in unencrypted storage.
- **Alternatives considered**:
  - Store credentials directly in the SQLDelight database: rejected for security reasons.
  - Require full sign-in on every launch: rejected because it degrades restore continuity and user experience.

## 9. Testing strategy

- **Decision**: Cover the feature primarily with shared tests for use cases, reconciliation, sync state transitions, and repository semantics. Add thin platform tests only where secure session storage or Firebase SDK bridging behavior differs.
- **Rationale**: Shared-first logic belongs in `commonTest`, matching repo conventions and minimizing duplicated test suites.
- **Alternatives considered**:
  - Platform-heavy integration testing only: rejected because it would make core sync rules harder to reason about and slower to validate.

## 10. UI integration scope

- **Decision**: Plan and tasks will treat UI wiring as thin shells over shared presentation state. Most feature design remains under `shared/`; platform UI updates are only adapters for account entry, sync status, and reconciliation prompts.
- **Rationale**: The user explicitly asked for shared-only planning unless the provider APIs differ, and the architecture already expects thin platform shells.
- **Alternatives considered**:
  - Put account flows directly into platform-specific UI logic first: rejected because the feature’s state machine is business logic, not purely view logic.
