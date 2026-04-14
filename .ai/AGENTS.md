# Kotlin Multiplatform Architecture & Agents

This document defines how agents (humans or tools) should reason about, extend, and maintain a **Kotlin Multiplatform (KMP)** application that:

- Targets **Android** and **iOS**.
- Follows the **official KMP structure and patterns** as in the [Kotlin Multiplatform documentation](https://kotlinlang.org/docs/multiplatform.html).
- Applies **Clean Architecture** and **state-of-the-art KMP practices**.
- **Heavily relies on sum types** to model happy-path vs. unhappy-path outcomes (e.g. [Arrow `Either`](https://apidocs.arrow-kt.io/arrow-core/arrow.core/-either/index.html) or other monadic result types) instead of relying on exceptions for expected failures.

The target audience is experienced Android engineers building or reviewing a KMP codebase.

## Skill And Rule Sources

- Canonical project-owned skills live under `.agents/skills/`.
- Engine-specific overlays may also exist under `.cursor/skills/`, `.cursor/rules/`, and `.claude/commands/`.
- User-level fallback skills may exist under `/Users/pablo/.codex/skills/`.
- When the same workflow or guidance exists in multiple places, prefer `.agents/skills/` as the authoritative project source and keep the engine-specific copies aligned to it.

---

## 1. High-Level System Shape

**Post-AGP 9.0 migration:** This project uses a **single KMP app module** (`:shared`) and a separate **Android app module** (`:androidApp`).

**Principle:** Maximise code in **commonMain** (`:shared`); keep **androidMain**, **iosMain**, and app modules as thin as possible (only what cannot live in commonMain).

- **Shared logic module** (`:shared`):
  - **commonMain** (and commonTest): Domain, data, and shared presentation (pure Kotlin ViewModels, UI state). Shared across targets; no platform types.
  - **androidMain**: Only `actual` implementations for platform services and Android-specific integrations (HTTP engine, logging, etc.). No Activities, Compose UI, or ViewModel wrappers here.
  - **iosMain** (if present): Only `actual` implementations for platform services; iOS UI lives in `iosApp` (Swift/SwiftUI).
- **Android app module** (`:androidApp`):
  - Activities, Compose UI screens, and **AndroidX ViewModel wrappers** that hold the pure ViewModel from commonMain. Depends on `:shared`.
- **iOS app target** (`iosApp`):
  - Swift/SwiftUI code that consumes the shared framework produced by `:shared`; Swift `ObservableObject` wrappers for ViewModels live here.

**Dependency direction**:

`UI (Android/iOS)` → `Presentation` → `Domain` → `Data` → `Platform APIs`

No layer may depend on a more external layer; dependencies always point inward.

### 1.1 Platform linking and DI (post–breaking change)

A previous approach that tied Android and iOS to shared logic via a single Koin setup and a shared `CoroutineScope` on iOS caused persistent bugs on iOS and made the app unreliable. The current design separates platform entry points and scope ownership clearly.

- **Android**
  - **Entry**: `StoreItApplication` in **`:androidApp`** (see `androidApp/.../StoreItApplication.kt`) extends `Application`, is declared in `AndroidManifest.xml` (`android:name=".StoreItApplication"`), and initialises Koin in `onCreate()`.
  - **Koin**: Uses `initKoin { androidLogger(); modules(AndroidModule().module); androidContext(this@StoreItApplication) }`. **AndroidModule** (in `:androidApp`, `di/AndroidModule.kt`) is a `@Module(includes = [AppModule::class])` with `@ComponentScan("org.deafsapps.storeit")`, so it pulls in the shared **AppModule** and any Android-specific bindings. The Android app does **not** use the default `initKoin()` module list directly; it passes `AndroidModule().module` so that the app module owns the composition root.
  - **ViewModels**: Android uses Koin’s `viewModel()` / `@KoinViewModel` in the app module; the shared pure ViewModels in `commonMain` receive `viewModelScope` from the AndroidX ViewModel wrapper (see §4.7 and §3.1).

- **iOS**
  - **Entry**: The Swift app entry (e.g. `@main struct iOSApp: App`) calls `KoinInitKt.doInitKoinIos()` in `init()`. That invokes **`initKoinIos()`** in `shared` commonMain (`di/KoinInit.kt`), which is `initKoin {}` — i.e. Koin starts with **only** `AppModule().module` and no platform-specific options.
  - **ViewModels**: iOS does **not** use a single shared `CoroutineScope` for all ViewModels. **`IosKoinHelper`** (commonMain, `di/IosKoinHelper.kt`) is a `KoinComponent` object that exposes typed getters for each shared ViewModel. It resolves ViewModels **without** passing a scope: e.g. `getRackListViewModel(): RackListViewModel = get()`, `getRackDetailViewModel(rackId: String) = get(parameters = { parametersOf(rackId) })`. The **scope** is provided by the **actual** implementation of **`StoreItViewModel`** in **iosMain** (`StoreItViewModel.ios.kt`): when `coroutineScope` is `null`, the iOS actual uses `MainScope()`. So each ViewModel instance gets its own scope from the actual, avoiding a single shared scope that could cause lifecycle/cancellation bugs.
  - **Lifecycle**: Swift **`ViewModelHolder<T: StoreItViewModel>`** (in `iosApp`) holds the KMP ViewModel and calls `sharedVm.clear()` in `deinit`, so when the Swift view is deallocated the ViewModel’s scope is cancelled.

- **Shared base ViewModel**
  - **`StoreItViewModel`** is an **expect** class in commonMain; **actuals** live in **androidMain** and **iosMain**.
  - **androidMain**: `StoreItViewModel` extends AndroidX `ViewModel` and uses `viewModelScope` (or the injected scope). The Android app uses the shared ViewModel subclasses (e.g. `RackListViewModel`) via Koin’s ViewModel machinery; no wrapper ViewModels in the app module are needed.
  - **iosMain**: `StoreItViewModel` is a `@Factory` class with `viewModelScope = MainScope()` when no scope is passed, and exposes a **`clear()`** method that calls `onCleared()` and `viewModelScope.cancel()`. Swift wraps the instance in `ViewModelHolder` and calls `clear()` in `deinit`.

**Rule**: Do not reintroduce a shared `CoroutineScope` in `IosKoinHelper` or elsewhere for iOS ViewModels; scope is owned by each `StoreItViewModel` actual (or by the AndroidX ViewModel on Android). For parameterised ViewModels (e.g. `RackDetailViewModel(rackId)`), only pass the business parameters (e.g. `rackId`) in `parametersOf`, not a scope.

**Relevant files**
- Android: `androidApp/.../StoreItApplication.kt`, `androidApp/.../di/AndroidModule.kt`, `AndroidManifest.xml` (`android:name=".StoreItApplication"`).
- Shared: `shared/.../di/KoinInit.kt` (`initKoin`, `initKoinIos`), `shared/.../di/IosKoinHelper.kt`, `shared/.../di/AppModule.kt`.
- Platform ViewModel base: `shared/.../presentation/StoreItViewModel.kt` (expect), `StoreItViewModel.android.kt` (androidMain), `StoreItViewModel.ios.kt` (iosMain).
- iOS: `iosApp/.../iOSApp.swift` (calls `KoinInitKt.doInitKoinIos()`), `iosApp/.../ViewModels/ViewModelHolder.swift` (holds KMP ViewModel, calls `clear()` in `deinit`).

---

## 2. KMP Module Structure (shared)

The multiplatform module (`:shared`) is configured roughly as:

- Targets:
  - `jvm("android")`
  - `iosX64()`, `iosArm64()`, `iosSimulatorArm64()` (or their `ios()` aggregate).
- Source sets:
  - `commonMain` / `commonTest`
  - `androidMain` / `androidUnitTest`
  - `iosMain` / `iosTest` (or `iosXxxMain` composed into `iosMain`)

### 2.1 Source-Set Responsibilities

- **commonMain**:
  - Pure Kotlin, **no platform types**.
  - Contains:
    - **Domain layer**: entities, value objects, use case interfaces and implementations, domain services, domain-specific errors. **Concrete domain data classes** (e.g. `internal data class …Model` for `Item`, `Rack`, …) are **`internal` to `:shared`**; **`androidApp`** and **`iosApp`** must only use **public domain interfaces** and **public factory functions** (see §3.1 and §7.4).
    - **Data layer contracts**: repository interfaces, common DTOs, mapping logic (when platform-agnostic), simple cache abstractions.
    - **Shared presentation** (optional): shared state holders (e.g. `StateFlow`-based ViewModels) used by both platforms.
    - `expect` declarations for minimal platform services (time, UUID, logging, secure storage, etc.).
- **androidMain** (minimal; no UI or ViewModel wrappers):
  - `actual` implementations for platform services only.
  - Android-specific integrations: HTTP client engines, persistence engines, logging, analytics bindings. No Activities, Compose UI, or AndroidX ViewModel classes—those live in `:androidApp`.
- **iosMain**:
  - `actual` implementations for platform services.
  - iOS-specific integrations: HTTP engines, persistence engines, logging, analytics, DI wiring for the Apple framework.

**Rule**: `commonMain` is **framework-free** (except for allowed KMP/multiplatform libraries). It must be testable purely with `commonTest` and not require any Android/iOS runtime.

---

## 3. Clean Architecture Mapping

### 3.1 Layers

- **Domain (core)** – in `commonMain`:
  - Business rules and invariants.
  - **Entities at the module boundary**: public **interfaces** (e.g. `Item`, `Rack`) for types that flow to **`androidApp`** / **`iosApp`** or the exported framework; **`internal`** `data class` implementations (e.g. `ItemModel`, `RackModel`) stay inside **`shared`** only. App modules and Swift must not depend on `*Model` types.
  - Value objects, domain services.
  - Use cases / interactors (`UseCase` classes or functions).
  - Repository interfaces, expressed in domain terms (they use the public domain interfaces, not internal concrete classes).
  - No reference to networking, databases, UI, or platform APIs.

- **Data** – primarily in `commonMain`, extended in `androidMain` & `iosMain`:
  - Repository implementations depending on:
    - Remote data sources (HTTP, gRPC, etc.).
    - Local data sources (SQL, key-value, file, secure storage).
  - DTOs, mappers (DTO ↔ domain).
  - Platform-specific data sources are implemented via `actual` code or injected platform services.

- **Presentation**:
  - Option A (recommended for heavy sharing):
    - **Pure Kotlin ViewModels in `commonMain`**: Shared state holders are plain Kotlin classes (no AndroidX `ViewModel` or iOS types). Annotate with `@Factory`; inject a `CoroutineScope` via `@Provided` and use cases via constructor (scope is owned by the platform). Expose state and events via `StateFlow`/`SharedFlow`. Implement `clear()` that calls `coroutineScope.cancel()` so the platform wrapper can cancel when the screen is disposed.
    - **Platform wrappers**: **Android** (`:androidApp`): AndroidX `ViewModel` (`@KoinViewModel`) in the Android app module holds the pure ViewModel (built with `viewModelScope`), exposes it to the UI, and calls `pureViewModel.clear()` in `onCleared()`. **iOS**: Swift `ObservableObject` in `iosApp` gets the pure ViewModel from `IosKoinHelper` with `parametersOf(createViewModelScope())`, exposes state/events to SwiftUI, and calls `viewModel.clear()` in `deinit`.
    - **Data loading**: Use `stateIn` or an `init { coroutineScope.launch { ... } }` block; initial value can have `isLoading = true`. For testability inject a `CoroutineScope` (e.g. `TestScope` from `runTest`).
    - Thin UI layers bind to the pure ViewModel (Android) or Swift wrapper (iOS) and forward user actions.
  - Option B (platform-first UI):
    - Presentation layer lives in platform modules only; shared module stops at Domain/Data.

### 3.2 Dependency Rules

- Domain depends on **nothing** but Kotlin stdlib and allowed shared libraries.
- Data depends on Domain and infrastructure abstractions/platform APIs (via `expect` or injected interfaces).
- Presentation depends on Domain (and optionally Data when the team explicitly decides to collapse them).
- UI depends only on Presentation and **shared domain types exposed as public interfaces** from `:shared` (not internal domain implementations).

These rules are enforced by:

- Gradle module and source-set boundaries (e.g. `:shared` with `commonMain`, `androidMain`, `iosMain`).
- `sourceSets` dependency configuration (`dependsOn` relationships).

---

## 4. Tech Stack & Dependencies

The following stack is designed to align with official KMP recommendations and current ecosystem practices.

### 4.1 Language & Core

- **Kotlin**:
  - Latest stable language version supported by the Kotlin Multiplatform Gradle plugin.
  - `kotlin("multiplatform")` (with Compose Multiplatform) for the `:shared` module.
  - Gradle Kotlin DSL for build scripts.
- **Coroutines & Flow**:
  - `kotlinx-coroutines-core` in `commonMain`.
  - Platform-specific coroutine dependencies in `androidMain`/`iosMain` if needed.

### 4.2 Result Types & Error Modelling (Sum Types)

- **Principle**: Model success and failure as **explicit types**, not exceptions. Use a **sum type** (disjoint union) so the compiler enforces handling of both paths.
- **Recommended (Kotlin)**:
  - [Arrow Core `Either<E, A>`](https://apidocs.arrow-kt.io/arrow-core/arrow.core/-either/index.html): right-biased; use `Left` for errors (e.g. domain/validation/IO), `Right` for success. Compose with `map`, `flatMap`, `fold`, `getOrElse`, `mapLeft`; use sealed hierarchies for `E` to get exhaustive handling.
  - Alternative: a project-defined `sealed interface Result<out E, out T>` (e.g. `Success(T)`, `Failure(E)`).
- **Usage**:
  - Use cases and repository interfaces return `Either<DomainError, T>` (or `Flow<Either<DomainError, T>>`) rather than throwing for expected failures.
  - Map infrastructure exceptions to domain errors at boundaries (e.g. `Either.catch { ... }.mapLeft { toDomainError(it) }`).
  - Presentation/UI layers `fold` or pattern-match on the result to derive UI state and user-facing messages.
- **Building Result values**: Prefer the extension functions `value.ok()` and `error.err()` when constructing success/failure (e.g. `list.ok()`, `DomainError.NotFound(...).err()`) instead of `Result.ok(value)` / `Result.err(error)`.
- **Delete / clear result shape**: For datasource operations that delete or clear persisted records, prefer `Result<DomainError, Long>` when the underlying store can report affected-row counts. `ok(0L)` means the operation executed successfully and nothing matched; reserve `err(...)` for execution failures.
- **Unknown error mapping**: When converting an unexpected `Throwable` into `DomainError.Unknown`, preserve the original failure context by setting both `message` and `cause`. Do not discard the thrown exception behind a bare `DomainError.Unknown()` unless there is no throwable available.
- **Swift alignment**: In iOS code, mirror the same semantics with Swift enums with associated values (e.g. `Result<Success, Failure>`) or a custom `Either`-like type so success/failure handling stays consistent across the stack.

### 4.3 Networking

- **Ktor Client** (or another KMP-capable client) in `commonMain`:
  - Core + content negotiation plugin (e.g. JSON).
  - Platform engines in `androidMain` and `iosMain` (e.g. CIO / OkHttp / Darwin).
  - HTTP interfaces abstracted behind repository/data-source interfaces; use dependency inversion to keep Ktor from leaking into Domain.

### 4.4 Serialization

- **kotlinx-serialization**:
  - JSON as default for network and persistence payloads.
  - DTOs defined in Data layer, mapping to domain entities.

### 4.5 Persistence

Choose a KMP-capable persistence solution (SQLDelight, Realm, or similar). The recommended pattern:

- Define DAOs or data sources in Data layer, behind interfaces.
- Expose only domain models or mappers at the domain boundary.
- Keep raw SQL or storage-specific schemas outside Domain.

### 4.6 Date/Time & Utilities

- **kotlinx-datetime** in `commonMain` for time/date handling.
- Platform-specific utilities (e.g. logging, locale) behind `expect`/`actual` pairs or injected interfaces.

### 4.7 Dependency Injection (Koin for KMP)

This project uses **Koin** with **Koin Annotations** (KSP) for dependency injection:

- **Module**: A single `@Module` with `@ComponentScan("org.deafsapps.storeit")` in `commonMain` discovers and registers all annotated types. The generated module is loaded via `AppModule().module` in `initKoin()`.
- **Registration**: Use cases and repositories are registered with `@Factory(binds = [UseCaseType::class])` or `@Single(binds = [Repository::class])`; use a typealias for the use case interface so the implementation binds to it (e.g. `GetRacksUseCaseType`, `SaveRackUseCaseType`).
- **ViewModels**: Shared presentation uses **pure Kotlin ViewModels** in `commonMain` (see §3.1): annotated with `@Factory`, they receive a `CoroutineScope` via `@Provided` and use cases via constructor. Resolution with a scope is done at the call site: Android wrappers in `:androidApp` pass `viewModelScope` via Koin’s ViewModel machinery; iOS uses **`IosKoinHelper`** getters (e.g. `getRackListViewModel()`, `getRackDetailViewModel(rackId)`) which resolve ViewModels **without** passing a scope — the scope is provided by the **StoreItViewModel** actual in iosMain (e.g. `MainScope()` when `coroutineScope` is null). Platform wrappers live in the **app** modules: AndroidX `ViewModel` in **`:androidApp`** (not in shared/androidMain), Swift `ObservableObject` in **`iosApp`**; they own the scope and call `clear()` on the pure ViewModel when the screen is disposed.
- **Initialisation**:
  - **Android**: Use a custom `Application` subclass (**`StoreItApplication`** in `:androidApp`) declared in the manifest. In `onCreate()` call `initKoin { androidLogger(); modules(AndroidModule().module); androidContext(this@StoreItApplication) }`. `AndroidModule` includes `AppModule` and provides the composition root.
  - **iOS**: Call `KoinInitKt.doInitKoinIos()` from the Swift app’s `init()` (e.g. in `@main struct iOSApp: App`). This runs `initKoin {}`, which starts Koin with only `AppModule().module` (no platform-specific options).
- **Rule**: Domain/use case types remain framework-agnostic; only the composition root and ViewModel layer use Koin APIs.

### 4.8 UI Technologies

- **Android** (`:androidApp`):
  - Jetpack Compose for UI, Activities, and AndroidX ViewModel wrappers. All Android UI and ViewModel wrappers live in `:androidApp`; `shared/androidMain` does not contain them.
  - AndroidX libraries for lifecycle, navigation, etc. Koin for DI at the app level; shared code remains in `:shared`.
- **iOS**:
  - SwiftUI as the primary UI framework; all iOS UI lives in the **`iosApp`** target (Swift), not in the KMP module. The shared framework produced by `:shared` is consumed as a dependency (`Shared`).
  - Integration with shared KMP ViewModels: use **Swift `ObservableObject` wrappers** that obtain the pure Kotlin ViewModel from `IosKoinHelper` with `parametersOf(createViewModelScope())`, expose its `uiState` and `uiEvent` to SwiftUI (e.g. via Skie or `Observing`), and call the ViewModel’s `clear()` in `deinit` so the coroutine scope is cancelled when the view is dismissed.
  - Koin is initialised from the iOS app entry point with `KoinInitKt.doInitKoinIos()` so shared dependencies are available.

---

## 5. Gradle & Project Configuration

### 5.1 Version Catalog & Build Logic

- Use a **version catalog** (`libs.versions.toml`) to centralize:
  - Library versions (Kotlin, Coroutines, Ktor, serialization, Koin, etc.).
  - Plugin versions (Kotlin Multiplatform, Android Gradle Plugin, KSP, etc.).
- Keep Kotlin, KSP, Koin, and Koin Annotations versions aligned when upgrading (e.g. Kotlin 2.3.x, KSP 2.3.x, Koin 4.x, koin-annotations 2.3.x).
- Encapsulate shared build logic in convention plugins or shared Gradle scripts where appropriate.

### 5.2 Multiplatform Plugin Configuration

- In the `:shared` (KMP) module:
  - Configure `kotlin {}` with:
    - Targets: `androidTarget()`, `iosX64()`, `iosArm64()`, `iosSimulatorArm64()` (or `ios()` aggregate).
    - `sourceSets` relationships: `iosMain` depends on individual iOS targets when aggregated.
  - Add dependencies to the correct source sets:
    - `commonMain` for shared libraries (Coroutines, Flow, serialization, Ktor core, etc.).
    - `androidMain` for Android-specific libraries (AndroidX, OkHttp engine, logging).
    - `iosMain` for iOS-specific libraries (Darwin engine, iOS logging).

### 5.3 Android Module Configuration

- Use **Android Gradle Plugin** (AGP 9.x-compatible) with Kotlin, Compose, and modern compile/target SDK versions.
- Configure:
  - `compileSdk`, `minSdk`, `targetSdk` according to product requirements.
  - Jetpack Compose compiler and BOM (if used).
  - The `:shared` module is the single KMP module (no separate shared library artifact).
- **Instrumented UI tests (`:androidApp`)**: Apply the **`de.mannodermaus.android-junit`** plugin alongside the Android application plugin so **JUnit 5** runs on device/emulator. `androidTestImplementation` dependencies include **`org.junit.jupiter:junit-jupiter-api`**, **`junit-jupiter-engine`** (runtime), **`de.mannodermaus.junit5:android-test-compose`** (Compose **`createAndroidComposeExtension`**, **`ComposeContext`**), and **`androidx.compose.ui:ui-test-junit4`**. Align transitive Compose UI versions with the project if needed (see `androidApp/build.gradle.kts`).

### 5.4 iOS Integration

- Configure `:shared` to export an **XCFramework** (or equivalent) for iOS:
  - Framework name stable and semantic (e.g. `SharedCore`).
  - Exposed APIs stable and idiomatic from Swift.
- Integrate via:
  - Xcode project linkage (Swift Package Manager or manual framework integration).
  - Swift bridging layer as needed to map KMP types to idiomatic Swift types.

---

## 6. Agent Playbook: Adding a New Feature

This section describes how an engineer (or automation agent) should add or modify a feature in a Clean Architecture KMP project.

### 6.1 Domain First

- **Define domain model(s)** in `commonMain`:
  - Add a **public interface** for each entity that may cross the **`shared`** boundary (UI state, repository contracts, Swift interop).
  - Implement it with an **`internal data class`** (suffix **`Model`** in this project) and expose **public factory functions** (e.g. `Item(...)`, `Rack(...)`) so **`androidApp`** / **`iosApp`** never reference concrete implementations. Do not add new public `data class` domain entities unless the team explicitly widens the API.
  - Value objects, domain events if needed.
- **Define or extend repository interfaces**:
  - Express operations purely in domain terms.
  - Avoid leaking transport/storage details (HTTP, DB schemas).
- **Create use case(s)**:
  - One per user-level operation (e.g. `GetUserProfile`, `UpdateSettings`).
  - Encapsulate validation, orchestration, and transactional rules.

### 6.2 Data Layer

- Implement repository interfaces in Data layer:
  - Introduce or extend remote/local data sources.
  - Map DTO ↔ domain entities via dedicated mappers.
- **Thread-safety for in-memory repositories**:
  - All in-memory repository implementations (e.g. `InMemoryRackRepository`, `InMemoryItemRepository`) must protect shared mutable state (e.g. `MutableMap`, `MutableList`) from concurrent access.
  - Use `Mutex` from `kotlinx.coroutines.sync` to synchronize access to shared collections.
  - Wrap all read and write operations (including `clear()` methods) with `mutex.withLock { }` to prevent race conditions.
  - Example pattern:
    ```kotlin
    internal class InMemoryRepository {
        private val items = mutableMapOf<String, Item>()
        private val mutex = Mutex()
        
        override suspend fun getItem(id: String) = mutex.withLock {
            items[id]?.ok() ?: DomainError.NotFound(...).err()
        }
        
        override suspend fun saveItem(item: Item) = mutex.withLock {
            items[item.id] = item
            item.ok()
        }
    }
    ```
- If platform-specific services are required:
  - Add `expect` declarations in `commonMain`.
  - Implement `actual` counterparts in `androidMain` and `iosMain`.

### 6.3 Presentation & UI

- If using shared presentation:
  - Create or extend **pure Kotlin ViewModels** in `commonMain`: plain classes with `@Factory`, `@Provided CoroutineScope`, use cases, `StateFlow`/`SharedFlow`, and `clear()` that cancels the scope.
  - Expose state as immutable streams (e.g. `StateFlow<UiState>`); route events through explicit functions.
  - On Android: add an **AndroidX ViewModel wrapper** in **`:androidApp`** (`@KoinViewModel`) that constructs the pure ViewModel with `viewModelScope` and use cases, exposes it to the Activity/Compose screen, and calls `pureViewModel.clear()` in `onCleared()`. Do not put ViewModel wrappers in `shared/androidMain`.
  - On iOS: add a **Swift `ObservableObject` wrapper** that gets the pure ViewModel from `IosKoinHelper` with `parametersOf(createViewModelScope())`, exposes state/events to SwiftUI, and calls `clear()` in `deinit`.
- On Android:
  - Bind Compose UI to the **pure ViewModel** held by the wrapper (e.g. `androidRackListViewModel.rackListViewModel`).
  - Maintain unidirectional data flow: UI → events → ViewModel → state → UI.
  - **Previews**: Every Compose screen must have `@Preview` composables that cover **all meaningful UI scenarios** (e.g. loading, empty state, error, success with data, different content sizes) so the screen can be reviewed in isolation without running the app.
- On iOS:
  - Bind SwiftUI views to the Swift wrapper; the wrapper holds and observes the pure Kotlin ViewModel.
  - Use Skie, `Observing`, or equivalent to observe KMP state/events consistently.

### 6.4 Code style

- **Named parameters (mandatory)**: All function and constructor calls **must** use named arguments at every call site (Kotlin and Swift), including tests and calls to standard library or framework APIs, e.g. `paramName = value`, so that call sites stay readable and refactor-safe.

### 6.5 Testing

- **commonTest**:
  - Test use cases with fake repositories.
  - Test repositories with fake data sources (or in-memory implementations).
  - **Subject under test (SUT)**: Hold the class or entity under test in a variable named **`sut`**. Use `sut` for all calls to the tested type in the test body.
  - **Initialisation**: Initialise the SUT and dependencies in a **`setUp()`** function annotated with **`@BeforeTest`**, using **`lateinit var`** so each test gets a fresh instance. Do not assign the SUT at class level.
  - **Dependencies: use fakes**: Do not instantiate real implementations (e.g. `InMemoryRackRepository`) when the test has a dependency. Use a **fake** that implements the interface (e.g. `FakeRackRepository` in `commonTest/.../fake/`). Name the dependency clearly (e.g. `fakeRackRepository` of type `RackRepository` or `FakeRackRepository`). In `setUp()` create the fake and assign it; in the test, set the fake’s return values (e.g. `fakeRackRepository.getAllRacksResult = listOf(...).ok()`) so the test can assess the SUT’s behaviour.
  - **Suspend / coroutines**: Use `runTest { }` from `kotlinx-coroutines-test` (not `runBlocking`) for tests that call suspend functions. Add `kotlinx-coroutines-test` to `commonTest` dependencies; use `fun testName() = runTest { ... }`.
  - **Test naming**: Follow GIVEN–WHEN–THEN with **GIVEN**, **WHEN**, **THEN** in caps in the test name (e.g. `` `GIVEN empty repository WHEN getAllRacks THEN returns empty list`() ``).
  - **Test body structure**: Structure each test in three sections (setup → action → assertions) separated by **blank lines only**; do not add `// GIVEN`, `// WHEN`, or `// THEN` comments. If there is no setup (e.g. “empty repository” or “any state”), leave a single blank line after the test opening, then the WHEN and THEN blocks.
- **Android ViewModel tests** (pure Kotlin ViewModels in `commonMain`):
  - Place unit tests in **`androidApp/src/test`** so JUnit and coroutines-test are available. Test the **pure ViewModel** directly: use **fakes** for use cases (e.g. `FakeGetRacksUseCase`) and inject a **`CoroutineScope`** (e.g. `TestScope(testDispatcher)` from `runTest`) so execution is deterministic. Use `runTest(testDispatcher)` and `advanceUntilIdle()`; collect `uiState`/`uiEvent` in a list and assert on the latest value so `stateIn`/`shareIn` updates are observed. Add `kotlinx-coroutines-test` to the Android module’s `testImplementation` dependencies.
- **UI tests (mandatory for every new screen/view)**:
  - **Android**: Every new Jetpack Compose screen or meaningful view must have **instrumented** UI tests under **`androidApp/src/androidTest`**, using **JUnit 5** (`@Test`, `@BeforeEach` from `org.junit.jupiter.api`) and **`StoreItComposeUiTestBase`**:
    - Subclass **`StoreItComposeUiTestBase`**; implement scenarios inside **`composeUiTest(initialScreen = ..., testBody = { ... })`** and use **`ComposeContext`** APIs (`onNodeWithTag`, `performClick`, `assertIsDisplayed`, etc.).
    - The base registers the Mannodermaus **`createAndroidComposeExtension<ComponentActivity>()`** extension, wires a minimal **`NavScreen`** graph with **`koinViewModel`** and real screens, clears repositories in **`@BeforeEach`**, and offers **`seedRack`** / **`seedSlot`** / **`seedItem`** for data setup.
    - Prefer **`testTag`** on composables for selectors; keep **`kotlinx-coroutines-test`** available for **`runTest`** in setup helpers. Follow existing test class naming: descriptive **`camelCase`** test method names (often with **`_`** between logical parts), not necessarily GIVEN–WHEN–THEN.
  - **iOS**: Every new SwiftUI screen or meaningful view must have UI tests (XCTest UI tests in the iOS app’s UI test target, e.g. `StoreItUITests`) that cover the screen’s main flows and states.
  - Add tests when introducing or significantly changing a screen; use accessibility identifiers (iOS) or **`testTag`** (Android) so tests stay stable when layout changes.
- **Android/iOS integration tests**:
  - Verify integration with platform-specific services, navigation, and lifecycle.

---

## 7. Non-Functional Concerns

### 7.1 Performance

- Prefer **cold flows** and suspend functions over ad-hoc callbacks.
- Avoid doing heavy work on the main thread:
  - Use `Dispatchers.Default`/`IO` or platform-specific dispatchers abstracted behind a dispatcher provider.
- Minimize overhead of Kotlin/Swift interop:
  - Expose coarse-grained APIs to iOS rather than many tiny calls when possible.

### 7.2 Error Handling & Logging

- **Prefer sum types over exceptions** for expected failure paths:
  - Use **`Either<E, T>`** (e.g. [Arrow Either](https://apidocs.arrow-kt.io/arrow-core/arrow.core/-either/index.html)) or an equivalent result type so success and failure are both in the type signature. Model domain/validation/IO errors as sealed hierarchies (e.g. `sealed interface DomainError`) for exhaustive handling.
  - Reserve **exceptions** for truly unrecoverable or programmer errors (e.g. contract violations, unexpected environment failures).
- Map errors at the boundaries:
  - In repositories/data: wrap throwing code with `Either.catch { ... }.mapLeft { toDomainError(it) }` (or equivalent) so domain layers receive `Either<DomainError, T>`.
  - In presentation/UI: `fold` (or pattern-match) on the result to derive UI state and user-facing messages; do not let raw exceptions bubble into the UI.
- Logging:
  - Provide a `Logger` abstraction in `commonMain`.
  - Implement in `androidMain` (Logcat / structured logs) and `iosMain` (OSLog/print or equivalent).

### 7.3 Security

- Keep secrets out of commonMain; use platform-specific secure storage via `expect`/`actual` or injected services.
- Treat any serialization/persistence boundary as untrusted input; validate accordingly.

### 7.4 Encapsulation & implementation style

- **Visibility**: Prefer `internal` for classes, interfaces, objects, and top-level functions unless the declaration is intentionally part of the module’s public API (e.g. consumed by another Gradle module). Use `internal` on both `expect` and `actual` when the API is only used inside the module.
- **Domain data models**: Shared logic lives in **`shared`**; **domain model implementations** (`internal data class …Model`) are **not** part of the public API for **`androidApp`** or **`iosApp`**. Other modules may import only **public interfaces** and **factories** from `org.deafsapps.storeit.domain.model`. Preserving this boundary keeps platform code from coupling to concrete Kotlin types and leaves room for future persistence/network DTOs named without colliding with domain internals.
- **Expression-bodied functions**: Prefer single-expression function bodies using `= expression` (no `return` keyword) when the logic fits clearly in one expression; use `when`/`if` expressions where appropriate to keep functions as expressions.

### 7.5 Concurrency & Thread Safety

- **Repository thread-safety**: All repository implementations that maintain mutable in-memory state (e.g. `MutableMap`, `MutableList`) must be thread-safe to prevent race conditions when accessed concurrently by multiple coroutines or threads.
- **Synchronization mechanism**: Use `Mutex` from `kotlinx.coroutines.sync` for coroutine-based synchronization. Wrap all operations that access or modify shared mutable state with `mutex.withLock { }`.
- **Scope of protection**: Protect both read and write operations. Even read-only operations must be synchronized if they access mutable collections that can be modified concurrently.
- **Helper methods**: Methods like `clear()` that modify shared state must also be `suspend` functions and protected by the mutex.
- **Pattern**: Each repository instance should have its own `Mutex` instance protecting its internal state. Do not share mutexes across different repository instances.

---

## 8. References

- **Kotlin Multiplatform Documentation**: [https://kotlinlang.org/docs/multiplatform.html](https://kotlinlang.org/docs/multiplatform.html)
- **Arrow Core (Either & result types)**: [Arrow Either](https://apidocs.arrow-kt.io/arrow-core/arrow.core/-either/index.html).
- **Coroutines & Flow**: official kotlinx.coroutines docs.
- **Ktor Client**: official Ktor client docs for KMP.
- **kotlinx.serialization**: official serialization docs.
- **Clean Architecture**: R. C. Martin, “The Clean Architecture”.

Agents maintaining or extending a KMP project should treat this document as the architectural contract for structure, dependencies, and technology choices.
