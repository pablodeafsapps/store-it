# Tasks: Store it! — Storage Rack Organiser & Item Locator

**Input**: Design documents from `specs/001-storage-rack-organiser/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Organization**: Tasks grouped by user story for independent implementation and testing. Paths follow KMP layout: `shared/`, `composeApp/`, `.github/workflows/`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1, US2, US3 (user story from spec.md)
- Include exact file paths in descriptions

## Path Conventions

- **Shared (KMP)**: `shared/src/commonMain/kotlin/org/deafsapps/mobile/storeit/` (domain, data, usecase)
- **Android app**: `composeApp/src/androidMain/kotlin/org/deafsapps/mobile/storeit/`
- **Tests**: `shared/src/commonTest/kotlin/...`, `composeApp/src/androidUnitTest/...`
- **CI**: `.github/workflows/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Linting and CI so all later work is gated.

- [X] T001 Configure Detekt for shared and composeApp in `shared/build.gradle.kts` and `composeApp/build.gradle.kts` (or root convention), add baseline if needed per research.md
- [ ] T002 Add GitHub Actions workflow for build and test in `.github/workflows/build-and-test.yml` (trigger on push/PR; run `./gradlew :shared:testDebugUnitTest :composeApp:assembleDebug` or equivalent)
- [ ] T003 [P] Add Detekt step to CI workflow in `.github/workflows/build-and-test.yml` (optional, run `./gradlew detekt`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain and data layer that ALL user stories depend on. No user story work until this phase is complete.

- [ ] T004 [P] Create Rack, ShelfSlot, Item domain entities in `shared/src/commonMain/kotlin/org/deafsapps/mobile/storeit/domain/` per data-model.md (id, name, description, location, photoUri, etc.)
- [ ] T005 [P] Define RackRepository and ItemRepository interfaces in `shared/src/commonMain/kotlin/org/deafsapps/mobile/storeit/domain/` per contracts/repository-interfaces.md (getAllRacks, getRackById, saveRack, deleteRack; getItemsBySlot, searchItems, saveItem, deleteItem; use Either or Result for success/failure)
- [ ] T006 [P] Define domain error sealed type in `shared/src/commonMain/kotlin/org/deafsapps/mobile/storeit/domain/` for repository failures (e.g. ValidationError, NotFound)
- [ ] T007 Implement in-memory RackRepository in `shared/src/commonMain/kotlin/org/deafsapps/mobile/storeit/data/` (implements RackRepository; unit tests in `shared/src/commonTest/kotlin/org/deafsapps/mobile/storeit/data/`)
- [ ] T008 Implement in-memory ItemRepository (and slot/placement handling) in `shared/src/commonMain/kotlin/org/deafsapps/mobile/storeit/data/` (getItemsBySlot, searchItems by name/description; unit tests in `shared/src/commonTest/`)
- [ ] T009 Add mock data provider (1–5 records: at least one rack and several items) in `shared/src/commonMain/kotlin/org/deafsapps/mobile/storeit/data/` or platform debug source set, toggled/preloaded for debug builds only (FR-011)

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Register a storage rack (Priority: P1) — MVP

**Goal**: User can register a rack (photo + metadata), see rack list, open rack and see tappable image map for slots.

**Independent Test**: Create one rack with photo and name, see it in list, open it and see rack image with tappable regions.

- [ ] T010 [P] [US1] Implement GetRacksUseCase and SaveRackUseCase in `shared/src/commonMain/kotlin/org/deafsapps/mobile/storeit/domain/usecase/` (depend on RackRepository; return Either/Flow as per AGENTS.md)
- [ ] T011 [P] [US1] Unit tests for GetRacksUseCase and SaveRackUseCase in `shared/src/commonTest/kotlin/org/deafsapps/mobile/storeit/domain/usecase/`
- [ ] T012 [US1] Add rack screen (Android): capture or pick photo, name, description, location in `composeApp/src/androidMain/kotlin/org/deafsapps/mobile/storeit/ui/rack/AddRackScreen.kt` (or equivalent); wire to SaveRackUseCase
- [ ] T013 [US1] Rack list screen (Android): list all racks in `composeApp/.../ui/rack/RackListScreen.kt`; empty state when no racks; navigate to add rack and to rack detail
- [ ] T014 [US1] Rack detail screen (Android): show rack image as tappable map in `composeApp/.../ui/rack/RackDetailScreen.kt`; tap = define/select slot (store position or slot id for later use in US2); support edit metadata and remove rack (FR-002)

**Checkpoint**: User Story 1 is fully functional and testable independently

---

## Phase 4: User Story 2 — Add an item to a rack (Priority: P2)

**Goal**: User can add an item (photo + metadata) and place it on a chosen rack and slot; both flows (item-first and rack-first) supported.

**Independent Test**: Select a rack, tap a slot, add item with photo and name; item appears in that slot. Or add item first then choose rack/slot; same outcome.

- [ ] T015 [P] [US2] Implement AddItemUseCase and GetItemsBySlotUseCase in `shared/src/commonMain/kotlin/org/deafsapps/mobile/storeit/domain/usecase/` (depend on ItemRepository, RackRepository if needed)
- [ ] T016 [P] [US2] Unit tests for AddItemUseCase and GetItemsBySlotUseCase in `shared/src/commonTest/kotlin/org/deafsapps/mobile/storeit/domain/usecase/`
- [ ] T017 [US2] Add item flow (Android): screen for photo (camera/gallery), name, description, quantity, owner, tags in `composeApp/.../ui/item/AddItemScreen.kt`; then select rack and tap slot to place (or reverse: select rack + slot first then add item); wire to AddItemUseCase (FR-004, FR-005)
- [ ] T018 [US2] When user has no racks, guide to create one or disable add-item until at least one rack exists (edge case spec); handle empty slot as valid placement target in `composeApp/.../ui/`

**Checkpoint**: User Stories 1 and 2 both work independently

---

## Phase 5: User Story 3 — Locate an item and view or edit it (Priority: P3)

**Goal**: User can open a rack, tap a slot, see items in that slot; tap item to view/edit; global search by name and description with navigation to item (rack/slot).

**Independent Test**: Open rack → tap slot with items → see list → tap item → view/edit. Use search box → get results → tap result → navigate to item.

- [ ] T019 [P] [US3] Implement SearchItemsUseCase and GetItemByIdUseCase in `shared/src/commonMain/kotlin/org/deafsapps/mobile/storeit/domain/usecase/` (search by name and description; return items with rack/slot info per FR-008)
- [ ] T020 [P] [US3] Unit tests for SearchItemsUseCase and GetItemByIdUseCase in `shared/src/commonTest/kotlin/org/deafsapps/mobile/storeit/domain/usecase/`
- [ ] T021 [US3] Slot items list (Android): from rack detail, tap slot → show list of items in that slot in `composeApp/.../ui/item/SlotItemsScreen.kt`; empty state and option to add item (FR-006)
- [ ] T022 [US3] Item detail and edit (Android): tap item → view all fields; edit name, description, quantity, owner, tags (and photo if required) in `composeApp/.../ui/item/ItemDetailScreen.kt` (FR-007)
- [ ] T023 [US3] Global search (Android): search box always available; search items by name and description; results show item and rack/slot; tap result navigates to item in `composeApp/.../ui/search/SearchScreen.kt` or equivalent (FR-008); no-results state (edge case)

**Checkpoint**: All user stories are independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Backend placeholder, documentation, and quality gates.

- [ ] T024 [P] Introduce RemoteRackSource and RemoteItemSource interfaces (or equivalent) in `shared/src/commonMain/kotlin/org/deafsapps/mobile/storeit/data/` with no-op/stub implementations; document where Firebase will plug in (research.md, contracts/)
- [ ] T025 [P] Add KDoc to public APIs (domain entities, repository interfaces, use cases) in `shared/src/commonMain/kotlin/org/deafsapps/mobile/storeit/`; update README and `specs/001-storage-rack-organiser/quickstart.md` with build/run/test instructions
- [ ] T026 Run quickstart.md validation: build Android and shared tests, confirm mock data visible in debug build

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — BLOCKS all user stories
- **Phase 3 (US1)**: Depends on Phase 2 — MVP
- **Phase 4 (US2)**: Depends on Phase 2 (and US1 for UI flow: add item needs rack list/detail)
- **Phase 5 (US3)**: Depends on Phase 2 (and US1/US2 for full locate flow)
- **Phase 6 (Polish)**: Depends on Phases 3–5 being complete (or in progress for T024/T025)

### User Story Dependencies

- **US1 (P1)**: After Foundational only — no dependency on US2/US3
- **US2 (P2)**: After Foundational; UI may reuse rack list/detail from US1 for “select rack and slot”
- **US3 (P3)**: After Foundational; reuses rack detail (tap slot) and item list/detail

### Within Each User Story

- Use cases and tests (T010–T011, T015–T016, T019–T020) can be implemented in parallel where marked [P]
- UI tasks (T012–T014, T017–T018, T021–T023) depend on use cases being available

### Parallel Opportunities

- T004, T005, T006 can run in parallel (Phase 2)
- T007, T008 can run in parallel after T004–T006
- T010 and T011 (US1); T015 and T016 (US2); T019 and T020 (US3) are parallel within their phase
- T024 and T025 (Polish) are parallel

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 (Setup)
2. Complete Phase 2 (Foundational)
3. Complete Phase 3 (US1)
4. **STOP and VALIDATE**: Create one rack, see list, open and tap image
5. Demo/deploy if ready

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 → test independently → MVP (rack registration + tappable map)
3. US2 → test independently → add item to rack/slot
4. US3 → test independently → locate item, search
5. Polish → Firebase placeholder, docs, quickstart check

---

## Notes

- [P] = parallelizable; [US1/US2/US3] = story label for traceability
- Paths use package `org.deafsapps.storeit`; adjust if your repo uses a different package
- Constitution: all code documented and covered by tests; PRs verify before merge
