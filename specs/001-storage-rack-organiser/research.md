# Research: Store it! — Storage Rack Organiser

**Feature**: 001-storage-rack-organiser  
**Date**: 2025-02-10

## 1. Kotlin Multiplatform (KMP) stack

- **Decision**: Use existing KMP layout: `shared` (commonMain, androidMain, iosMain), `shared` (Android), `iosApp` (SwiftUI). Follow `.ai/AGENTS.md` and `.ai/CONVENTIONS.md`.
- **Rationale**: Project already uses KMP; spec targets Android and iOS with shared business logic; Clean Architecture and sum types (Either/Result) keep domain testable and backend-agnostic.
- **Alternatives considered**: Native-only Android or iOS — rejected; single codebase for domain/data reduces duplication and aligns with prompt.

## 2. Backend / persistence strategy

- **Decision**: (1) In-memory only for first deliverable; (2) Local persistence next (KMP-capable DB e.g. SQLDelight or Realm); (3) Remote via abstraction only — Firebase placeholder (interfaces / modules) so backend can be swapped later.
- **Rationale**: Spec requires progressive persistence; prompt asks for Firebase “placeholders” and “keep things as flexible as possible.”
- **Alternatives considered**: Firebase from day one — rejected for MVP scope; direct remote-only — rejected to allow offline-first and UI assessment with mock data.

## 3. Linting (Detekt)

- **Decision**: Add Detekt for Kotlin with a baseline and rules aligned with `.ai/CONVENTIONS.md` (naming, complexity, style). Run in CI.
- **Rationale**: Prompt explicitly requests “a linter configured (such as Detekt)”; constitution requires quality gates.
- **Alternatives considered**: ktlint only — rejected in favour of Detekt’s broader rule set and configurable style.

## 4. CI/CD (GitHub Actions)

- **Decision**: GitHub Actions workflows for: (1) build (Android + shared); (2) test (unit + optional instrumented); (3) optional Detekt. Keep workflows minimal and fast.
- **Rationale**: Prompt asks for “basic CI/CD operations, for instance, build and test.”
- **Alternatives considered**: No CI — rejected; other CI providers — deferred; GitHub Actions is standard for GitHub-hosted repos.

## 5. Mock / debug data

- **Decision**: Implement 1–5 mock records (at least one rack and several items) in shared or platform code, toggled or preloaded in debug builds only. No mock data in production build.
- **Rationale**: Spec FR-011 and clarifications require mock data for debugging and UI assessment.
- **Alternatives considered**: No mock data — rejected; production seed data — rejected.

## 6. Firebase placeholder

- **Decision**: Reuse the existing `RackDataSource` and `ItemDataSource` contracts and add Firebase placeholder implementations behind those interfaces. Document where Firebase (or alternative) will be wired later. Do not add Firebase SDK or real backend in MVP.
- **Rationale**: Prompt: “Initially the back-end will be hosted in Firebase, so leave some placeholders… may change in the future, so keep things as flexible as possible.”
- **Alternatives considered**: Full Firebase in MVP — out of scope; no placeholder — would make future migration harder.
- **Implementation note**: The placeholder seam lives in `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/datasource/`. `FirebaseRackDataSource` and `FirebaseItemDataSource` are stub implementations of the existing datasource interfaces and are not yet wired into production DI.
- **Integration point**: `SqlDelightRackRepository` and `SqlDelightItemRepository` remain the local-first repository facades. When Firebase sync is added, the repositories can switch datasource implementations or coordinate between the SQLDelight and Firebase datasources without changing domain contracts.

## 7. SQLDelight local persistence (T037)

- **Schema**: `racks`, `shelf_slots` (FK to `racks` `ON DELETE CASCADE`), `items` (FK to `racks` and `shelf_slots` `ON DELETE CASCADE`). Android enables `setForeignKeyConstraintsEnabled(true)` on open; iOS uses Sqliter `extendedConfig.foreignKeyConstraints = true` so cascade and FK checks apply.
- **Tags**: Stored in `items.tags_json` as a single TEXT column using an internal delimiter codec (`encodeTags` / `decodeTags` in `SqlDelightCodec.kt`), not a join table — sufficient for MVP; FTS or normalized tags can follow if needed.
- **Search**: `searchItems` uses SQLite `LIKE` on lowercased `name` / `description` (see `searchItemsByQuery` in `StoreItDatabase.sq`).
- **Timestamps**: `created_at` / `updated_at` as INTEGER epoch millis; repositories persist values supplied by domain (`Clock` / UI).
- **photoUri**: Stored as nullable TEXT (platform path or URI string). Copies vs `content://` handling are a future concern if remote sync or strict lifecycle requires it.
- **Layering**: SQL runs only in `SqlDelight*DataSource` implementations; `SqlDelight*Repository` classes delegate to those sources and keep `Result` / `Flow` domain contracts.
