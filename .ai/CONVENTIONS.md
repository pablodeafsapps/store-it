# KMP Code Conventions & Design Guidelines

A reference for Kotlin Multiplatform (KMP) applications aligning Kotlin and Swift platform idioms with Clean Architecture and SOLID. Target: seasoned Android engineers.

---

## 1. Kotlin Conventions

### 1.1 Official Baseline

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)

### 1.2 Naming

| Element | Convention | Example |
| ------- | ---------- | ------- |
| Packages | lowercase, no underscores | `org.example.feature.auth` |
| Classes, objects, interfaces | PascalCase | `UserRepository`, `AuthUseCase` |
| Functions, properties, variables | camelCase | `fetchUser()`, `isLoggedIn` |
| Constants | UPPER_SNAKE or `const val` in object | `MAX_RETRY_COUNT`, `object Config { const val API_BASE = "..." }` |
| Private properties | camelCase; prefix `_` only if needed to shadow | `_cachedResult` (sparingly) |
| Type parameters | Single uppercase letter or PascalCase | `T`, `E`, `Key`, `ResultType` |
| Test methods | `snake_case` or `shouldDoSomething` | `returns_user_when_valid_id()` |

### 1.3 File & Class Layout

- One top-level declaration per file when that declaration is the primary type; filename matches type name: `UserRepository.kt` → `class UserRepository`.
- For extension or small supporting types, co-locate when they have a single logical owner: `User.kt` may contain `class User` and `fun User.toDisplayName()`.
- Order inside a file: property declarations → `init` → companion → public API (overrides first) → private API.

### 1.4 Idioms

- **Named parameters (mandatory)**: When calling any function or constructor, you **must** use named arguments at every call site (including tests and calls to standard library or framework APIs), using the form `paramName = value` (e.g. `Rack(id = "1", name = "Rack 1")`, `sut.saveRack(rack = rack)`, `assertEquals(expected = 0, actual = list.size)`). This improves readability and makes refactors safer and is enforced across the codebase.
- Prefer `val` over `var`; use `var` only when state must change.
- **Prefer expression-bodied functions**: Use `= expression` instead of `{ return expression }` when the body is a single expression (single- or multi-line). Avoid the `return` keyword where an expression form is clear: e.g. `fun get(id: String) = repo.findById(id)` or `fun delete(id: String) = when { id.isBlank() -> error.err(); else -> unit.ok() }`.
- Use expression-bodied members when they fit on one line: `fun id(): String = uuid`.
- Prefer `when` over long `if`/`else`; make it exhaustive for sealed types.
- Use scope functions by intent: `apply` (configuring), `also` (side effects), `let` (null-safe transform), `run` (object-scoped block), `with` (non-extension).
- Prefer `?.let { }` or early return over nested null checks.
- Use `require()` / `check()` / `requireNotNull()` for preconditions; `error()` for unreachable or illegal state.
- Prefer `Sequence` for chained transformations on large collections; use `Iterable` when you need multiple iterations or materialized results.

### 1.5 Nullability & Types

- Mark types nullable only when the value can be absent; avoid `!!`; use `requireNotNull()` or `checkNotNull()` at boundaries if needed.
- Prefer sealed classes/interfaces for closed hierarchies and exhaustive `when`.
- Use `inline` classes (value classes) for type-safe wrappers (e.g. `UserId`, `Email`) to avoid primitive obsession.

### 1.6 Coroutines & Flow

- Suspend functions for one-shot async work; `Flow` for streams.
- Prefer `flow { }` / `flowOf()` and operators over `Channel` unless you need backpressure or multi-consumer semantics.
- Expose cold flows from data layers; collect in the presentation layer.
- Use `SupervisorJob` in scopes where one child failure should not cancel siblings.
- Prefer `stateIn`/`shareIn` with appropriate `SharingStarted` (e.g. `WhileSubscribed(5000)`) for shared flows in ViewModels.
- **Thread-safety**: When implementing repositories or data sources with mutable in-memory state (e.g. `MutableMap`, `MutableList`), use `Mutex` from `kotlinx.coroutines.sync` to synchronize access. Wrap all operations (reads and writes) with `mutex.withLock { }` to prevent race conditions. Example:
  ```kotlin
  private val items = mutableMapOf<String, Item>()
  private val mutex = Mutex()
  
  override suspend fun getItem(id: String) = mutex.withLock {
      items[id]?.ok() ?: DomainError.NotFound(...).err()
  }
  ```

---

## 2. Swift Conventions (iOS / KMP Native)

When writing Swift in the iOS app or in shared contracts that mirror Swift style:

### 2.1 Baseline

- [Swift API Design Guidelines](https://swift.org/documentation/api-design-guidelines/)
- [Swift Style Guide (Ray Wenderlich)](https://github.com/raywenderlich/swift-style-guide) (optional team choice)

### 2.2 Naming

| Element | Convention | Example |
| ------- | ---------- | ------- |
| Types | PascalCase | `UserRepository`, `AuthUseCase` |
| Functions, variables, parameters | camelCase | `fetchUser()`, `isLoggedIn` |
| Constants | camelCase (not SCREAMING) | `maxRetryCount`, `apiBaseURL` |
| Enum cases | camelCase | `case notFound`, `case loading` |
| Protocols | Noun or -able/-ing | `UserFetching`, `Cacheable` |

### 2.3 Clarity Over Brevity

- Name parameters for call-site clarity; use argument labels.
- Prefer `guard` for early exits; keep the “happy path” less indented.
- Use `enum` with associated values for result/state instead of tuples or multiple optionals where it improves clarity.

### 2.4 Concurrency

- Prefer `async/await` over completion handlers and raw `DispatchQueue`.
- Use `@MainActor` for UI and UI-bound state; isolate background work in actor or nonisolated code.
- Prefer `async let` and `TaskGroup` for structured concurrency over unstructured `Task` when dependencies are clear.

---

## 3. KMP-Specific Conventions

### 3.1 Source Set Layout

- In this project the KMP module is **`:shared`**. **Principle:** Maximise code in **commonMain**; keep androidMain, iosMain, and app modules as thin as possible.
- **commonMain**: Shared business logic, domain models, use cases, repository interfaces, shared presentation (pure Kotlin ViewModels, UI state), expect declarations, **AppModule**, **KoinInit** (`initKoin` / `initKoinIos`), and **IosKoinHelper**. No platform types.
- **androidMain** / **iosMain**: Only **actual** implementations for platform services (e.g. **StoreItViewModel** actuals) and minimal platform integrations. No UI, no ViewModel wrappers—those live in the app modules.
- **androidApp** (separate module): **StoreItApplication** (custom `Application` that initialises Koin with **AndroidModule**), Activities, Compose UI, and use of shared ViewModels via Koin (no wrapper ViewModels; shared ViewModels extend **StoreItViewModel** and are resolved with `viewModelScope` from the AndroidX environment).
- **iosApp** (Swift): SwiftUI UI; `ViewModelHolder<T: StoreItViewModel>` holds the KMP ViewModel and calls `clear()` in `deinit`. ViewModels are obtained from **IosKoinHelper** (no scope passed; scope comes from **StoreItViewModel** iOS actual).
- **commonTest**: Shared unit tests for common code; no platform APIs.

### 3.1.1 Speckit Artifact Locations

- Store project-wide Speckit governance and templates under `.specify/`.
- `/speckit.constitution` updates `.specify/memory/constitution.md`.
- Store feature-scoped Speckit artifacts under `specs/<NNN-feature-name>/`.
- `/speckit.specify` creates the feature folder and writes `spec.md`.
- `/speckit.plan` writes `plan.md` plus supporting design artifacts such as `research.md`, `data-model.md`, `quickstart.md`, and `contracts/`.
- `/speckit.tasks` writes `tasks.md`.
- `/speckit.clarify` updates the feature `spec.md`.
- `/speckit.checklist` writes markdown under `specs/<NNN-feature-name>/checklists/`.
- `/speckit.analyze` is primarily read/report oriented; if its output is persisted, keep it in the same feature folder.
- `/speckit.implement` primarily consumes feature artifacts and produces code changes rather than a required markdown file.
- `/speckit.taskstoissues` consumes `tasks.md` and primarily produces GitHub issues rather than markdown output.

### 3.2 Platform DI and linking (post–breaking change)

- **Android**: Koin is started from **StoreItApplication** (`:androidApp`) with `initKoin { androidLogger(); modules(AndroidModule().module); androidContext(this) }`. `AndroidModule` includes `AppModule`; the app module owns the composition root. Shared ViewModels are resolved via Koin’s ViewModel support; they extend **StoreItViewModel** (actual in androidMain extends AndroidX `ViewModel`).
- **iOS**: Koin is started from the Swift app entry with `KoinInitKt.doInitKoinIos()` (which runs `initKoin {}` — only `AppModule`). ViewModels are obtained from **IosKoinHelper** (commonMain). **Do not** pass a shared `CoroutineScope` from IosKoinHelper; the **StoreItViewModel** actual in iosMain provides the scope (e.g. `MainScope()` when `coroutineScope` is null). For parameterised ViewModels (e.g. `RackDetailViewModel`), pass only the business parameters: `getRackDetailViewModel(rackId: String)` with `parametersOf(rackId)`. Swift uses **ViewModelHolder** and calls `sharedVm.clear()` in `deinit`.

### 3.3 Visibility & encapsulation

- **Prefer `internal` by default**: Use `internal` for classes, interfaces, objects, and top-level functions unless the declaration is intentionally part of the module’s public API (e.g. consumed by another Gradle module). Do not add `internal` to members inside an interface (e.g. sealed subclasses); the containing type’s visibility applies.
- **Expect/actual**: Use `internal` on both `expect` and `actual` when the API is only used inside the module; use public only for types that other modules or the framework must see.
- **Domain data models (`:shared` only)**: Concrete domain model types (e.g. `internal data class …Model` implementing `Item`, `Rack`, …) live in **`shared`** and are **`internal`** so they are **not** visible to **`androidApp`** or **`iosApp`**. Other modules depend on **public interfaces** in `org.deafsapps.storeit.domain.model` (e.g. `Item`, `Rack`) and **public factory functions** with the same names as the former constructors (e.g. `Item(...)`, `Rack(...)`) to construct values. Do not reference `*Model` types from app modules or tests outside `shared`.

### 3.4 Expect / Actual

- **Expect** in `commonMain`: minimal surface (what the platform must provide); prefer `internal` when not part of the public API.
- **Actual** in platform source sets: one implementation per target; avoid branching inside actuals when possible.
- Name expect/actual consistently: `expect class Platform()`, `expect fun currentTimeMillis(): Long`.
- Prefer expect/actual for a small set of primitives (clock, UUID, crypto, logging, analytics); keep heavy or frequently changing APIs behind interfaces in common code and inject platform implementations.

### 3.5 Naming Across Platforms

- Use the same logical names for shared concepts: e.g. `User`, `AuthToken`, `Result` so that Kotlin and Swift (or KMP-generated headers) align.
- Repository and use case names should be identical in shared and platform code to avoid cognitive mismatch.

### 3.6 Shared Models

- **Boundary**: Domain models used across **`androidApp`**, **`iosApp`**, and **`shared`** are expressed as **public interfaces** in `commonMain` (e.g. `Item`, `Rack`, `ShelfSlot`). **Implementations** are **`internal data class …Model`** in `:shared` only; they must not be imported or referenced from **`androidApp`** or **`iosApp`**.
- **Construction**: Use **public factory functions** in the same package (e.g. `Item(...)`, `Rack(...)`) so call sites keep a constructor-like API without exposing concrete types. Inside `shared`, prefer factories or `asModel()` helpers when you need `data class` features (e.g. replacing former `.copy()` usage).
- Prefer immutable values at the interface boundary; in Swift, structs with `let` properties where you mirror types locally.
- Use a single source of truth for DTOs if you share networking (e.g. kotlinx.serialization); document field names and optionality so iOS can mirror or generate models consistently.

---

## 4. Clean Architecture in KMP

### 4.1 Layer Boundaries

- **Domain**: Entities (as public interfaces + internal implementations in `:shared`), use case interfaces (and implementations). No framework or platform types; only pure Kotlin (and shared types). Types consumed by **`androidApp`** / **`iosApp`** are the public domain interfaces, not internal `*Model` classes (see §3.6).
- **Data**: Repository implementations, DTOs, mappers, remote/local data sources. Depends only on domain.
- **Presentation / UI**: ViewModels (or equivalent), UI state, platform UI. Depends on domain (use cases); avoid depending on data layer types in the UI.

Dependency rule: inner layers do not know outer layers. Dependencies point inward (e.g. data → domain, presentation → domain).

### 4.2 Module Structure (Logical or Physical)

- **domain**: entities, use case interfaces, repository interfaces (optional: use case implementations here).
- **data**: data sources, repository implementations, DTOs, mappers.
- **ui / presentation**: ViewModels, UI state, screens (platform-specific: Android Compose in `androidApp`, iOS SwiftUI in `iosApp`).

Use cases sit in domain and orchestrate repository interfaces; they return domain types or simple sealed results (Success / Error).

**Pure Kotlin ViewModels (shared)**: State holders in `shared/commonMain` are plain Kotlin classes (no AndroidX `ViewModel` or iOS types). Annotate with `@Factory`; inject a `CoroutineScope` via `@Provided` and use cases via constructor (scope owned by the platform). Expose state via `StateFlow`, events via `SharedFlow`. Implement `clear()` that calls `coroutineScope.cancel()`. Prefer automatic data loading: use `stateIn` or `init { coroutineScope.launch { … } }`; for testability inject a `CoroutineScope` (e.g. `TestScope` from `runTest`).

**Platform wrappers**: **Android** (`:androidApp`): AndroidX `ViewModel` with `@KoinViewModel` in the Android app module holds the pure ViewModel (built with `viewModelScope`), exposes it to the UI, and calls `pureViewModel.clear()` in `onCleared()`. Do not put ViewModel wrappers in `shared/androidMain`. **iOS**: Swift `ObservableObject` in `iosApp` obtains the pure ViewModel from `IosKoinHelper` with `parametersOf(createViewModelScope())`, exposes state/events to SwiftUI, and calls `viewModel.clear()` in `deinit`.

### 4.3 Dependency Direction

- ViewModel → UseCase (interface), not Repository.
- UseCase → Repository (interface).
- Repository implementation → DataSource (interface or concrete for simple cases), Mapper (DTO → Entity).

Inject interfaces; provide implementations in the platform or shared DI graph.

---

## 5. SOLID in Practice

### 5.1 Single Responsibility (SRP)

- One reason to change per class: e.g. `UserRepository` handles user persistence contract; `FetchUserUseCase` handles the “fetch user” flow; `UserViewModel` handles UI state and user actions.
- Split large “god” classes into smaller types (e.g. separate mappers, validators, formatters).

### 5.2 Open/Closed (OCP)

- Extend via interfaces and new implementations rather than modifying existing classes. E.g. new auth strategy = new class implementing `AuthStrategy`, not changing `AuthManager`.
- Use sealed hierarchies and `when` for behaviour that must stay closed to modification but open to new variants (new subclass = new branch in `when`).

### 5.3 Liskov Substitution (LSP)

- Implementations of interfaces must be substitutable without breaking callers: no throwing in cases the interface does not allow, no strengthening preconditions or weakening postconditions.
- In KMP, actual implementations of expect classes or repository interfaces must honour the same contract on all platforms.

### 5.4 Interface Segregation (ISP)

- Narrow interfaces: e.g. `UserReader` and `UserWriter` instead of a single `UserRepository` if clients only need read or only write.
- ViewModels depend on use case interfaces that expose only the methods they need (e.g. `GetUserUseCase` with `getUser(id): Flow<User>`).

### 5.5 Dependency Inversion (DIP)

- High-level modules (use cases, ViewModels) depend on abstractions (interfaces); low-level modules (repos, data sources) implement them.
- Constructors (or factories) receive interfaces; the composition root (DI) wires concrete implementations.

---

## 6. Design Patterns

### 6.1 Repository

- One repository per aggregate or entity root. Exposes domain types; hides data source details (remote, cache, DB).
- Methods: `getById`, `getStream`, `save`, `delete` (or equivalent). Return `Flow`/streams for reactive consumption; suspend for one-shot.
- Implementation coordinates one or more data sources and applies caching/strategy internally.
- **Thread-safety**: In-memory repository implementations must protect shared mutable state from concurrent access using `Mutex`. All read and write operations accessing mutable collections must be wrapped with `mutex.withLock { }`. Helper methods like `clear()` must also be `suspend` and protected by the mutex.

### 6.2 Use Case (Interactor)

- One use case per application operation: e.g. `GetUserUseCase`, `LoginUseCase`. Encapsulates business rules and orchestration.
- Input: simple parameters (ids, DTOs at boundary). Output: domain type, sealed result, or `Flow`.
- Use case should not depend on UI or framework; keep it testable with plain Kotlin.

### 6.3 Mapper

- Dedicated types or extension functions for DTO → Entity and Entity → UI model. Keep mapping in one place; avoid anemic entities that are “just DTOs” in domain.

### 6.4 Dependency Injection (Koin)

- This project uses **Koin** with **Koin Annotations** (KSP): **AppModule** in `commonMain` has `@Module` and `@ComponentScan("org.deafsapps.storeit")`. **Android** uses a custom **Application** subclass (**StoreItApplication** in `:androidApp`) declared in the manifest; in `onCreate()` it calls `initKoin { androidLogger(); modules(AndroidModule().module); androidContext(this@StoreItApplication) }`. **AndroidModule** (in `:androidApp`) is `@Module(includes = [AppModule::class])` and provides the composition root. **iOS** calls `KoinInitKt.doInitKoinIos()` from the app entry point; that runs `initKoin {}` (only `AppModule().module`, no platform options).
- **Shared ViewModels** in `commonMain` extend **StoreItViewModel** (expect in commonMain; actuals in androidMain and iosMain). They take an optional `coroutineScope` (default `null`); on Android the scope comes from the ViewModel machinery; on iOS the **StoreItViewModel** actual uses `MainScope()` when null. **Do not** pass a shared `CoroutineScope` from **IosKoinHelper**; that pattern caused iOS bugs. **IosKoinHelper** (commonMain) exposes getters like `getRackListViewModel()`, `getAddRackViewModel()`, `getRackDetailViewModel(rackId: String)` and resolves with **only** business parameters (e.g. `parametersOf(rackId)` for RackDetail), not a scope.
- **Android**: Use Koin’s `viewModel()` / `@KoinViewModel` in the app module; no wrapper ViewModel classes. **iOS**: Use `ViewModelHolder<T: StoreItViewModel>` in Swift; it holds the KMP ViewModel and calls `sharedVm.clear()` in `deinit`. Prefer constructor injection; avoid `KoinComponent`/`inject()` in the pure ViewModels.
- Use case interfaces: expose a typealias (e.g. `GetRacksUseCaseType`) and bind the implementation to it so ViewModels depend on the interface.

### 6.5 Result / Either

- Use a sealed hierarchy for operations that can fail: e.g. `sealed interface Result<out T> { data class Ok<T>(val value: T) : Result<T>; data class Error(val cause: Throwable) : Result<Nothing> }`, or a type like `Either<L, R>`.
- **Building Result values**: Prefer the extension functions `value.ok()` and `error.err()` when constructing success/failure (e.g. `list.ok()`, `DomainError.NotFound(...).err()`) instead of `Result.ok(value)` / `Result.err(error)`.
- Prefer returning `Result<T>` or `Flow<Result<T>>` from use cases rather than throwing in the business layer; let the UI layer handle error presentation.

---

## 7. Testing

### 7.1 Unit Tests

- Test use cases with fake or mock repositories (interfaces); test repository implementations with fake data sources.
- **Subject under test (SUT)**: The class or entity under test must be held in a variable named **`sut`**. All references to the tested type in the test body use `sut` (e.g. `sut(Unit)`, `sut.saveRack(...)`).
- **Initialisation**: Initialise the SUT and its dependencies in a **`setUp`** function annotated with **`@BeforeTest`**, using **`lateinit var`** for the SUT and any shared dependencies so each test starts from a fresh setup. Do not assign the SUT or dependencies at class level; assign them in `setUp()`.
- **Naming**: Use **GIVEN–WHEN–THEN** with the words **GIVEN**, **WHEN**, and **THEN** in caps in the test name (e.g. `` `GIVEN saved rack WHEN getRackById with existing id THEN returns rack`() ``). Alternative patterns: `methodUnderTest_scenario_expectedOutcome` or `shouldDoSomethingWhenCondition` only when GIVEN–WHEN–THEN does not fit.
- **Test body structure**: Organise each test into three sections (setup → action → assertions) separated by **blank lines only**. Do not add `// GIVEN`, `// WHEN`, or `// THEN` comments. If there is no setup section, leave a single blank line after the test opening, then the action and assertion blocks.
- **Coroutines / suspend**: For tests that call suspend functions, use `runTest { }` from `kotlinx-coroutines-test` (e.g. `fun testName() = runTest { ... }`). Do not use `runBlocking` in tests. Add `kotlinx-coroutines-test` to the test source set dependencies.
- One logical behaviour per test; avoid testing implementation details (e.g. exact call count unless contract requires it).

### 7.2 Dependencies: use fakes, not real implementations

- **Do not instantiate real dependencies** in unit tests (e.g. do not use `InMemoryRackRepository()` when testing a use case that depends on `RackRepository`). Use a **fake** that implements the same interface.
- **Name fakes clearly**: e.g. `fakeRackRepository` of type `RackRepository` (or the concrete fake type, e.g. `FakeRackRepository`, if you need to configure it). Initialise the fake in `setUp()` and assign **return values** (or state) on the fake so the test can assess the SUT behaviour. For example, set `fakeRackRepository.getAllRacksResult = listOf(rack1, rack2).ok()` before invoking the use case to verify it returns that list.
- Fakes live in the test source set (e.g. `commonTest/.../fake/`) and implement the production interface with configurable results or simple in-memory behaviour.

### 7.3 Fakes vs Mocks

- Prefer fakes (in-memory or stub implementations with configurable return values) for speed and stability; use mocks when you need to assert interactions (e.g. “save was called once”).

### 7.4 UI tests (screens and views)

- **Requirement**: Every new screen or significant view must have UI tests on **both** platforms.
- **Android (Jetpack Compose, instrumented)**:
  - Tests live under **`androidApp/src/androidTest/java/...`** (typically mirroring the presentation package, e.g. `presentation/<feature>/ui/`).
  - **JUnit 5**: Use **`@Test`** from JUnit Jupiter (`org.junit.jupiter.api`). Shared setup on the base class uses **`@BeforeEach`** (e.g. clearing seeded repositories). Unit tests in `:androidApp` also use JUnit 5; instrumented tests rely on the **`de.mannodermaus.android-junit`** Gradle plugin so Jupiter runs on device/emulator.
  - **Compose rule**: Extend **`StoreItComposeUiTestBase`** (or follow the same pattern). It registers **`createAndroidComposeExtension<ComponentActivity>()`** from **`de.mannodermaus.junit5:android-test-compose`** via **`@RegisterExtension`**, and exposes **`composeUiTest { ... }`** whose lambda is a **`ComposeContext`** receiver—use **`onNodeWithTag`**, **`performClick`**, **`assertIsDisplayed`**, etc. from **`androidx.compose.ui.test`** (with **`androidx.compose.ui:ui-test-junit4`** on the `androidTest` classpath).
  - **What the base provides**: Koin is available (**`KoinComponent`**); **`baseSetUp()`** clears **`RackRepository`**, **`SlotRepository`**, and **`ItemRepository`** via **`runTest`**. Helpers **`seedRack`**, **`seedSlot`**, **`seedItem`** prepare data. **`renderApp`** builds a small in-test navigation shell ( **`NavScreen`** ) with **`koinViewModel`** and the real Compose screens—prefer exercising production composables with test tags rather than duplicating UI.
  - **Stable selectors**: Prefer **`Modifier.testTag(...)`** on interactive and assertable nodes; use semantics/text only when tags are not appropriate. **`debugImplementation`** of **`androidx.compose.ui:ui-test-manifest`** is required for Compose UI tests.
  - **Naming**: Instrumented UI tests in this project use descriptive **`camelCase`** method names (often with **`_`**-separated segments for scenario steps), e.g. `rackList_addRackFab_navigatesToAddRack`. They do **not** use the GIVEN–WHEN–THEN naming from §7.1 unless you deliberately align them.
- **iOS**: Add UI tests for each new SwiftUI screen (e.g. in the app’s UI test target such as `StoreItUITests`). Cover main user flows and key states. Use `accessibilityIdentifier` (or equivalent) so tests are stable across layout changes.
- Add or update UI tests whenever a screen is introduced or its behaviour is meaningfully changed.

### 7.5 Android Compose previews

- **Requirement**: Every Android (Jetpack Compose) screen must provide **previews that cover all possible scenarios**.
- Add `@Preview` composables for: loading state, empty state, error state, success state with data, and any other distinct UI states the screen can show (e.g. different steps in a flow, with/without optional content). This allows design and behaviour review without running the app and catches missing state handling.

### 7.6 Shared vs Platform Tests

- **commonTest**: Use case and repository tests with shared fakes; no Android/iOS APIs. No AndroidX types.
- **androidApp/src/test**: Unit tests for **pure Kotlin ViewModels** in `commonMain`. Test the pure ViewModel directly: use fakes for use cases and inject a `CoroutineScope` (e.g. `TestScope(testDispatcher)` from `runTest`) for deterministic execution. Use `runTest(testDispatcher)` and `advanceUntilIdle()`; collect `uiState`/`uiEvent` in a list and assert on the latest value so `stateIn`/`shareIn` updates are observed.
- **`androidApp/src/androidTest`**: Instrumented **Compose UI tests** with JUnit 5 and the Mannodermaus Compose extension (see §7.4). Not to be confused with **`commonTest`** (no Android APIs).

---

## 8. Resources & Configuration

### 8.1 Android

- Resources: `snake_case`: `ic_user_avatar`, `string_app_name`. IDs: `camelCase` in code; `snake_case` in XML when consistent with resources.
- Keep string and dimension resources in appropriate `values` files; use qualifiers for locales and configurations.
- **Compose dimensions**: Avoid magic numbers for spacing, padding, and sizes. Use a single **`Dimens`** object (e.g. in `androidApp/.../design/Dimens.kt`) with named properties (e.g. `screenPadding`, `spacingMedium`, `cardCornerRadiusSmall`) and reference them from all Compose screens and components.

### 8.2 iOS

- All iOS UI lives in the **`iosApp`** Swift target; the shared framework (`Shared`) is consumed as a dependency. Koin is initialised in the app entry with `KoinInitKt.doInitKoinIos()`. Obtain ViewModels from **IosKoinHelper** (e.g. `IosKoinHelper().getRackListViewModel()`, `getRackDetailViewModel(rackId:)`) and wrap them in **ViewModelHolder**; the holder calls `sharedVm.clear()` in `deinit`. Use SwiftUI views that take ViewModel state and event callbacks (e.g. `Observing(… uiState, … uiEvent.withInitialValue(nil)) { state, event in … }`).
- Asset and storyboard names: PascalCase or kebab-case per team choice; be consistent. Swift code references by symbol name.

### 8.3 Shared Configuration

- For KMP, prefer a single place (e.g. build-time or expect/actual) for environment or feature flags so both platforms stay in sync.
- **Library versions**: Centralise all library and plugin versions in `gradle/libs.versions.toml`. When upgrading, keep Kotlin, KSP, and Koin/koin-annotations aligned (e.g. Kotlin 2.3.x with KSP 2.3.x; Koin 4.x with koin-annotations 2.3.x).

---

## 9. References

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- [Swift API Design Guidelines](https://swift.org/documentation/api-design-guidelines/)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Clean Architecture (Uncle Bob)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)

---

*Keep this document as the single source of truth for style and structure; update it when the team adopts new patterns or tooling.*
