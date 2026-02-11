# Implementation Plan: Store it! — Storage Rack Organiser & Item Locator

**Branch**: `001-storage-rack-organiser` | **Date**: 2025-02-10 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `specs/001-storage-rack-organiser/spec.md`

## Summary

Build the Store it! MVP: a KMP (Android + iOS) organiser/locator for belongings. Users register storage racks (photo + metadata), use the rack image as a tappable map to place items on shelf slots, add items (photo + metadata), and locate items via slot drill-down or global search. Persistence is progressive: in-memory first, then local storage, then remote (Firebase placeholder). Mock data (1–5 records) for debugging and UI assessment. No login in MVP. Delivered using Clean Architecture (`.ai/AGENTS.md`), conventions (`.ai/CONVENTIONS.md`), Detekt, and GitHub Actions CI (build + test).

## Technical Context

**Language/Version**: Kotlin (latest stable for KMP), Swift 5.x for iOS app layer  
**Primary Dependencies**: Kotlin Multiplatform, Jetpack Compose (Android), Compose Multiplatform / SwiftUI (iOS), kotlinx-coroutines, kotlinx-serialization; backend placeholder for Firebase (flexible for future change)  
**Storage**: In-memory for first iteration; then local (e.g. SQLDelight or Realm KMP); then remote via Firebase (placeholder: interface/abstraction only, no implementation in MVP)  
**Testing**: JUnit 5, kotlinx-coroutines-test, MockK (or Mockito-Kotlin); Compose UI tests; XCTest for iOS where needed  
**Target Platform**: Android (minSdk 24+), iOS (latest stable); shared logic in `shared`  
**Project Type**: Mobile (KMP) — existing structure: `shared/`, `composeApp/`, `iosApp/`  
**Performance Goals**: First rack in &lt;2 min, add item in &lt;3 min, locate via search in &lt;30 s (per SC-001–SC-003)  
**Constraints**: No login in MVP; UX easy-to-use; code documented and tested (constitution)  
**Scale/Scope**: Single user, local-first; 1–5 mock records; progressive persistence  
**Task list scope**: The tasks in tasks.md target Android first (composeApp); iOS (iosApp) is a later increment.

**Linting & CI**: Detekt for Kotlin; GitHub Actions for build and test (and optionally lint). Firebase: placeholders only; keep backend abstraction flexible.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. User Experience First**: Spec and flows (register rack → add item → locate) designed for clarity and no training; mock data supports UI assessment.
- **II. Full Documentation**: Plan and implementation will require KDoc, README, and spec/plan alignment; documented in DoD.
- **III. Test Coverage**: Unit tests for domain/data; UI/integration where critical; no merge without tests (per workflow).
- **IV. Simplicity**: YAGNI; in-memory then local then remote; Firebase behind abstraction.
- **V. Quality Gates & Compliance**: PRs verify docs + tests; constitution check in plan.

No violations. Complexity Tracking table left empty.

## Project Structure

### Documentation (this feature)

```text
specs/001-storage-rack-organiser/
├── plan.md              # This file
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/           # Phase 1 (repository contracts; backend placeholder)
└── tasks.md             # Phase 2 (/speckit.tasks)
```

### Source Code (repository root)

```text
shared/                     # KMP shared module (domain + data + optional shared VM)
├── src/
│   ├── commonMain/kotlin/  # Domain entities, use cases, repo interfaces, DTOs
│   ├── androidMain/kotlin/ # actuals, Android data sources
│   ├── iosMain/kotlin/     # actuals, iOS data sources
│   └── commonTest/         # Shared unit tests
└── build.gradle.kts

composeApp/                  # Android app (Compose UI)
├── src/
│   ├── androidMain/        # Activities, Compose screens, platform DI
│   └── androidUnitTest/
└── build.gradle.kts

iosApp/                      # iOS app (SwiftUI)
├── iosApp/                  # SwiftUI views, iOS entry, KMP framework consumption
└── iosApp.xcodeproj/

.github/workflows/            # CI: build, test, (Detekt)
```

**Structure Decision**: KMP mobile layout per `.ai/AGENTS.md`. Domain and data in `shared`; UI in `composeApp` (Android) and `iosApp` (iOS). Backend (Firebase) abstracted behind interfaces; placeholders for future setup.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (none) | — | — |
