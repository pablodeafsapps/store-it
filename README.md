This is a Kotlin Multiplatform project targeting Android and iOS.

**Important (AGP 9.0 migration):** The project no longer has a separate `:shared` module. All shared logic and the Android app live in the **`:composeApp`** module.

* **[composeApp](./composeApp/src)** — KMP app module containing both shared and Android code:
  - **[commonMain](./composeApp/src/commonMain/kotlin)** — code common to all targets (domain, data, use cases).
  - **[androidMain](./composeApp/src/androidMain/kotlin)** — Android-only code (Activities, Compose UI, platform services).
  - Other source sets (e.g. [iosMain](./composeApp/src/iosMain/kotlin), [jvmMain](./composeApp/src/jvmMain/kotlin)) for platform-specific code when present.
  For example, [iosMain](./composeApp/src/iosMain/kotlin) is for iOS-specific Kotlin; [jvmMain](./composeApp/src/jvmMain/kotlin) for Desktop/JVM if used.

* **[iosApp](./iosApp/iosApp)** — iOS application entry. Use this for SwiftUI and for running the iOS app, even when sharing UI with Compose Multiplatform.

### Build and Run Android Application

From the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```
Or use the run configuration from the IDE run widget.

### Build and Run iOS Application

Use the run configuration from the IDE, or open [/iosApp](./iosApp) in Xcode and run the iOS app target.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html).
