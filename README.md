# Store it!

Kotlin Multiplatform project targeting Android and iOS.

## Structure

- **`:shared`** contains all shared business logic plus the Android app shell.
- **`shared/src/commonMain`** contains domain, data, use cases, DI, and shared presentation logic.
- **`shared/src/androidMain`** contains Android-specific integrations and UI.
- **`shared/src/iosMain`** contains iOS-specific Kotlin actuals and integrations.
- **`iosApp/`** contains the SwiftUI iOS application entry point.

## Prerequisites

- JDK 17
- Android SDK
- Xcode and an iOS Simulator runtime for iOS builds

## Build

```bash
./gradlew :androidApp:assembleDebug
```

## Test

```bash
./gradlew :shared:allTests :androidApp:testDebugUnitTest
```

## Verify

Use the same verification command that CI and local delivery use:

```bash
./gradlew detekt :shared:allTests :androidApp:testDebugUnitTest :androidApp:assembleDebug --no-daemon
```

## Run

Android:

```bash
./gradlew :androidApp:installDebug
```

iOS:

Open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme on an available simulator.

## Feature docs

- [Feature spec](./specs/001-storage-rack-organiser/spec.md)
- [Implementation plan](./specs/001-storage-rack-organiser/plan.md)
- [Quickstart](./specs/001-storage-rack-organiser/quickstart.md)
