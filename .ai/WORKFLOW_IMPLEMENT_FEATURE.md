# Workflow: Implementing a New Feature (Android/Kotlin)

This document defines a **Git-centric, TDD-first workflow** for implementing any new feature in an Android/Kotlin codebase, from idea to production. It is aimed at experienced engineers and assumes familiarity with modern Android/Kotlin tooling, CI/CD, and code review practices.

---

## 1. Goals & Principles

- **Goal**: Deliver a new feature that is:
  - Correct and resilient (backed by automated tests).
  - Consistent with existing architecture and conventions.
  - Observable and measurable via well-defined KPIs.
  - Easy to evolve and maintain.
- **Guiding principles**:
  - Favour **TDD** (red → green → refactor) at both unit and integration levels.
  - Respect **Clean Architecture** and existing project boundaries.
  - Maintain a **clean Git history** (small, meaningful commits; feature branches; code review).
  - Keep feature scope **vertically sliced** (end-to-end behaviour over horizontal layers).

---

## 2. Prerequisites

- **Product & requirements**
  - Feature description, including user story(ies) and business context.
  - Acceptance criteria and non-functional requirements (performance, security, accessibility, etc.).
  - Defined KPIs/metrics (e.g. usage, error rate, latency, conversion).
- **Architecture & conventions**
  - Understanding of the project’s architecture (layers/modules, DI strategy, navigation patterns).
  - Adherence to established coding conventions and design guidelines.
- **Environment**
  - Local environment configured (JDK, Android SDK, emulators, required CLIs).
  - CI pipeline in place for running tests and static analysis.
- **Version control**
  - Git remote configured.
  - Permissions to create branches and open pull/merge requests.

---

## 3. High-Level Phases

1. **Discovery & Definition**
2. **Design & Breakdown**
3. **TDD Implementation Loop**
4. **Integration & Hardening**
5. **Review, Merge & Release Preparation**
6. **Post-Release Monitoring & Iteration**

Each phase has explicit **checkpoints**, **KPIs**, and suggested **time allocation**.

---

## 4. Phase 1 – Discovery & Definition

### 4.1 Actions

- Clarify the feature:
  - Define primary and secondary user journeys.
  - Identify edge cases and error scenarios.
- Capture **acceptance criteria** as concrete examples (Given/When/Then or similar).
- Define **KPIs**:
  - Examples: feature adoption rate, success rate of a flow, latency budget, crash-free sessions, error rate thresholds.
- Identify **dependencies**:
  - Backend contracts (APIs), other teams, feature flags, external services.

### 4.2 Checkpoints

- User stories and acceptance criteria documented.
- Dependencies, assumptions, and constraints listed.
- KPIs defined and measurable (you know where/how they will be recorded).

### 4.3 Time & KPIs

- **Suggested time**: 10–20% of feature effort.
- **KPIs**:
  - Number of open questions at implementation start (should be minimal).
  - Degree of testability of acceptance criteria (each criterion should be testable).

---

## 5. Phase 2 – Design & Breakdown

### 5.1 Actions

- Map the feature onto **Clean Architecture**:
  - Domain: new use cases, entities/value objects, domain services.
  - Data: repositories, data sources, DTOs, mappers.
  - Presentation/UI: ViewModels/state holders, UI components, navigation changes.
- Draw a **sequence diagram** or flow for key interactions, including error paths.
- Break feature into **vertical slices**:
  - Prefer small increments that run end-to-end (from UI trigger to persistence/network and back).
- Decide **testing strategy**:
  - Unit tests for domain and data.
  - Integration tests (e.g. repository with real DB/network test double).
  - UI tests (instrumented, snapshot, or robotics-style tests) where value is high.

### 5.2 Git Actions

- Create a feature branch from the main integration branch:
  - Example: `feature/<area>-<short-description>` (e.g. `feature/auth-login-flow`).
- Optionally create sub-branches for large work, merging back into the feature branch.

### 5.3 Checkpoints

- Architecture impact is understood (modules/classes to touch or create are identified).
- Feature is decomposed into 2–6 vertical slices.
- Testing levels and coverage goals are explicit.

### 5.4 Time & KPIs

- **Suggested time**: 10–15% of feature effort.
- **KPIs**:
  - Number of slices with clear acceptance criteria and test strategy.
  - Absence of cross-layer violations (no UI depending directly on Data/infra).

---

## 6. Phase 3 – TDD Implementation Loop

This is the core implementation work. For each vertical slice, follow a tight **TDD cycle**.

### 6.1 Per-Slice TDD Steps

For each slice:

1. **Write/extend tests first**:
   - Start at the **lowest level with the most stable contract** (often domain use cases).
   - Write failing unit tests that encode the acceptance criteria (red).
2. **Minimal implementation**:
   - Implement just enough production code to make the tests compile and fail meaningfully, then pass (green).
   - Do not optimise prematurely; preserve clarity.
3. **Refactor**:
   - Clean up code, remove duplication, improve names.
   - Maintain passing tests (refactor-only commits).
4. **Promote upwards**:
   - After domain and data are in place, add/extend tests for presentation and UI (e.g. ViewModel tests).
   - Wire UI and navigation last, backed by tests where suitable.

Repeat this cycle until the slice is complete, then move to the next slice.

### 6.2 Testing Layers

- **Domain tests**:
  - Pure unit tests (no Android/IO dependencies).
  - Validate business rules and invariants.
- **Data tests**:
  - Repository tests using fakes/mocks for data sources, or integration tests with real DB/HTTP test doubles.
- **Presentation tests**:
  - ViewModel/state tests: assert state transitions and side effects in response to events.
- **UI tests (optional, value-based)**:
  - Automated end-to-end checks for critical flows (happy path + critical error paths).

### 6.3 Git Actions

- Commit **frequently** in small increments:
  - Separate commits for:
    - New tests (may fail temporarily if agreed in team conventions).
    - Implementations that make tests pass.
    - Pure refactors (no behaviour change).
- Use descriptive commit messages:
  - Example: `feat: add login use case`, `test: cover invalid password scenarios`, `refactor: extract token mapper`.
- Rebase regularly on the integration branch or keep feature branch up to date using the team’s preferred strategy.

### 6.4 Checkpoints

- All new code is covered by tests at appropriate layers.
- Feature slices are demonstrable in isolation (behind flag or dedicated entry point if needed).

### 6.5 Time & KPIs

- **Suggested time**: 40–60% of feature effort.
- **KPIs**:
  - Test coverage delta (lines/branches, but also critical path coverage).
  - Number of tests added vs. lines of production code.
  - Build stability (no new flaky tests introduced).

---

## 7. Phase 4 – Integration & Hardening

### 7.1 Actions

- Run full **test suites**:
  - Unit, integration, UI, and any contract or snapshot tests.
- Integrate with:
  - Navigation and routing.
  - Analytics/telemetry (events for KPIs).
  - Feature flags (if used).
- Validate **backwards compatibility**:
  - API stability (public interfaces, shared modules).
  - Data migrations (schema changes, preference keys).
- Perform **performance checks**:
  - Measure latency for critical paths.
  - Verify memory allocations and avoid regressions where applicable.

### 7.2 Git & CI Actions

- Ensure CI pipeline passes:
  - Lint, static analysis, unit tests, instrumentation tests (where configured).
- Fix or refactor until CI is green.
- Optionally squash or reorganise commits locally before opening a PR, depending on team policy.

### 7.3 Checkpoints

- CI green on feature branch.
- No new critical or high-severity static analysis findings without justification.
- Manual exploratory testing of the main user flows completed.

### 7.4 Time & KPIs

- **Suggested time**: 10–20% of feature effort.
- **KPIs**:
  - CI success rate for feature branch.
  - Time-to-fix for test or lint failures.
  - Number of found vs. unresolved defects in this phase.

---

## 8. Phase 5 – Review, Merge & Release Preparation

### 8.1 Actions

- Open a **pull request** against the integration branch with:
  - Clear title and concise, technical description.
  - Summary of the change, affected modules, and any risks.
  - Testing notes: test types executed and results (including manual checks).
- Participate in **code review**:
  - Address comments with focused follow-up commits.
  - Keep the diff small by slicing features; avoid “mega-PRs”.
- Perform final checks:
  - Verify that logging/analytics are correct and not overly verbose.
  - Confirm feature flags are correctly wired (if applicable).

### 8.2 Git Actions

- Optional: squash commits before merge per team preference (rebase + squash vs. merge commit).
- Ensure branch is up to date with integration branch before merge (rebase or merge).
- Merge only when:
  - All required reviews are approved.
  - All required checks (CI, quality gates) are green.

### 8.3 Checkpoints

- PR approved and merged without unexpected rollbacks.
- Build artefact for release candidate generated (as per pipeline).

### 8.4 Time & KPIs

- **Suggested time**: 5–15% of feature effort.
- **KPIs**:
  - Review turnaround time.
  - Number of review cycles required.
  - Post-merge defect rate (issues detected shortly after merge).

---

## 9. Phase 6 – Post-Release Monitoring & Iteration

### 9.1 Actions

- Monitor **runtime metrics** and KPIs:
  - Crash/ANR rate and error logs.
  - Feature adoption and funnel conversion.
  - Latency and resource usage.
- Collect **feedback**:
  - Internal QA, dogfooding, beta users, production usage patterns.
- Plan **follow-up work**:
  - Bug fixes, UX polish, optimisations, or incremental scope.

### 9.2 Git Actions

- Use hotfix or patch branches for critical issues:
  - Example: `hotfix/<area>-<issue>` or equivalent naming.
- Continue using TDD for fixes to prevent regressions:
  - Reproduce issue with a failing test → fix → refactor.

### 9.3 Checkpoints

- KPIs meet or exceed targets, or deviations are understood and tracked.
- No critical production issues attributable to the feature remain open.

### 9.4 Time & KPIs

- **Suggested time**: 5–10% of feature effort (initial post-release window).
- **KPIs**:
  - Time-to-detect and time-to-resolve production issues.
  - Defect density related to this feature.

---

## 10. Summary of Key KPIs

Across the lifecycle, typical indicators include:

- **Quality**
  - Test coverage on new/changed code.
  - Number and severity of defects (pre- and post-release).
  - Flakiness rate of new tests.
- **Flow & Efficiency**
  - Lead time from feature definition to merge.
  - Cycle time per slice (idea → merged).
  - Review turnaround time.
- **Runtime Behaviour**
  - Crash-free sessions for feature-related code paths.
  - Latency and resource usage within agreed budgets.
  - Adoption and successful completion rate of the new flow.

These should be tailored to the specific team and product, but every feature implementation should aim to improve or at least preserve these indicators.
