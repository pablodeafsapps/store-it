# Implementation Plan: Shared Rack Collaboration

**Branch**: `006-rack-sharing` | **Date**: 2026-07-14 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/Users/pablo/gitroot/store-it/specs/006-rack-sharing/spec.md`

## Summary

Add role-based rack sharing so authenticated users can see both owned racks and invited racks after login, while shared business logic enforces which collaboration actions are allowed per rack role. The design extends the existing account-backed Firebase and SQLDelight architecture by introducing rack memberships, explicit rack roles, role-to-permission policy checks, collaboration activity attribution, and shared presentation models for combined rack browsing and access-controlled rack detail flows. Per the requested planning constraint, implementation slices and task paths for this feature are planned under `shared/` only, with existing Android and iOS shells treated as consumers of the new shared contracts rather than primary implementation surfaces.

## Technical Context

**Language/Version**: Kotlin Multiplatform project baseline with Swift 5.x iOS shell  
**Primary Dependencies**: Kotlin Multiplatform, kotlinx-coroutines, Koin annotations, SQLDelight, Kotlinx Serialization, kotlinx.collections.immutable, GitLive Firebase Authentication, GitLive Firebase Cloud Firestore, GitLive Firebase Cloud Storage  
**Storage**: Local SQLDelight organizer dataset plus remote Firebase-backed account dataset extended with rack membership, rack role, and collaboration-activity metadata; secure authenticated session storage remains behind shared abstractions  
**Testing**: `kotlin.test`, `kotlinx-coroutines-test`, shared domain/data/presentation tests in `shared/src/commonTest`, plus repository and permission-policy contract coverage aligned with existing Gradle verification  
**Target Platform**: Android and iOS consuming shared KMP presentation/domain logic  
**Project Type**: Mobile Kotlin Multiplatform application with shared-first business logic and thin platform consumers  
**Performance Goals**: Users with active invitations should see accessible racks in under 30 seconds after reaching authenticated home state; action-permission checks should resolve as part of normal shared state composition without introducing extra user-visible steps; authorised edits should become visible to later readers without manual recovery  
**Constraints**: Plans and tasks must target paths under `shared/` only; every rack membership must carry an explicit role; role changes must be representable without reshaping the storage model; owner-only access management remains in scope for the first release; permission enforcement must live in shared code; no silent access leakage or silent loss of confirmed rack changes; Android and iOS shells remain thin consumers of shared state  
**Scale/Scope**: One rack owner and multiple collaborators per rack; first release supports explicit roles `OWNER`, `EDITOR`, `COMMENTER`, and `SUGGESTER`; collaboration applies at whole-rack scope; concurrent updates use deterministic shared policy instead of live co-editing semantics

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. User Experience First**: Pass. The plan keeps owned and invited racks in one shared browsing model, makes access level explicit through role-derived affordances, and defines clear outcomes for revoked or stale access so users are not left in ambiguous states.
- The role model is future-proofed in shared logic, but the first release stays intentionally small by keeping sharing management owner-only and whole-rack scoped.
- **II. Full Documentation**: Pass. This plan generates explicit research, data-model, quickstart, and contracts artifacts for role-based rack collaboration and requires contract documentation for repositories and shared presentation states.
- **III. Test Coverage**: Pass. Permission policy, membership filtering, rack list composition, access revocation handling, and collaboration attribution are planned for automated shared tests before implementation is considered complete.
- **IV. Simplicity**: Pass with caution. Roles add complexity, but the design contains it by separating membership and permission policy from rack content and by avoiding granular per-item ACLs or real-time merge semantics in the first release.
- **V. Quality Gates & Compliance**: Pass. The feature remains aligned with shared-first architecture, explicit documentation, deterministic tests, and the repository verification workflow already required by the project.

No constitution violations require exceptions at planning time.

## Project Structure

### Documentation (this feature)

```text
specs/006-rack-sharing/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── rack-collaboration-repositories.md
│   └── rack-role-ui-contract.md
└── tasks.md
```

### Source Code (repository root)

```text
shared/
├── src/
│   ├── commonMain/
│   │   ├── kotlin/org/deafsapps/storeit/domain/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── gateway/
│   │   │   └── usecase/
│   │   ├── kotlin/org/deafsapps/storeit/data/
│   │   │   ├── datasource/
│   │   │   ├── repository/
│   │   │   └── gateway/
│   │   ├── kotlin/org/deafsapps/storeit/presentation/
│   │   │   ├── rack/model/
│   │   │   ├── rack/viewmodel/
│   │   │   └── mapper/
│   │   └── sqldelight/org/deafsapps/storeit/data/database/
│   ├── commonTest/kotlin/org/deafsapps/storeit/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│   ├── androidMain/kotlin/org/deafsapps/storeit/
│   │   └── data/datasource/
│   └── iosMain/kotlin/org/deafsapps/storeit/
│       └── data/datasource/
└── build.gradle.kts
```

**Structure Decision**: This plan intentionally constrains implementation and later tasks to `shared/` paths only, per the user’s instruction. Shared code owns the role model, membership persistence contracts, permission policy, collaboration orchestration, and rack-browsing presentation state. `shared/src/androidMain` and `shared/src/iosMain` remain available only for thin provider adapters if Firebase or secure-session boundaries require target-specific code, but no feature slice is planned in `androidApp/` or `iosApp/`.

## Alignment Notes

- The feature extends the current remote-account architecture rather than creating a second collaboration backend.
- Existing rack content models remain the source for rack data; sharing is layered on through membership and permission metadata.
- Each rack participant must have exactly one active role for a given rack at a time.
- Roles may change over time without creating a new membership identity for the same user and rack.
- The first release keeps sharing management with the owner while allowing role-gated content participation for invited users.
- Deterministic conflict handling is required for concurrent updates, but full live collaborative editing and arbitrary merge tooling remain out of scope.
- Tasks derived from this plan should avoid `androidApp/` and `iosApp/` file targets unless the user later broadens scope.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (none) | — | — |
