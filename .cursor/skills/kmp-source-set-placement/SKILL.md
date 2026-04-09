---
name: kmp-source-set-placement
description: >-
  Places Kotlin Multiplatform code in the correct Gradle source sets for this
  repo (shared common/android/ios vs androidApp). Use when adding or moving
  Kotlin files, expect/actual, platform APIs (SqlDriver, ViewModel bases), or
  when the user asks where KMP code should live.
---

# KMP source set placement (Store It)

## Modules

| Module | Role                                                                                                                          |
|--------|-------------------------------------------------------------------------------------------------------------------------------|
| `:shared` | Shared KMP library: domain, data, shared Compose UI, DI init, SQLDelight. Ships a static `Shared` framework to iOS. |
| `:androidApp` | Android application entry: navigation shell, Android-only Compose screens, `implementation(projects.shared)`.                 |

## `:shared` source sets

### `commonMain`

- Business logic, repositories, use cases, mappers, shared ViewModels **when** they use only multiplatform APIs.
- Shared Compose UI (`org.deafsapps.storeit.presentation…`), shared models, `commonMain` resources via Compose Multiplatform resources.
- SQLDelight `.sq` files and generated APIs; **database opening** stays behind `expect` (see `androidMain` / `iosMain`).
- Koin modules and metadata; KSP output under `build/generated/ksp/metadata/commonMain/kotlin` (already wired in `shared/build.gradle.kts`).

### `androidMain`

- `actual` implementations for `expect` declarations.
- Android-specific drivers and bindings (e.g. SQLDelight Android driver usage in `createStoreItSqlDriver()`).
- AndroidX `ViewModel` base classes for `expect`/`actual` abstract ViewModels (`StoreItViewModel.android.kt`).
- Dependencies that are JVM/Android-only (see `androidMain.dependencies` in `shared/build.gradle.kts`).

### `iosMain`

- `actual` for the same `expect` surface as `androidMain`.
- iOS-specific drivers and helpers (e.g. native SQLDelight driver, SKIE-related patterns).
- Do **not** put SwiftUI here; Swift lives under `iosApp/`.

### `commonTest`

- Pure multiplatform unit tests (kotlin-test, coroutines test, fakes).
- No Robolectric or Android APIs unless using a dedicated Android test source set (this project’s JVM Android tests for presentation often live in `androidApp`; see below).

## `:androidApp` source sets

- **App-specific** Compose, navigation, and Android-only presentation tests.
- **`src/test`**: JVM unit tests that need Android/JUnit5/Robolectric-style setup or test code that lives next to Android-only UI.
- **`src/androidTest`**: Instrumented UI tests.

Prefer **`shared/src/commonTest`** for logic that is truly shared and has no Android dependency.

## Decision checklist

1. **Does it import `android.*`, `androidx.*` (non-multiplatform), or JVM-only APIs?** → `androidMain` or `:androidApp`, not `commonMain`.
2. **Does it need different behavior on iOS vs Android?** → `expect` in `commonMain`, `actual` in `androidMain` and `iosMain` (keep declarations aligned; use `internal` when the API is not public — see project encapsulation rule).
3. **Is it UI shown on both platforms from shared code?** → `shared` `commonMain` (and platform resources only if truly platform-specific).
4. **Is it only for the Android app shell (deep links, Android nav, Material-only experiments)?** → `androidApp`.

## Examples in this repo

- `StoreItDatabaseProvider.kt` + `createStoreItSqlDriver()` **expect** in `commonMain`; **actual** in `androidMain` / `iosMain`.
- `StoreItViewModel` **expect** in `commonMain`; **actual** subclasses in `androidMain` / `iosMain`.
- `RackDetailViewModel` in `commonMain` when logic is multiplatform; Android-focused tests under `androidApp/src/test/...` when they target Android integration.

## Related

- SQLDelight config: `shared/build.gradle.kts` (`sqldelight { databases { … } }`).
- New `expect`/`actual`: add both platform actuals before merging; register new KSP targets if you add new KMP targets (mirror existing `kspIosArm64` / `kspIosSimulatorArm64` pattern).
