# General Software Conventions (Kotlin, Android, KMP)

This document is intentionally project-agnostic.

## 1. Engineering Principles

- Favor clarity, correctness, and maintainability over cleverness.
- Keep boundaries explicit and dependencies directional.
- Prefer deterministic behavior and observable state transitions.
- Treat performance as a measured concern, not a default assumption.

## 2. Kotlin Conventions

### 2.1 Naming and Structure

- Package names: lowercase.
- Types: PascalCase.
- Functions/properties: camelCase.
- Constants: `UPPER_SNAKE_CASE` for true constants.
- Prefer one primary top-level type per file.

### 2.2 Language Usage

- Prefer `val` to `var`.
- Prefer expression-bodied functions when the body is a single expression.
- Prefer exhaustive `when` for sealed hierarchies.
- Avoid `!!`; use explicit null handling and boundary checks.
- Use `require`, `check`, `error` for explicit contract/invariant failures.

### 2.3 Error Handling

- Catch narrow exception types.
- Avoid blanket `Exception`/`Throwable` catches unless there is a strong boundary reason.
- Never swallow coroutine `CancellationException`.
- Preserve root cause and message when mapping unknown failures.

### 2.4 Coroutines and Flow

- Use `suspend` for one-shot asynchronous operations.
- Use `Flow` for streams and reactive state.
- Keep streams cold by default unless sharing is intentional.
- Use `stateIn`/`shareIn` with explicit sharing strategy.
- Protect mutable shared in-memory state with `Mutex` + `withLock`.

## 3. Android Conventions

### 3.1 UI and State

- Use state-driven UI and unidirectional data flow.
- Keep composables small and focused.
- Pass only required parameters to composables unless broader state passing is intentionally justified.

### 3.2 Compose Stability

- Prefer true immutability over relying on `@Stable`/`@Immutable`.
- Use stability annotations only when required and justified by behavior/perf evidence.

### 3.3 Resources and Theming

- Use `snake_case` for Android resource names.
- Avoid hardcoded strings/dimensions in UI logic.
- Centralize spacing/typography/color tokens.

## 4. Kotlin Multiplatform Conventions

### 4.1 Source Set Responsibilities

- `commonMain`: business rules, domain contracts, shared logic.
- Platform source sets: only platform-specific integrations and `actual` implementations.
- Keep platform layers thin; keep business decisions in shared/domain layers.

### 4.2 `expect`/`actual`

- Keep `expect` APIs minimal and stable.
- Ensure behavioral parity across platform `actual` implementations.
- Prefer interfaces + DI for frequently changing capabilities.

### 4.3 Visibility

- Prefer `internal` by default.
- Use `public` only for intentional module API surfaces.

## 5. Architecture and Design

- Follow dependency direction: UI/Presentation -> Domain -> Data.
- Domain should not depend on framework/platform details.
- Use constructor injection and interfaces at boundaries.
- Keep repositories domain-oriented and hide storage/transport details.
- Apply SOLID pragmatically; avoid over-abstraction.

## 6. Testing Conventions

### 6.1 Unit Testing Style

- Use clear GIVEN-WHEN-THEN naming.
- Use `sut` as subject-under-test variable name.
- Initialize common setup in `@BeforeTest setUp()`.
- Separate setup/action/assertions with blank lines.
- Use `runTest {}` for coroutine tests.

### 6.2 Determinism

- Prefer fakes over mocks unless interaction verification is required.
- Avoid real network/filesystem/clock in unit tests.
- Keep tests hermetic and repeatable.

### 6.3 Presentation Test Utilities

- Use a shared `TestUtils` file for `uiState` and `uiEvent` collection helpers.
- Avoid redefining ad-hoc collector utilities in each test file.

## 7. Review Checklist

- Are boundaries respected?
- Is mutable shared state synchronized?
- Are errors mapped and surfaced consistently?
- Are tests deterministic and behavior-focused?
- Is platform-specific logic kept minimal?

## 8. References

- Kotlin coding conventions: https://kotlinlang.org/docs/coding-conventions.html
- Android Kotlin style guide: https://developer.android.com/kotlin/style-guide
- Kotlin Multiplatform docs: https://kotlinlang.org/docs/multiplatform.html
- Clean Architecture: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
- SOLID principles: https://en.wikipedia.org/wiki/SOLID
