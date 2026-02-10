# Workflow: Unit Testing Logic-Bearing KMP Files (Kotlin/Swift)

This document defines a **rigorous, repeatable workflow** for designing, implementing, and validating unit tests for any logic-bearing KMP file (Kotlin and Swift): use cases, repositories, domain models, mappers, and services. The goals are **high coverage**, **deterministic results**, and **maintainable test suites** for business-critical logic, using popular testing libraries and the **GIVEN–WHEN–THEN** pattern.

---

## 1. Goals & Principles

- **Goals**
  - Ensure that all business rules and edge cases are covered by fast, deterministic unit tests.
  - Provide a uniform test structure across Kotlin and Swift codebases.
  - Make tests the primary specification of behaviour for critical logic.
- **Principles**
  - Always follow **GIVEN–WHEN–THEN** in test design and naming.
  - Keep unit tests **pure and isolated**: no real network, database, clocks, or randomness.
  - Prefer **TDD loops** (red → green → refactor) wherever feasible.
  - Keep test code **as simple as possible**: no clever abstractions unless clearly justified.

---

## 2. Prerequisites

- **Source Under Test (SUT)**
  - A logic-bearing unit with clear responsibilities:
    - Kotlin: use case, repository, domain model, mapper, domain service, etc.
    - Swift: use case/Interactor, repository, domain struct/class, mapper, service, etc.
  - Well-defined public API (methods/properties) and contracts.
- **Testing Frameworks**
  - **Kotlin/JVM**
    - JUnit 4 or 5 (`junit-jupiter`) as the primary test runner.
    - `kotlinx-coroutines-test` for coroutine-based code.
    - Mocking:
      - **MockK** (recommended) or **Mockito/Mockito-Kotlin**.
    - Optional: assertion libraries (e.g. AssertJ, Truth, Kotest assertions).
  - **Swift**
    - XCTest as the primary framework.
    - Optional: Quick/Nimble or similar BDD/assertion libraries.
- **KMP Layout & Build**
  - `commonTest` source set for shared Kotlin tests.
  - Platform-specific test targets for JVM/Android and iOS if needed.
  - Swift tests in corresponding iOS test targets.
- **Version Control (Git)**
  - Feature branch per piece of work: e.g. `test/<module>-<sut-name>` or within a feature branch.
  - All test-related changes committed with meaningful messages.

---

## 3. Phase 1 – Test Planning & Coverage Design

### 3.1 Catalogue Behaviour

For each SUT:

1. **List public methods/properties** and any external observable effects.
2. For each behaviour, identify:
   - Valid input scenarios.
   - Boundary and edge cases.
   - Error conditions and exceptional flows.
   - Concurrency or ordering concerns (if applicable).

### 3.2 Build a Test Matrix

Create a simple matrix (table or checklist) mapping:

- **Rows**: Scenarios (e.g. “valid input”, “empty list”, “invalid token”, “timeout”, “cache hit”, “cache miss”).
- **Columns**: Expected outputs, side effects, and interactions with collaborators (repositories, data sources, etc.).

Use this matrix as the **source of truth** for test cases.

### 3.3 Coverage Targets

- **Kotlin**
  - Line & branch coverage ≥ **90%** on logic-heavy code.
  - All branches of `when` expressions and conditionals exercised.
  - All error paths tested (including exception throwing and domain error types).
- **Swift**
  - Line coverage ≥ **80–90%** for critical modules.
  - All enum cases and associated-value paths tested.
- Optionally, track **mutation testing scores** where supported (e.g. PIT for JVM).

### 3.4 Git Actions

- Create or update a feature branch if not already present:
  - Example: `feature/payments-support-refunds`.
  - If only tests are being added for an existing SUT: `test/<module>-<sut>` (e.g. `test/billing-ApplyDiscountUseCase`).

---

## 4. Phase 2 – Test Environment & Determinism

### 4.1 Isolation Strategy

- No real network, database, system clock, or filesystem.
- Use:
  - **Fakes** (in-memory implementations) for complex dependencies where behaviour matters.
  - **Mocks/spies** (MockK/Mockito/XCTest spies) for verifying interactions, not behaviour.

### 4.2 Time & Randomness

- **Kotlin**
  - Abstract time behind an interface (e.g. `Clock`) and provide a test implementation.
  - Use fixed seeds for randomness or inject a `Random`/generator abstraction.
  - For coroutines:
    - Use `StandardTestDispatcher` or `UnconfinedTestDispatcher`.
    - Wrap tests in `runTest { ... }` with a test `CoroutineScope`.
- **Swift**
  - Inject `DateProvider`/`Clock` abstractions.
  - Provide deterministic random-number providers for tests.

### 4.3 KMP Test Targets

- Use **`commonTest`** for:
  - Pure domain logic, mappers, and platform-agnostic repositories/use cases.
- Use **platform-specific tests** only when:
  - Verifying integration with platform APIs.
  - Testing platform-specific actual implementations.

### 4.4 Git Actions

- Introduce or update **test utilities** (factory methods, mother/faker objects, test doubles) in dedicated test packages/modules.
- Commit these utilities separately from SUT-focused tests when they are reusable.

---

## 5. Phase 3 – Test Design (GIVEN–WHEN–THEN)

### 5.1 Structure

Every unit test must explicitly follow GIVEN–WHEN–THEN:

- **GIVEN** – Context and preconditions:
  - SUT instantiation.
  - Collaborator setup (fakes/mocks).
  - Input data creation.
- **WHEN** – Action under test:
  - Single method call or event.
- **THEN** – Assertions:
  - Returned values, state changes, emitted events, and collaborator interactions.

### 5.2 Naming Conventions

- **Kotlin (JUnit)**
  - Test function names:
    - Back-ticked descriptive names:
      - `` `GIVEN valid input WHEN apply discount THEN returns reduced total` ``.
    - Or `givenValidInput_whenApplyDiscount_thenReturnsReducedTotal`.
- **Swift (XCTest)**
  - `test_GIVEN_validInput_WHEN_applyDiscount_THEN_returnsReducedTotal()`
  - Or BDD-style if using Quick/Nimble:
    - `describe("ApplyDiscountUseCase") { context("valid input") { it("returns reduced total") { ... } } }`

### 5.3 Test Layout

- Arrange test body in clearly separated blocks (comments are acceptable if needed):
  - `// GIVEN`
  - `// WHEN`
  - `// THEN`
- Avoid more than one “WHEN” per test; if multiple actions are needed, ensure the test still has a single logical assertion of behaviour.

---

## 6. Phase 4 – TDD Loop per SUT

### 6.1 Red – Write the Failing Test

1. Pick a single scenario from the test matrix.
2. Write a test in **GIVEN–WHEN–THEN** form that:
   - Compiles with the current SUT API (or the API you intend to create).
   - Fails because the behaviour is not yet implemented or is incomplete.
3. Use mocks/fakes only as needed for this scenario; avoid generalisation too early.

### 6.2 Green – Implement Minimal Behaviour

1. Implement just enough code in the SUT to make the new test pass.
2. Keep implementation straightforward; avoid premature optimisation.
3. Ensure:
   - All tests (existing + new) pass locally.
   - No flakiness (run multiple times as needed).

### 6.3 Refactor – Improve Design

1. Apply refactorings:
   - Extract methods, rename symbols, remove duplication.
   - Introduce internal helpers or test utility functions where needed.
2. Ensure tests remain green after refactor.
3. Keep **test refactors** separate from **behavioural changes** in Git history where practical.

### 6.4 Git Actions

- Typical commit sequence per SUT:
  - `test: add failing tests for <SUT>` (optional if team allows temporarily failing commits only locally).
  - `feat: implement <SUT> behaviour to satisfy tests`.
  - `refactor: clean up <SUT> and tests`.
- Before pushing:
  - Ensure **all tests pass** locally.
  - Optionally rebase/squash to produce a small, coherent commit history.

---

## 7. Layer-Specific Guidelines

### 7.1 Use Cases / Interactors

- Test:
  - Happy paths for all major scenarios.
  - Validation rules and preconditions.
  - Interaction with repositories and other domain services.
  - Mapping from data layer errors/exceptions to domain errors or result types.
- Use:
  - Fakes for repositories when behaviour is complex.
  - Mocks/spies to verify that dependencies are called correctly (`verify` in MockK/Mockito; expectations in XCTest).

### 7.2 Repositories

- Test:
  - Correct mapping between domain models and DTOs/entities.
  - Error mapping from network/storage failures to domain errors.
  - Cache policies: hits, misses, invalidation rules.
  - Ordering and deduplication logic.
- Use:
  - Fakes for data sources where full-stack integration is not needed.
  - MockK/Mockito for verifying collaboration with HTTP clients or DAOs.

### 7.3 Domain Models & Value Objects

- Test:
  - Invariants and validation rules.
  - Derived properties and computed fields.
  - Equality/`hashCode`/`Comparable` semantics where used.
- Ensure:
  - Constructors and factory functions reject invalid states deterministically.

### 7.4 Mappers & Translators

- Test:
  - Round-trip conversions when applicable (domain ↔ DTO).
  - Handling of missing/optional fields and defaults.
  - Backwards/forwards compatibility behaviour where needed.

---

## 8. Determinism, Flakiness & Performance

### 8.1 Determinism

- Avoid:
  - Real network calls.
  - Real clocks.
  - Global mutable state.
- Ensure:
  - Each test method sets up its entire context.
  - Mocks are reset between tests.

### 8.2 Flakiness Detection

- Run tests repeatedly (locally or via CI “stress” runs) to ensure stable results.
- In Kotlin, treat reliance on `Dispatchers.Main` or real threading as a code smell in unit tests; abstract and inject dispatchers.
- In Swift, watch for async tasks that are not properly awaited or synchronised in tests.

### 8.3 Performance

- Keep individual unit tests fast (ideally **< 100 ms**; rarely > 500 ms).
- Avoid heavy object graphs or integration-like behaviour in unit tests.
- Use parameterised tests (JUnit 5, Kotest, XCTest data-driven patterns) when multiple inputs share the same structure.

---

## 9. Review, Maintenance & CI

### 9.1 Code Review

- Ensure reviewers check:
  - GIVEN–WHEN–THEN clarity and correct use.
  - Test cases covering all scenarios in the test matrix.
  - Absence of duplicated logic in tests (use builders/mothers/factories).
  - Clear, descriptive test names.

### 9.2 Test Refactoring

- Regularly:
  - Extract common test data builders (e.g. `UserMother.validUser()`).
  - Move complex setup into helper functions or base classes where justified.
  - Remove obsolete tests when behaviour is intentionally changed and re-specified.

### 9.3 CI Integration

- Ensure:
  - All unit tests (Kotlin + Swift) run on every push/PR.
  - Failing tests block merges.
  - Coverage reports are generated and visible.
  - Optionally, mutation testing or static analysis is part of the pipeline for critical modules.

### 9.4 Git Actions

- For any change to tests or SUT:
  - Commit message should clearly indicate whether it’s:
    - `test:` (tests only),
    - `feat:` / `fix:` (behavioural change),
    - `refactor:` (no behaviour change).
- Keep PRs **narrow** and **focused**:
  - Avoid mixing large refactors with new behaviours.

---

## 10. Time Estimation & KPIs

### 10.1 Time Estimates (Per SUT)

- Test planning & matrix: **15–30 minutes**.
- Environment/setup & utilities (once per module): **10–30 minutes**.
- TDD cycles (per scenario): **5–15 minutes** depending on complexity.
- Review & refactor: **15–30 minutes**.

### 10.2 Assessment Indicators

- **Coverage**
  - Line/branch coverage vs. target thresholds.
  - Mutation testing score where available.
- **Quality**
  - Number of bugs caught in unit tests vs. higher levels (integration, QA, production).
  - Flakiness rate (should trend to zero).
  - Readability scores from peer feedback.
- **Flow**
  - Lead time from test design to stable passing suite.
  - CI success rate for test-related changes.

---

## 11. References & Further Reading

- **Kotlin**
  - JUnit 5 User Guide.
  - Kotlin Coroutines Test documentation.
  - MockK / Mockito-Kotlin documentation.
- **Swift**
  - XCTest documentation.
  - Quick/Nimble (if adopted).
- General:
  - GIVEN–WHEN–THEN / BDD literature.
  - Articles and books on TDD and unit testing best practices.

This workflow should be applied consistently across KMP Kotlin and Swift logic to keep unit tests reliable, expressive, and aligned with business-critical requirements.
