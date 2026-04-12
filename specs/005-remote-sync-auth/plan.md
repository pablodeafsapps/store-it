# Implementation Plan: Remote Account Sync And Backup

**Branch**: `005-remote-sync-auth` | **Date**: 2026-04-09 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/Users/pablo/gitroot/store-it/specs/005-remote-sync-auth/spec.md`

## Summary

Extend Store it! from device-only persistence to optional account-backed synchronization. Users may remain in local-only mode, or create/sign in with email and password to an account that backs up their racks, slots, items, and supported photos to remote infrastructure. The design remains local-first: the device keeps a working copy for offline use, while synchronized account data becomes the long-term recovery source after successful sync. The first release supports explicit reconciliation by choosing either keep-local or keep-remote, keeps the last readable local dataset on sign-out by switching it to local-only mode, and implements shared orchestration, sync policy, repository contracts, and state handling in `shared/src/commonMain`, while limiting platform-specific code to thin adapters only where remote SDK or secure session APIs differ.

## Technical Context

**Language/Version**: Kotlin Multiplatform with Swift app integration; Kotlin current project baseline, Swift 5.x for iOS shell  
**Primary Dependencies**: Kotlin Multiplatform, kotlinx-coroutines, Koin annotations, SQLDelight, Kotlinx Serialization, Firebase Authentication, Firebase Cloud Firestore, Firebase Cloud Storage  
**Storage**: Local SQLDelight database plus remote account-backed dataset in Firebase; rack and item photos are part of the backed-up dataset; secure local session/token storage via platform-secure facilities behind shared abstractions  
**Testing**: `kotlin.test`, `kotlinx-coroutines-test`, shared repository/use-case tests in `shared/src/commonTest`, Android unit tests in `androidApp/src/test`, iOS smoke validation via existing app build path  
**Target Platform**: Android and iOS, with feature logic maximized in `shared/src/commonMain`  
**Project Type**: Mobile KMP application with one shared Kotlin module and thin platform shells  
**Performance Goals**: Sign-up or sign-in to synchronized state in under 3 minutes; restore account-backed data on reopen without manual re-entry; preserve offline usability for core organizer flows; treat sync-status visibility as best-effort for MVP rather than a hard timing SLA  
**Constraints**: Local-only mode must remain available; no silent data loss; remote-backed mode must remain usable offline through a local copy; only explicit keep-local or keep-remote reconciliation outcomes are supported in the first release; sign-out must preserve the last readable local dataset by switching it into local-only mode; shared-first implementation unless platform SDK differences force thin adapters  
**Scale/Scope**: Single active signed-in account per device, organizer dataset per account, initial support for one-device-at-a-time mental model with deterministic reconciliation when multi-device divergence happens; manual validation plus later analytics/telemetry will be used to assess MVP success criteria

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. User Experience First**: Pass. The feature preserves the existing no-login flow, adds explicit sync status, and requires clear reconciliation and sign-out behavior so users are not surprised by where their data lives.
- User-visible states now explicitly include local-only, synchronized, pending sync, restore pending, failed with retry, reconciliation required, and signed-out-with-local-copy.
- **II. Full Documentation**: Pass. This plan generates research, data model, quickstart, and contracts for the remote sync capability and requires implementation docs to stay aligned.
- Privacy, data-retention, and account-recovery wording are intentionally delegated to a separate security/privacy document referenced by the spec.
- **III. Test Coverage**: Pass. Shared sync rules, repository behavior, reconciliation decisions, and local/remote state transitions will be covered with automated tests before implementation completion.
- **IV. Simplicity**: Pass with caution. The feature adds unavoidable complexity, but the design keeps it constrained by centering orchestration in shared code and limiting platform-specific code to provider adapters and secure session storage only.
- The first release avoids a general merge path during reconciliation in order to keep behavior deterministic and testable.
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

**Structure Decision**: The feature is designed shared-first. All business rules, reconciliation logic, sync orchestration, repository contracts, DTO mapping, and presentation state live in `shared/src/commonMain`. `shared/src/androidMain` and `shared/src/iosMain` are reserved for thin provider adapters only if Firebase Auth, Firestore, Storage, or secure credential APIs require platform-specific bridging. The shared implementation must cover email/password auth, explicit keep-local vs keep-remote reconciliation, photo backup, restore-pending states, and sign-out transitions into local-only mode. Existing platform UI shells may need follow-up view wiring, but the plan intentionally concentrates feature logic under `shared/`.

## Alignment Notes

- Authentication is limited to email plus password in the first release.
- Photos already supported by the organizer domain are part of the mandatory backed-up dataset for MVP.
- Reconciliation in the first release supports only explicit `keep local` and `keep remote` outcomes; merge remains out of scope.
- On sign-out, the app keeps the last readable local dataset on the device and switches it into local-only mode.
- Sync-state visibility is best-effort for MVP, with no hard latency guarantee in the plan.
- Privacy, data-retention, and account-recovery wording are intentionally externalized to a separate security/privacy document that should be referenced later.
- Success-criteria validation is expected to be mixed: manual during MVP acceptance, with telemetry or analytics added later where available.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (none) | — | — |
