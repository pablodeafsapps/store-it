# Tasks: Remote Account Sync And Backup

**Input**: Design documents from `specs/005-remote-sync-auth/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included by design. This repo requires a TDD-first loop for new features, and the plan explicitly calls for shared automated coverage of sync rules, reconciliation, and session handling.

**Organization**: Tasks are grouped by user story for independent implementation and testing. Paths are kept under `shared/` unless a thin platform adapter is required for secure storage or Firebase SDK differences.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1, US2, US3 (user story from spec.md)
- Include exact file paths in descriptions

## Path Conventions

- **Shared common code**: `shared/src/commonMain/kotlin/org/deafsapps/storeit/`
- **Shared SQLDelight**: `shared/src/commonMain/sqldelight/org/deafsapps/storeit/data/database/`
- **Thin platform adapters**: `shared/src/androidMain/kotlin/org/deafsapps/storeit/`, `shared/src/iosMain/kotlin/org/deafsapps/storeit/`
- **Shared tests**: `shared/src/commonTest/kotlin/org/deafsapps/storeit/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare shared dependencies and dependency injection for account-backed sync work.

- [X] T001 Update remote sync dependencies and source set wiring in `shared/build.gradle.kts`
- [ ] T002 [P] Reserve shared DI entry points for auth and sync components in `shared/src/commonMain/kotlin/org/deafsapps/storeit/di/AppModule.kt`
- [ ] T003 [P] Add secure-session and remote-provider abstraction shells in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/auth/RemoteAuthDataSource.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core sync metadata, contracts, and storage infrastructure that MUST be complete before any user story can be implemented.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T004 Extend local persistence for account sessions, dataset state, and sync queue metadata in `shared/src/commonMain/sqldelight/org/deafsapps/storeit/data/database/StoreItDatabase.sq`
- [ ] T005 [P] Create shared account and sync domain models in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/model/AccountSyncModels.kt`
- [ ] T006 [P] Define account, sync, reconciliation, and session-store contracts in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/repository/AccountSyncRepositories.kt`
- [ ] T007 [P] Add SQLDelight-backed sync metadata data sources in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/datasource/SqlDelightSyncMetadataDataSource.kt`
- [ ] T008 Implement shared sync orchestration primitives and error mapping in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/sync/SyncCoordinator.kt`
- [ ] T009 Create Android secure session storage actual in `shared/src/androidMain/kotlin/org/deafsapps/storeit/data/auth/SessionCredentialStore.android.kt`
- [ ] T010 Create iOS secure session storage actual in `shared/src/iosMain/kotlin/org/deafsapps/storeit/data/auth/SessionCredentialStore.ios.kt`

**Checkpoint**: Foundation ready. User story work can begin.

---

## Phase 3: User Story 1 - Back Up With An Account (Priority: P1) 🎯 MVP

**Goal**: Let users sign up or sign in, restore their backed-up dataset into the local store, and keep remote data authoritative after successful sync.

**Independent Test**: A user can sign up or sign in, modify organizer data, relaunch the app, and see the same account-backed data restored into the local copy.

### Tests for User Story 1

- [ ] T011 [P] [US1] Add sign-up, sign-in, and session-restore use case coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/domain/usecase/AuthenticateAccountUseCaseTest.kt`
- [ ] T012 [P] [US1] Add first-sync restore and remote-authority repository coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/data/repository/AccountSyncRepositoryTest.kt`
- [ ] T013 [P] [US1] Add account presentation state coverage for authenticated restore in `shared/src/commonTest/kotlin/org/deafsapps/storeit/presentation/account/AccountViewModelTest.kt`

### Implementation for User Story 1

- [ ] T014 [P] [US1] Implement Firebase-backed account authentication repository in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/FirebaseAccountRepository.kt`
- [ ] T015 [P] [US1] Implement remote dataset snapshot fetch and apply pipeline in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/FirebaseRemoteAccountDataSource.kt`
- [ ] T016 [US1] Implement sign-up, sign-in, and session-restore use cases in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/AuthenticateAccountUseCases.kt`
- [ ] T017 [US1] Implement account-backed synchronization bootstrap and restore flow in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/sync/AccountRestoreCoordinator.kt`
- [ ] T018 [US1] Expose account mode and restore state through shared presentation models in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/account/model/AccountUiState.kt`
- [ ] T019 [US1] Implement shared account entry and restore view model in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/account/viewmodel/AccountViewModel.kt`

**Checkpoint**: User Story 1 should be fully functional and independently testable.

---

## Phase 4: User Story 2 - Keep Working Offline (Priority: P2)

**Goal**: Preserve local-first editing for signed-in users, queue offline changes, and synchronize them once connectivity returns.

**Independent Test**: A signed-in user can browse and edit while offline, reopen with pending changes intact, and later synchronize those changes when connectivity returns.

### Tests for User Story 2

- [ ] T020 [P] [US2] Add pending-operation queue and retry coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/data/sync/SyncCoordinatorTest.kt`
- [ ] T021 [P] [US2] Add offline mutation persistence coverage for organizer repositories in `shared/src/commonTest/kotlin/org/deafsapps/storeit/data/repository/OfflineMutationQueueTest.kt`
- [ ] T022 [P] [US2] Add sync-status presentation coverage for pending and failed states in `shared/src/commonTest/kotlin/org/deafsapps/storeit/presentation/sync/SyncStatusViewModelTest.kt`

### Implementation for User Story 2

- [ ] T023 [P] [US2] Record create, update, and delete sync operations from local organizer writes in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/sync/SyncOperationRecorder.kt`
- [ ] T024 [P] [US2] Implement pending upload, retry, and failure persistence rules in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/DefaultSyncRepository.kt`
- [ ] T025 [US2] Integrate sync operation recording into rack, slot, and item repositories in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/SyncAwareOrganizerRepositories.kt`
- [ ] T026 [US2] Implement signed-in sync trigger use cases for retry and background catch-up in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/SyncAccountDataUseCases.kt`
- [ ] T027 [US2] Expose user-visible sync status and recoverable failure messaging in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/sync/model/SyncStatusUiState.kt`
- [ ] T028 [US2] Implement shared sync status view model for pending, failed, and synchronized states in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/sync/viewmodel/SyncStatusViewModel.kt`

**Checkpoint**: User Stories 1 and 2 should both work independently.

---

## Phase 5: User Story 3 - Stay Local By Choice (Priority: P3)

**Goal**: Preserve local-only mode, let users attach existing local data to an account later, and make sign-out plus reconciliation safe and explicit.

**Independent Test**: A local-only user can keep using the app without an account, later connect an account without silent data loss, and sign out while still understanding what local data remains.

### Tests for User Story 3

- [ ] T029 [P] [US3] Add reconciliation decision and conflict-summary coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/domain/usecase/ReconcileDatasetsUseCaseTest.kt`
- [ ] T030 [P] [US3] Add local-only mode and sign-out data-retention coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/data/repository/LocalModeAccountRepositoryTest.kt`
- [ ] T031 [P] [US3] Add local-only and reconciliation presentation coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/presentation/account/LocalModeViewModelTest.kt`

### Implementation for User Story 3

- [ ] T032 [P] [US3] Implement reconciliation detection and summary generation in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/DefaultReconciliationRepository.kt`
- [ ] T033 [P] [US3] Implement reconciliation decision use cases for keep-local, keep-remote, and merge flows in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/ReconcileDatasetsUseCases.kt`
- [ ] T034 [US3] Implement local-only to account-backed migration flow in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/sync/LocalToAccountMigrationCoordinator.kt`
- [ ] T035 [US3] Implement sign-out safeguards for pending changes and local data retention in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/SignOutAccountUseCase.kt`
- [ ] T036 [US3] Expose local-only, reconciliation-required, and signed-out-with-local-copy state in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/account/model/LocalModeUiState.kt`
- [ ] T037 [US3] Implement shared local-mode and reconciliation view model in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/account/viewmodel/LocalModeViewModel.kt`

**Checkpoint**: All user stories should now be independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finish observability, docs, and acceptance validation across the whole feature.

- [ ] T038 [P] Add sync telemetry and success-metric instrumentation points in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/sync/SyncTelemetry.kt`
- [ ] T039 [P] Update feature and developer documentation in `specs/005-remote-sync-auth/quickstart.md`
- [ ] T040 Run quickstart validation scenarios and record final implementation notes in `specs/005-remote-sync-auth/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies. Start immediately.
- **Phase 2 (Foundational)**: Depends on Phase 1. Blocks all user stories.
- **Phase 3 (US1)**: Depends on Phase 2. This is the MVP slice.
- **Phase 4 (US2)**: Depends on Phase 2 and builds on the signed-in state delivered by US1.
- **Phase 5 (US3)**: Depends on Phase 2 and integrates with the account and sync primitives from US1 and US2.
- **Phase 6 (Polish)**: Depends on the user stories you intend to ship.

### User Story Dependencies

- **US1 (P1)**: Can start immediately after the foundational phase.
- **US2 (P2)**: Requires the signed-in account flow from US1 to exist, but remains independently testable once that slice is present.
- **US3 (P3)**: Requires the shared account and sync primitives, but remains independently testable as a local-only plus reconciliation slice.

### Within Each User Story

- Tests must be written and fail before implementation.
- Data sources and repositories come before coordinating use cases.
- Use cases come before presentation models and view models.
- Story-specific checkpoints should be validated before moving to the next priority.

### Parallel Opportunities

- T002 and T003 can run in parallel.
- T005, T006, and T007 can run in parallel after T004 is scoped.
- T011, T012, and T013 can run in parallel within US1.
- T014 and T015 can run in parallel within US1.
- T020, T021, and T022 can run in parallel within US2.
- T023 and T024 can run in parallel within US2.
- T029, T030, and T031 can run in parallel within US3.
- T032 and T033 can run in parallel within US3.
- T038 and T039 can run in parallel during polish.

---

## Parallel Example: User Story 1

```bash
# Launch the US1 test tasks together:
Task: "Add sign-up, sign-in, and session-restore use case coverage in shared/src/commonTest/kotlin/org/deafsapps/storeit/domain/usecase/AuthenticateAccountUseCaseTest.kt"
Task: "Add first-sync restore and remote-authority repository coverage in shared/src/commonTest/kotlin/org/deafsapps/storeit/data/repository/AccountSyncRepositoryTest.kt"
Task: "Add account presentation state coverage for authenticated restore in shared/src/commonTest/kotlin/org/deafsapps/storeit/presentation/account/AccountViewModelTest.kt"

# Then parallelize the first repository implementations:
Task: "Implement Firebase-backed account authentication repository in shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/FirebaseAccountRepository.kt"
Task: "Implement remote dataset snapshot fetch and apply pipeline in shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/FirebaseRemoteAccountDataSource.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Validate sign-up, sign-in, restore, and post-relaunch continuity.
5. Demo the account-backed backup flow before expanding scope.

### Incremental Delivery

1. Setup + Foundational -> shared auth and sync infrastructure ready.
2. Add US1 -> validate account backup and restore.
3. Add US2 -> validate offline work and retry synchronization.
4. Add US3 -> validate local-only continuity, reconciliation, and safe sign-out.
5. Finish polish -> run quickstart scenarios and capture final notes.

### Single-Engineer Delivery Strategy

1. Treat each phase as roughly one weekly tranche at 15-20 hours.
2. Finish all test tasks before the corresponding implementation tasks in that tranche.
3. Keep WIP low: at most one story in flight at a time after the foundation.

---

## Notes

- All tasks follow the shared-first KMP architecture from the feature plan and repo instructions.
- `androidMain` and `iosMain` are reserved for secure storage or provider bridging only.
- The MVP scope is Phase 1 + Phase 2 + US1.
