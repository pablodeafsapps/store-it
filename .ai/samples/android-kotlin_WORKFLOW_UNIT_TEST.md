# Unit Test Workflow for Android/Kotlin Logic Files

## Goal

Document a rigorous, repeatable workflow for designing, implementing, and validating unit tests for any logic-bearing Kotlin/Android file (e.g., use cases, repositories, domain models). The workflow ensures high coverage, deterministic results, and maintainable test suites for business-critical logic.

## Prerequisites

- **Source File**: Target logic file (e.g., use case, repository, domain entity) with well-defined responsibilities.
- **Test Framework**: JUnit 4/5, Kotlinx Coroutines Test, Mockito (or equivalent mocking library).
- **Build System**: Gradle configured for JVM unit tests (not instrumented tests).
- **Dependency Isolation**: All external dependencies must be mockable or injectable.
- **Domain Contracts**: Clear interface boundaries for domain logic and data sources.
- **Test Data Factories**: Utilities for generating valid/invalid domain objects and DTOs.

## Workflow Phases

### Phase 1 — Test Planning & Coverage Analysis

- **Identify Core Logic**: Enumerate all public methods, business rules, and error paths in the target file.
- **Define Test Matrix**: For each method, list input scenarios, edge cases, and expected outcomes.
- **Coverage Targets**:
  - Branch coverage ≥ 90%
  - All error paths exercised
  - DTO ↔ Domain mapping verified
  - Caching, side effects, and concurrency tested (if applicable)

### Phase 2 — Test Environment Setup

- **Mock Dependencies**: Use Mockito or test doubles for all collaborators (e.g., remote data sources, caches).
- **Coroutine Context**: Use `runTest` and test dispatchers for coroutine-based logic.
- **Factory Methods**: Centralize test data creation for maintainability.
- **Isolation**: Ensure each test is independent (no shared mutable state).

### Phase 3 — Test Implementation

- **GIVEN-WHEN-THEN Pattern**:
  - *GIVEN*: Initial context and preconditions; set up mocks, test data, and the SUT (System Under Test).
  - *WHEN*: Execute the action under test (invoke the method, trigger the event, or mutate state).
  - *THEN*: Assert expected outcomes, state changes, side effects, and collaborator interactions.
- **Naming Convention**: Use descriptive test names (GIVEN/WHEN/THEN or should/when).
- **Error Simulation**: Explicitly test network, server, and client error scenarios.
- **Concurrency & Caching**: Validate cache persistence, invalidation, and thread safety.
- **Edge Cases**: Test zero, negative, and large values for domain entities.

### Phase 4 — Checkpoints & KPIs

- **Checkpoints**:
  - All branches and error paths covered
  - All mocks verified for expected interactions
  - No test pollution (tests do not affect each other)
  - Deterministic results (no flakiness)
- **KPIs**:
  - Branch/line coverage (≥ 90%)
  - Mutation score (if using mutation testing)
  - Test execution time (< 500ms per test)
  - Readability and maintainability (no magic values, clear intent)

### Phase 5 — Assessment & Review

- **Peer Review**: All test code must be reviewed by at least one other engineer.
- **Refactor for Clarity**: Remove duplication, extract reusable test utilities.
- **Documentation**: Inline comments for non-obvious logic, rationale for complex scenarios.
- **Continuous Integration**: Tests must pass in CI pipeline with no manual intervention.

## Time Estimation

- **Test Planning**: 15–30 min per logic file
- **Test Environment Setup**: 10–20 min (once per module)
- **Test Implementation**: 5–10 min per scenario (complex logic may require more)
- **Review & Refactor**: 10–20 min per file
- **Total**: 1–2 hours per logic file (typical)

## Example Assessment Indicators

- **Logic Coverage**: All business rules, error paths, and edge cases are tested.
- **Determinism**: Tests produce the same result on every run.
- **Isolation**: No shared state between tests; mocks reset per test.
- **Maintainability**: Test code is readable, DRY, and easy to extend.
- **Performance**: Test suite runs quickly (< 1s per file).

## 6. References

## Examples

The following concise Kotlin snippets illustrate the recommended GIVEN/WHEN/THEN structure and common patterns for logic tests (use cases, repositories, mappers). These are framework-agnostic examples suitable for local JVM unit tests.

- Example — Use case (coroutine-based) — GIVEN/WHEN/THEN with `runTest`:

```kotlin
package com.example.tests

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

// Minimal domain types and SUT to make the snippet copy-paste runnable
data class Tariff(val pricePerKilometer: Double, val pricePerSecond: Double)
data class LocationPoint(val lat: Double = 0.0, val lon: Double = 0.0)
data class Journey(val route: List<LocationPoint>, val luggageCount: Int, val tariff: Tariff)
data class Fare(val distancePrice: Double, val timePrice: Double, val luggagePrice: Double) {
  val totalPrice: Double get() = distancePrice + timePrice + luggagePrice
}

class CalculateFareUseCase {
  operator fun invoke(input: Journey): Result<Fare> {
    // Simple deterministic logic for example purposes
    val distanceKm = if (input.route.size >= 2) 11.0 else 0.0
    val distancePrice = distanceKm * input.tariff.pricePerKilometer
    val timePrice = 1800 * input.tariff.pricePerSecond // example: 30 minutes
    val luggagePrice = input.luggageCount * 5.0
    return Result.success(Fare(distancePrice, timePrice, luggagePrice))
  }
}

class CalculateFareUseCaseTest {
  @Test
  fun `GIVEN valid route WHEN calculate fare THEN returns expected components`() = runTest {
    val tariff = Tariff(pricePerKilometer = 0.2, pricePerSecond = 0.01)
    val startPoint = LocationPoint()
    val endPoint = LocationPoint()
    val route = listOf(startPoint, endPoint)
    val journey = Journey(route = route, luggageCount = 1, tariff = tariff)
    val sut = CalculateFareUseCase()

    val result = sut(input = journey)

    val fare = result.getOrNull() ?: error("Expected OK result")
    assertTrue(fare.totalPrice >= 0)
    assertEquals(5.0, fare.luggagePrice, 0.01)
  }
}
```

- Quick guidelines for examples:
  - Keep GIVEN setup minimal and explicit — prefer factories like `TestFactories.routeOf(distanceKm = 1.0)`.
  - Use `whenever(...).thenReturn(...)` or dedicated fakes for deterministic behavior.
  - Name tests with `GIVEN/WHEN/THEN` phrases to make intent clear and searchable.
  - Verify interactions (`verify(...)`) for side effects only; prefer testing state for pure functions.
  - [Google Testing Blog: Test Engineering Best Practices](https://testing.googleblog.com/)
  - [Kotlin Coroutines Test Guide](https://kotlinlang.org/docs/coroutines-test.html)
  - [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
  - [Mockito-Kotlin Documentation](https://github.com/mockito/mockito-kotlin)

---

*This workflow is intended for use by experienced Android engineers seeking to maximize reliability, coverage, and maintainability of unit tests for any logic-centric Kotlin file.*
