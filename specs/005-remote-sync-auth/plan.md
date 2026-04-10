# Implementation Plan: Remote Account Sync And Backup

**Branch**: `005-remote-sync-auth` | **Date**: 2026-04-09 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/Users/pablo/gitroot/store-it/specs/005-remote-sync-auth/spec.md`

## Summary

Extend Store it! from device-only persistence to optional account-backed synchronization. Users may remain in local-only mode, or create/sign in to an account that backs up their racks, slots, and items to remote infrastructure. The design remains local-first: the device keeps a working copy for offline use, while synchronized account data becomes the long-term source of truth after successful sync. The feature will be implemented with shared orchestration, sync policy, repository contracts, and state handling in `shared/src/commonMain`, while limiting platform-specific code to thin adapters only where remote SDK or secure session APIs differ.

## Technical Context

**Language/Version**: Kotlin Multiplatform with Swift app integration; Kotlin current project baseline, Swift 5.x for iOS shell  
**Primary Dependencies**: Kotlin Multiplatform, kotlinx-coroutines, Koin annotations, SQLDelight, Kotlinx Serialization, Firebase Authentication, Firebase Cloud Firestore, Firebase Cloud Storage  
**Storage**: Local SQLDelight database plus remote account-backed dataset in Firebase; secure local session/token storage via platform-secure facilities behind shared abstractions  
**Testing**: `kotlin.test`, `kotlinx-coroutines-test`, shared repository/use-case tests in `shared/src/commonTest`, Android unit tests in `androidApp/src/test`, iOS smoke validation via existing app build path  
**Target Platform**: Android and iOS, with feature logic maximized in `shared/src/commonMain`  
**Project Type**: Mobile KMP application with one shared Kotlin module and thin platform shells  
**Performance Goals**: Sign-up or sign-in to synchronized state in under 3 minutes; restore account-backed data on reopen without manual re-entry; preserve offline usability for core organizer flows  
**Constraints**: Local-only mode must remain available; no silent data loss; remote-backed mode must remain usable offline through a local copy; shared-first implementation unless platform SDK differences force thin adapters  
**Scale/Scope**: Single active signed-in account per device, organizer dataset per account, initial support for one-device-at-a-time mental model with deterministic reconciliation when multi-device divergence happens

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. User Experience First**: Pass. The feature preserves the existing no-login flow, adds explicit sync status, and requires clear reconciliation and sign-out behavior so users are not surprised by where their data lives.
- **II. Full Documentation**: Pass. This plan generates research, data model, quickstart, and contracts for the remote sync capability and requires implementation docs to stay aligned.
- **III. Test Coverage**: Pass. Shared sync rules, repository behavior, reconciliation decisions, and local/remote state transitions will be covered with automated tests before implementation completion.
- **IV. Simplicity**: Pass with caution. The feature adds unavoidable complexity, but the design keeps it constrained by centering orchestration in shared code and limiting platform-specific code to provider adapters and secure session storage only.
- **V. Quality Gates & Compliance**: Pass. Delivery will continue to use Detekt, shared and Android tests, and iOS validation when shared integration changes.

No constitution violations require exceptions at planning time.

## Project Structure

### Documentation (this feature)

```text
specs/005-remote-sync-auth/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── account-sync-repositories.md
│   └── sync-state-contract.md
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
│   │   │   └── usecase/
│   │   ├── kotlin/org/deafsapps/storeit/data/
│   │   │   ├── datasource/
│   │   │   ├── repository/
│   │   │   ├── sync/
│   │   │   └── auth/
│   │   ├── kotlin/org/deafsapps/storeit/presentation/
│   │   │   ├── account/
│   │   │   └── sync/
│   │   └── sqldelight/org/deafsapps/storeit/data/database/
│   ├── androidMain/kotlin/org/deafsapps/storeit/
│   │   ├── auth/
│   │   └── sync/
│   ├── iosMain/kotlin/org/deafsapps/storeit/
│   │   ├── auth/
│   │   └── sync/
│   └── commonTest/kotlin/org/deafsapps/storeit/
│       ├── data/
│       ├── domain/
│       └── presentation/
└── build.gradle.kts

androidApp/
└── src/
    └── main/java/org/deafsapps/storeit/androidapp/

iosApp/
└── iosApp/
```

**Structure Decision**: The feature is designed shared-first. All business rules, reconciliation logic, sync orchestration, repository contracts, DTO mapping, and presentation state live in `shared/src/commonMain`. `shared/src/androidMain` and `shared/src/iosMain` are reserved for thin provider adapters only if Firebase Auth, Firestore, Storage, or secure credential APIs require platform-specific bridging. Existing platform UI shells may need follow-up view wiring, but the plan intentionally concentrates feature logic under `shared/`.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (none) | — | — |
