# Quickstart: Store it! (001-storage-rack-organiser)

**Branch**: `001-storage-rack-organiser`  
**Date**: 2025-02-10

## Prerequisites

- JDK 17+
- Android SDK (for Android build)
- Xcode and iOS toolchain (for iOS build; macOS only)
- Git

## Build & run

Project modules:

- `:shared` for shared Kotlin Multiplatform logic and SQLDelight
- `:androidApp` for the Android application shell and Compose UI
- `iosApp/` for the SwiftUI iOS application

### Android

```bash
./gradlew :androidApp:assembleDebug
# Run from IDE or:
./gradlew :androidApp:installDebug
```

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run the iOS app target, or use the run configuration from the IDE.

### Tests

```bash
./gradlew :shared:allTests :androidApp:testDebugUnitTest
```

## Linting (Detekt)

```bash
./gradlew detekt
```

## Full verification

```bash
./gradlew detekt :shared:allTests :androidApp:testDebugUnitTest :androidApp:assembleDebug --no-daemon
```

## CI (GitHub Actions)

Push to the feature branch to trigger build and test. Check `.github/workflows/build-and-test.yml` for the current job definitions.

The current CI verification flow is:

```bash
./gradlew detekt :shared:allTests :androidApp:testDebugUnitTest :androidApp:assembleDebug --no-daemon
```

## Feature docs

- **Spec**: [spec.md](./spec.md)
- **Plan**: [plan.md](./plan.md)
- **Data model**: [data-model.md](./data-model.md)
- **Contracts**: [contracts/repository-interfaces.md](./contracts/repository-interfaces.md)
- **Research**: [research.md](./research.md)

## Mock data

On Android and iOS debug builds, app startup preloads one sample rack and three sample items for validation:
- Rack list should show `Garage shelf`.
- Rack detail / slot drill-down should surface `Power drill`, `Paint cans`, and `Toolbox`.

Release builds should not preload mock data.

## Local DB (SQLDelight)

- SQLDelight schema files live in `shared/src/commonMain/sqldelight/`.
- The current schema is generated from `StoreItDatabase.sq` (v1 baseline).
- Runtime DB file name is `storeit.db`:
  - Android: app database directory via `AndroidSqliteDriver`.
  - iOS: app sandbox via `NativeSqliteDriver`.
- Migration strategy: add incremental `*.sqm` migration files in the same SQLDelight folder for future schema versions; keep `verifyMigrations` enabled in Gradle.
