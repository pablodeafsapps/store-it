# Quickstart: Store it! (001-storage-rack-organiser)

**Branch**: `001-storage-rack-organiser`  
**Date**: 2025-02-10

## Prerequisites

- JDK 17+
- Android SDK (for Android build)
- Xcode and iOS toolchain (for iOS build; macOS only)
- Git

## Build & run

### Android

```bash
./gradlew :composeApp:assembleDebug
# Run from IDE or:
./gradlew :composeApp:installDebug
```

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run the iOS app target, or use the run configuration from the IDE.

### Tests (composeApp; no separate :shared module)

```bash
./gradlew :composeApp:testDebugUnitTest
# Or all tests:
./gradlew test
```

## Linting (Detekt)

When Detekt is configured:

```bash
./gradlew detekt
```

## CI (GitHub Actions)

After workflows are added: push to the branch to trigger build and test. Check `.github/workflows/` for job definitions.

## Feature docs

- **Spec**: [spec.md](./spec.md)
- **Plan**: [plan.md](./plan.md)
- **Data model**: [data-model.md](./data-model.md)
- **Contracts**: [contracts/repository-interfaces.md](./contracts/repository-interfaces.md)
- **Research**: [research.md](./research.md)

## Mock data

Debug builds should include 1–5 mock records (at least one rack and some items). Toggle or preload per FR-011; no mock data in production builds.

## Local DB (SQLDelight)

- SQLDelight schema files live in `composeApp/src/commonMain/sqldelight/`.
- The current schema is generated from `StoreItDatabase.sq` (v1 baseline).
- Runtime DB file name is `storeit.db`:
  - Android: app database directory via `AndroidSqliteDriver`.
  - iOS: app sandbox via `NativeSqliteDriver`.
- Migration strategy: add incremental `*.sqm` migration files in the same SQLDelight folder for future schema versions; keep `verifyMigrations` enabled in Gradle.
