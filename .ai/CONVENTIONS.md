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

- Prefer `val` over `var`; use `var` only when state must change.
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

- **commonMain**: Shared business logic, domain models, use cases, repository interfaces, and expect declarations.
- **androidMain** / **iosMain**: Platform implementations (expect/actual), platform APIs, and DI wiring.
- **commonTest**: Shared unit tests for common code; no platform APIs.

### 3.2 Expect / Actual

- **Expect** in `commonMain`: public API only; minimal surface (what the platform must provide).
- **Actual** in platform source sets: one implementation per target; avoid branching inside actuals when possible.
- Name expect/actual consistently: `expect class Platform()`, `expect fun currentTimeMillis(): Long`.
- Prefer expect/actual for a small set of primitives (clock, UUID, crypto, logging, analytics); keep heavy or frequently changing APIs behind interfaces in common code and inject platform implementations.

### 3.3 Naming Across Platforms

- Use the same logical names for shared concepts: e.g. `User`, `AuthToken`, `Result` so that Kotlin and Swift (or KMP-generated headers) align.
- Repository and use case names should be identical in shared and platform code to avoid cognitive mismatch.

### 3.4 Shared Models

- Keep shared models in commonMain: data classes or interfaces used in use cases and repositories.
- Prefer immutable data: `data class` in Kotlin; in Swift, structs with `let` properties.
- Use a single source of truth for DTOs if you share networking (e.g. kotlinx.serialization); document field names and optionality so iOS can mirror or generate models consistently.

---

## 4. Clean Architecture in KMP

### 4.1 Layer Boundaries

- **Domain**: Entities and use case interfaces (and implementations). No framework or platform types; only pure Kotlin (and shared types).
- **Data**: Repository implementations, DTOs, mappers, remote/local data sources. Depends only on domain.
- **Presentation / UI**: ViewModels (or equivalent), UI state, platform UI. Depends on domain (use cases); avoid depending on data layer types in the UI.

Dependency rule: inner layers do not know outer layers. Dependencies point inward (e.g. data → domain, presentation → domain).

### 4.2 Module Structure (Logical or Physical)

- **domain**: entities, use case interfaces, repository interfaces (optional: use case implementations here).
- **data**: data sources, repository implementations, DTOs, mappers.
- **ui / presentation**: ViewModels, UI state, screens (platform-specific or Compose Multiplatform).

Use cases sit in domain and orchestrate repository interfaces; they return domain types or simple sealed results (Success / Error).

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

### 6.2 Use Case (Interactor)

- One use case per application operation: e.g. `GetUserUseCase`, `LoginUseCase`. Encapsulates business rules and orchestration.
- Input: simple parameters (ids, DTOs at boundary). Output: domain type, sealed result, or `Flow`.
- Use case should not depend on UI or framework; keep it testable with plain Kotlin.

### 6.3 Mapper

- Dedicated types or extension functions for DTO → Entity and Entity → UI model. Keep mapping in one place; avoid anemic entities that are “just DTOs” in domain.

### 6.4 Dependency Injection

- Prefer constructor injection; avoid service locators in business logic.
- In KMP: shared `expect` for “obtain use case / repository” if needed, or inject in platform main; keep DI configuration in platform source sets when using platform-specific DI (Hilt, Koin, Swift DI).

### 6.5 Result / Either

- Use a sealed hierarchy for operations that can fail: e.g. `sealed interface Result<out T> { data class Ok<T>(val value: T) : Result<T>; data class Error(val cause: Throwable) : Result<Nothing> }`, or a type like `Either<L, R>`.
- Prefer returning `Result<T>` or `Flow<Result<T>>` from use cases rather than throwing in the business layer; let the UI layer handle error presentation.

---

## 7. Testing

### 7.1 Unit Tests

- Test use cases with fake or mock repositories (interfaces); test repository implementations with fake data sources.
- Naming: `methodUnderTest_scenario_expectedOutcome` or `shouldDoSomethingWhenCondition`.
- One logical behaviour per test; avoid testing implementation details (e.g. exact call count unless contract requires it).

### 7.2 Fakes vs Mocks

- Prefer fakes (in-memory or stub implementations) for speed and stability; use mocks when you need to assert interactions (e.g. “save was called once”).

### 7.3 Shared Tests

- commonTest: run use case and repository interface tests with shared fakes; no Android/iOS APIs.
- Platform tests: ViewModel, UI, or platform-specific code in androidTest/iosTest or equivalent.

---

## 8. Resources & Configuration

### 8.1 Android

- Resources: `snake_case`: `ic_user_avatar`, `string_app_name`. IDs: `camelCase` in code; `snake_case` in XML when consistent with resources.
- Keep string and dimension resources in appropriate `values` files; use qualifiers for locales and configurations.

### 8.2 iOS

- Asset and storyboard names: PascalCase or kebab-case per team choice; be consistent. Swift code references by symbol name.

### 8.3 Shared Configuration

- For KMP, prefer a single place (e.g. build-time or expect/actual) for environment or feature flags so both platforms stay in sync.

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
