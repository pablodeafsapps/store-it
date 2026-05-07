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

**Purpose**: Prepare shared dependencies and dependency injection for email/password account sync with photo backup.

- [X] T001 Update remote sync dependencies and source set wiring in `shared/build.gradle.kts`
- [X] T002 [P] Reserve shared DI entry points for email/password auth, photo backup, and sync components in `shared/src/commonMain/kotlin/org/deafsapps/storeit/di/AppModule.kt`
- [X] T003 [P] Add secure-session and remote-provider abstraction shells for email/password auth and dataset/photo sync in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/datasource/AuthRemoteDataSource.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core sync metadata, contracts, and storage infrastructure that MUST be complete before any user story can be implemented.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 Extend local persistence for account sessions, dataset state, photo sync scope, and sync queue metadata in `shared/src/commonMain/sqldelight/org/deafsapps/storeit/data/database/StoreItDatabase.sq`
- [X] T005 [P] Create shared account and sync domain models for email/password auth, restore-pending state, and signed-out-with-local-copy mode in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/model/AccountSyncModels.kt`
- [X] T006 [P] Define account, sync, reconciliation, photo-backup, and session-store contracts in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/repository/AccountSyncRepositories.kt`
- [X] T007 [P] Add SQLDelight-backed sync metadata data sources in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/datasource/SqlDelightSyncMetadataDataSource.kt`
- [X] T008 Implement shared sync orchestration primitives and error mapping for synchronized, pending-upload, pending-restore, failed, and reconciliation-required states in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/GetSyncStageUseCase.kt`
- [X] T009 Create Android secure session storage actual in `shared/src/androidMain/kotlin/org/deafsapps/storeit/data/datasource/SessionCredentialAndroidDataSource.kt`
- [X] T010 Create iOS Keychain-backed secure session storage adapter in `shared/src/iosMain/kotlin/org/deafsapps/storeit/data/datasource/SessionCredentialIosDataSource.kt`

**Checkpoint**: Foundation ready. User story work can begin.

---

## Phase 3: User Story 1 - Back Up With An Account (Priority: P1) 🎯 MVP

**Goal**: Let users sign up or sign in with email/password, restore their backed-up dataset and photos into the local store, and keep remote data authoritative after successful sync.

**Independent Test**: A user can sign up or sign in, modify organizer data, relaunch the app, and see the same account-backed data restored into the local copy.

### Tests for User Story 1

- [X] T011 [P] [US1] Add email/password sign-up, sign-in, and session-restore use case coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/domain/usecase/AuthenticateAccountUseCaseTest.kt`
- [X] T012 [P] [US1] Add account repository restore coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/data/repository/AccountSyncRepositoryTest.kt` and first-sync dataset-plus-photo restore plus remote-authority use-case coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/domain/usecase/RestoreAccountDataUseCaseTest.kt`
- [X] T013 [P] [US1] Add account presentation state coverage for authenticated restore and restore-pending status in `shared/src/commonTest/kotlin/org/deafsapps/storeit/presentation/account/AccountViewModelTest.kt`

### Implementation for User Story 1

- [X] T014 [P] [US1] Implement Firebase-backed email/password account authentication repository in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/FirebaseAccountRepository.kt`
- [X] T015 [P] [US1] Implement remote dataset and photo snapshot fetch/apply pipeline in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/FirebaseRemoteAccountDataSource.kt`
- [X] T016 [US1] Implement email/password sign-up, sign-in, and session-restore use cases in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/AuthenticateAccountUseCases.kt`
- [X] T017 [US1] Implement account-backed synchronization bootstrap and restore flow with restore-pending handling in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/RestoreAccountDataUseCase.kt`
- [X] T017A [US1] Implement account restore gateway adapters and refactor restore orchestration in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/gateway/FeatureAccountRestoreGateways.kt` and `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/FirebaseAccountDataRestoreRepository.kt`
- [X] T018 [US1] Expose account mode, synchronized, and restore-pending state through shared presentation models in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/account/model/AccountUiState.kt`
- [X] T019 [US1] Implement shared account entry and restore view model for email/password auth and restore retry in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/account/viewmodel/AccountViewModel.kt`
- [X] T019A [US1] Implement Android and iOS account sign-in/sign-up UI flow in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/account/`, `androidApp/src/main/java/org/deafsapps/storeit/androidapp/presentation/account/ui/`, `androidApp/src/main/java/org/deafsapps/storeit/androidapp/`, `iosApp/iosApp/Presentation/Account/Views/`, `iosApp/iosApp/Presentation/AppRoute.swift`, and `iosApp/iosApp/Presentation/ContentView.swift`

**Checkpoint**: User Story 1 should be fully functional and independently testable.

---

## Phase 4: User Story 2 - Keep Working Offline (Priority: P2)

**Goal**: Preserve local-first editing for signed-in users, queue offline changes, and synchronize them once connectivity returns.

**Independent Test**: A signed-in user can browse and edit while offline, reopen with pending changes intact, and later synchronize those changes when connectivity returns.

### Tests for User Story 2

- [X] T020 [P] [US2] Add pending-operation queue, restore retry, and sync retry coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/domain/usecase/SyncAccountDataUseCasesTest.kt`
- [ ] T021 [P] [US2] Add offline mutation persistence coverage for organizer repositories in `shared/src/commonTest/kotlin/org/deafsapps/storeit/data/repository/OfflineMutationQueueTest.kt`
- [ ] T022 [P] [US2] Add sync-status presentation coverage for pending and failed states in `shared/src/commonTest/kotlin/org/deafsapps/storeit/presentation/sync/SyncStatusViewModelTest.kt`

### Implementation for User Story 2

- [ ] T023 [P] [US2] Record create, update, and delete sync operations from local organizer writes in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/sync/SyncOperationRecorder.kt`
- [ ] T024 [P] [US2] Implement pending upload, pending restore, retry, and failure persistence rules in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/DefaultSyncRepository.kt`
- [ ] T025 [US2] Integrate sync operation recording into rack, slot, and item repositories in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/SyncAwareOrganizerRepositories.kt`
- [ ] T026 [US2] Implement signed-in sync trigger use cases for retry and background catch-up in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/SyncAccountDataUseCases.kt`
- [ ] T027 [US2] Expose user-visible sync status and recoverable failure messaging for pending upload, pending restore, failed, and synchronized states in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/sync/model/SyncStatusUiState.kt`
- [ ] T028 [US2] Implement shared sync status view model for pending upload, pending restore, failed, and synchronized states in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/sync/viewmodel/SyncStatusViewModel.kt`

**Checkpoint**: User Stories 1 and 2 should both work independently.

---

## Phase 5: User Story 3 - Stay Local By Choice (Priority: P3)

**Goal**: Preserve local-only mode, let users attach existing local data to an account later, and make sign-out plus reconciliation safe and explicit with only keep-local or keep-remote outcomes.

**Independent Test**: A local-only user can keep using the app without an account, later connect an account without silent data loss, and sign out while still understanding what local data remains.

### Tests for User Story 3

- [ ] T029 [P] [US3] Add keep-local vs keep-remote reconciliation decision and conflict-summary coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/domain/usecase/ReconcileDatasetsUseCaseTest.kt`
- [ ] T030 [P] [US3] Add local-only mode and sign-out-to-local-only data-retention coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/data/repository/LocalModeAccountRepositoryTest.kt`
- [ ] T031 [P] [US3] Add local-only, signed-out-with-local-copy, and reconciliation presentation coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/presentation/account/LocalModeViewModelTest.kt`

### Implementation for User Story 3

- [ ] T032 [P] [US3] Implement reconciliation detection and summary generation for explicit keep-local vs keep-remote choice in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/DefaultReconciliationRepository.kt`
- [ ] T033 [P] [US3] Implement reconciliation decision use cases for keep-local and keep-remote flows in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/ReconcileDatasetsUseCases.kt`
- [ ] T034 [US3] Implement local-only to account-backed migration flow for empty-remote upload and non-empty-remote reconciliation in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/sync/LocalToAccountMigrationCoordinator.kt`
- [ ] T035 [US3] Implement sign-out safeguards for pending changes and transition into local-only retained data in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/SignOutAccountUseCase.kt`
- [ ] T036 [US3] Expose local-only, reconciliation-required, and signed-out-with-local-copy state in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/account/model/LocalModeUiState.kt`
- [ ] T037 [US3] Implement shared local-mode and reconciliation view model for keep-local vs keep-remote flows in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/account/viewmodel/LocalModeViewModel.kt`

**Checkpoint**: All user stories should now be independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finish observability, docs, privacy/security references, and acceptance validation across the whole feature.

- [ ] T038 [P] Add sync telemetry and success-metric instrumentation points in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/sync/SyncTelemetry.kt`
- [ ] T039 [P] Update feature documentation and add the planned privacy/security document reference in `specs/005-remote-sync-auth/quickstart.md`
- [ ] T040 Run quickstart validation scenarios and record final implementation notes in `specs/005-remote-sync-auth/quickstart.md`
- [X] T041 Add a visible signed-in account status header with avatar/initial and restore-complete state on the account screen in `androidApp/src/main/java/org/deafsapps/storeit/androidapp/presentation/account/ui/AccountScreen.kt`
- [X] T042 Add shared sign-out support and a main rack-list app-bar account badge that reflects backup state in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/account/viewmodel/AccountViewModel.kt` and `androidApp/src/main/java/org/deafsapps/storeit/androidapp/presentation/rack/ui/RackListScreen.kt`

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
- **US3 (P3)**: Requires the shared account and sync primitives, but remains independently testable as a local-only plus explicit keep-local/keep-remote reconciliation slice.

### Within Each User Story

- Tests must be written and fail before implementation.
- Data sources and repositories come before coordinating use cases.
- Use cases come before presentation models and view models.
- Reconciliation tasks must preserve the v1 constraint that merge is out of scope.
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
4. Validate email/password sign-up, sign-in, restore, photo recovery, and post-relaunch continuity.
5. Demo the account-backed backup flow before expanding scope.

### Incremental Delivery

1. Setup + Foundational -> shared auth and sync infrastructure ready.
2. Add US1 -> validate account backup and restore.
3. Add US2 -> validate offline work, pending restore, and retry synchronization.
4. Add US3 -> validate local-only continuity, explicit keep-local/keep-remote reconciliation, and safe sign-out into local-only mode.
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
