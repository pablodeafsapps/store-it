# Store it!

Kotlin Multiplatform project targeting Android and iOS.

## Structure

- **`:shared`** is the Kotlin Multiplatform module for shared domain, data, SQLDelight, DI, and shared presentation logic.
- **`shared/src/commonMain`** contains business logic, repositories, use cases, and shared presentation state.
- **`shared/src/androidMain`** contains Android-specific Kotlin actuals and integrations used by the shared module.
- **`:androidApp`** contains the Android application entry point, navigation shell, and Android Compose UI.
- **`shared/src/iosMain`** contains iOS-specific Kotlin actuals and integrations used by the shared module.
- **`iosApp/`** contains the SwiftUI iOS application.

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
- [Research](./specs/001-storage-rack-organiser/research.md)
