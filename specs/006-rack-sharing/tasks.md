# Tasks: Shared Rack Collaboration

**Input**: Design documents from `specs/006-rack-sharing/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included by design. This repo requires a TDD-first loop for new features, and the plan explicitly calls for automated shared coverage of permission policy, membership filtering, rack list composition, revocation handling, and collaboration attribution.

**Organization**: Tasks are grouped by user story for independent implementation and testing. Paths are kept under `shared/` only, following the planning constraint for this feature.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1, US2, US3 (user story from spec.md)
- Include exact file paths in descriptions

## Path Conventions

- **Shared common code**: `shared/src/commonMain/kotlin/org/deafsapps/storeit/`
- **Shared SQLDelight**: `shared/src/commonMain/sqldelight/org/deafsapps/storeit/data/database/`
- **Shared tests**: `shared/src/commonTest/kotlin/org/deafsapps/storeit/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare shared contracts and persistence entry points for role-based rack sharing.

- [X] T001 Update collaboration dependencies and generated-source wiring if needed in `shared/build.gradle.kts`
- [ ] T002 [P] Reserve DI bindings for rack access, membership, role policy, and collaboration activity components in `shared/src/commonMain/kotlin/org/deafsapps/storeit/di/AppModule.kt`
- [ ] T003 [P] Add shared datasource contract shells for rack membership and collaboration activity in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/datasource/RackSharingDataSources.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain, storage, and policy primitives that MUST be complete before any user story can be implemented.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T004 Extend rack-sharing persistence for memberships, role changes, access status, and collaboration activity in `shared/src/commonMain/sqldelight/org/deafsapps/storeit/data/database/StoreItDatabase.sq`
- [ ] T005 [P] Create shared rack access domain models for roles, memberships, accessible rack summaries, and collaboration activity in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/model/RackAccessModels.kt`
- [ ] T006 [P] Define rack access, membership, permission-policy, and collaboration repository contracts in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/repository/RackCollaborationRepositories.kt`
- [ ] T007 [P] Implement SQLDelight-backed membership and collaboration activity data sources in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/datasource/SqlDelightRackSharingDataSource.kt`
- [ ] T008 Implement shared role-to-permission policy resolution and access-denied error mapping in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/ResolveRackPermissionsUseCase.kt`

**Checkpoint**: Foundation ready. User story work can begin.

---

## Phase 3: User Story 1 - Access Shared Racks After Login (Priority: P1) 🎯 MVP

**Goal**: Let authenticated users browse both owned and invited racks in one shared flow with clear access context and effective role metadata.

**Independent Test**: A signed-in user who owns one rack and is invited to another can load the shared rack list and see both racks exactly once, with owned vs invited context and the invited role exposed in shared state.

### Tests for User Story 1

- [ ] T009 [P] [US1] Add accessible-rack query and ownership-vs-invited filtering coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/data/repository/RackAccessRepositoryTest.kt`
- [ ] T010 [P] [US1] Add accessible-rack list use case coverage for mixed owned and invited results in `shared/src/commonTest/kotlin/org/deafsapps/storeit/domain/usecase/GetAccessibleRacksUseCaseTest.kt`
- [ ] T011 [P] [US1] Add rack list presentation coverage for owned vs invited sections, empty state, and duplicate suppression in `shared/src/commonTest/kotlin/org/deafsapps/storeit/presentation/rack/RackListViewModelTest.kt`

### Implementation for User Story 1

- [ ] T012 [P] [US1] Implement Firebase-backed rack membership and access snapshot datasource in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/datasource/FirebaseRackSharingDataSource.kt`
- [ ] T013 [P] [US1] Implement shared rack access repository for owned-plus-invited projections in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/DefaultRackAccessRepository.kt`
- [ ] T014 [US1] Implement accessible-rack flow and single-rack access resolution use cases in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/GetAccessibleRacksUseCase.kt`
- [ ] T015 [US1] Extend rack summary presentation models with access kind, effective role, and owner context in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/rack/model/RackSummaryVo.kt` and `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/rack/model/RackListUiState.kt`
- [ ] T016 [US1] Update rack list mapping and shared view-model composition for owned and invited racks in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/mapper/PresentationVoMappers.kt` and `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/rack/viewmodel/RackListViewModel.kt`

**Checkpoint**: User Story 1 should be fully functional and independently testable.

---

## Phase 4: User Story 2 - Collaborate On Shared Rack Content (Priority: P2)

**Goal**: Enforce role-gated comment, suggestion, and direct-edit actions for shared racks while preserving attribution and a recoverable rack state.

**Independent Test**: A collaborator with restricted permissions can only perform the actions allowed by their role, and another authorised user can later see the resulting comment, suggestion, or content edit with actor attribution.

### Tests for User Story 2

- [ ] T017 [P] [US2] Add permission-policy coverage for owner, editor, commenter, and suggester actions in `shared/src/commonTest/kotlin/org/deafsapps/storeit/domain/usecase/ResolveRackPermissionsUseCaseTest.kt`
- [ ] T018 [P] [US2] Add collaboration repository coverage for comments, suggestions, attributed edits, and access denial in `shared/src/commonTest/kotlin/org/deafsapps/storeit/data/repository/RackCollaborationRepositoryTest.kt`
- [ ] T019 [P] [US2] Add rack detail presentation coverage for role-gated affordances and collaboration attribution in `shared/src/commonTest/kotlin/org/deafsapps/storeit/presentation/rack/RackDetailViewModelTest.kt`

### Implementation for User Story 2

- [ ] T020 [P] [US2] Implement shared collaboration repository for comments, suggestions, and attributed edit activity in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/DefaultRackCollaborationRepository.kt`
- [ ] T021 [P] [US2] Implement collaboration action use cases for comment, suggest, and role-checked edit flows in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/CollaborateOnRackUseCases.kt`
- [ ] T022 [US2] Extend rack detail state with effective role, permission flags, activity timeline, and access-denied feedback in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/rack/model/RackDetailUiState.kt`
- [ ] T023 [US2] Update rack detail view-model orchestration to consume permission policy and collaboration actions in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/rack/viewmodel/RackDetailViewModel.kt`
- [ ] T024 [US2] Preserve actor attribution and stale-state protection in rack/detail mapping logic in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/rack/mapper/RackMapper.kt` and `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/rack/model/RackActivityVo.kt`

**Checkpoint**: User Stories 1 and 2 should both work independently.

---

## Phase 5: User Story 3 - Control Rack Sharing (Priority: P3)

**Goal**: Let the rack owner manage collaborator invitations, revocations, and role changes in shared logic while preventing non-owners from managing access.

**Independent Test**: A rack owner can invite a collaborator, change that collaborator’s role, and revoke access; the affected user’s accessible rack list updates accordingly, and a non-owner cannot perform the same management actions.

### Tests for User Story 3

- [ ] T025 [P] [US3] Add membership upsert, duplicate-invitation idempotency, and role-change coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/data/repository/RackMembershipRepositoryTest.kt`
- [ ] T026 [P] [US3] Add invite, role-change, revoke, and non-owner rejection use case coverage in `shared/src/commonTest/kotlin/org/deafsapps/storeit/domain/usecase/ManageRackMembershipUseCasesTest.kt`
- [ ] T027 [P] [US3] Add shared access-removal presentation coverage for revoked, withdrawn, and deleted racks in `shared/src/commonTest/kotlin/org/deafsapps/storeit/presentation/rack/RackAccessLifecycleViewModelTest.kt`

### Implementation for User Story 3

- [ ] T028 [P] [US3] Implement shared rack membership repository for invite, role change, revoke, and access list observation in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/DefaultRackMembershipRepository.kt`
- [ ] T029 [P] [US3] Implement owner-only membership management use cases in `shared/src/commonMain/kotlin/org/deafsapps/storeit/domain/usecase/ManageRackMembershipUseCases.kt`
- [ ] T030 [US3] Extend rack detail access-management state and collaborator list projections in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/rack/model/RackDetailUiState.kt` and `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/rack/model/RackCollaboratorVo.kt`
- [ ] T031 [US3] Update shared view-model handling for invite, revoke, role change, and revoked-access transitions in `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/rack/viewmodel/RackDetailViewModel.kt` and `shared/src/commonMain/kotlin/org/deafsapps/storeit/presentation/rack/viewmodel/RackListViewModel.kt`

**Checkpoint**: All user stories should now be independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finish documentation, migration safety, and acceptance validation across the whole feature.

- [ ] T032 [P] Document role matrix, owner-only scope, and future-role extension assumptions in `specs/006-rack-sharing/research.md` and `specs/006-rack-sharing/quickstart.md`
- [ ] T033 [P] Add shared migration and backward-compatibility notes for pre-sharing rack records in `specs/006-rack-sharing/data-model.md`
- [ ] T034 Run quickstart validation scenarios and capture delivery notes in `specs/006-rack-sharing/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies. Start immediately.
- **Phase 2 (Foundational)**: Depends on Phase 1. Blocks all user stories.
- **Phase 3 (US1)**: Depends on Phase 2. This is the MVP slice.
- **Phase 4 (US2)**: Depends on Phase 2 and builds on the access resolution from US1.
- **Phase 5 (US3)**: Depends on Phase 2 and integrates with the permission and membership primitives from US1 and US2.
- **Phase 6 (Polish)**: Depends on the user stories you intend to ship.

### User Story Dependencies

- **US1 (P1)**: Can start immediately after the foundational phase.
- **US2 (P2)**: Requires the role and access primitives from Phase 2 plus rack visibility from US1.
- **US3 (P3)**: Requires the membership and permission foundations and updates the same shared rack access model delivered in US1.

### Within Each User Story

- Tests must be written and fail before implementation.
- Repository and datasource work comes before coordinating use cases.
- Use cases come before presentation models and view-model orchestration.
- Shared state changes must preserve immutable UI-state conventions.
- Owner-only management rules must be enforced in shared logic, not only in presentation affordances.

### Parallel Opportunities

- T002 and T003 can run in parallel.
- T005, T006, and T007 can run in parallel after T004 is scoped.
- T009, T010, and T011 can run in parallel within US1.
- T012 and T013 can run in parallel within US1.
- T017, T018, and T019 can run in parallel within US2.
- T020 and T021 can run in parallel within US2.
- T025, T026, and T027 can run in parallel within US3.
- T028 and T029 can run in parallel within US3.
- T032 and T033 can run in parallel during polish.

---

## Parallel Example: User Story 1

```bash
# Launch the US1 test tasks together:
Task: "Add accessible-rack query and ownership-vs-invited filtering coverage in shared/src/commonTest/kotlin/org/deafsapps/storeit/data/repository/RackAccessRepositoryTest.kt"
Task: "Add accessible-rack list use case coverage for mixed owned and invited results in shared/src/commonTest/kotlin/org/deafsapps/storeit/domain/usecase/GetAccessibleRacksUseCaseTest.kt"
Task: "Add rack list presentation coverage for owned vs invited sections in shared/src/commonTest/kotlin/org/deafsapps/storeit/presentation/rack/RackListViewModelTest.kt"

# Then parallelize the first implementation tasks:
Task: "Implement Firebase-backed rack membership and access snapshot datasource in shared/src/commonMain/kotlin/org/deafsapps/storeit/data/datasource/FirebaseRackSharingDataSource.kt"
Task: "Implement shared rack access repository for owned-plus-invited projections in shared/src/commonMain/kotlin/org/deafsapps/storeit/data/repository/DefaultRackAccessRepository.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Validate mixed owned/invited rack visibility and duplicate-free shared browse state.
5. Demo the shared rack access flow before expanding scope.

### Incremental Delivery

1. Setup + Foundational -> role, membership, and policy primitives ready.
2. Add US1 -> validate owned/invited rack visibility.
3. Add US2 -> validate role-gated collaboration and attribution.
4. Add US3 -> validate invite, role change, revoke, and stale-access removal.
5. Finish polish -> run quickstart scenarios and capture final notes.

### Single-Engineer Delivery Strategy

1. Treat the setup plus foundational work as the first weekly tranche.
2. Keep only one user story active at a time after the foundation.
3. Finish the story test tasks before the implementation tasks in the same tranche.
4. Use the polish phase only after all planned story slices are stable.

---

## Notes

- All tasks follow the shared-first KMP architecture from the feature plan and repo instructions.
- `shared/src/androidMain` and `shared/src/iosMain` remain reserved for narrow adapter work only if a later implementation step proves it necessary.
- The MVP scope is Phase 1 + Phase 2 + US1.
