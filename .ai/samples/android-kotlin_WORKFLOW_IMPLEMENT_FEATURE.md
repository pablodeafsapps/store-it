# Workflow: Implementing a New Feature in Android/Kotlin Projects

## 1. Goal

Establish a rigorous, repeatable workflow for implementing new features in Android/Kotlin projects. This workflow ensures:

- High code quality and maintainability
- Test-driven development (TDD) and continuous integration
- Proper architectural alignment (Clean Architecture, MVVM, modularization)
- Traceability and collaboration via version control (Git)

---

## 2. Prerequisites

- **Development environment**: Android Studio (latest stable), JDK 17+, Gradle, Kotlin plugin
- **Project setup**: Modularized structure, version catalog, DI framework (Koin/Hilt), CI pipeline
- **Access**: Git repository with appropriate permissions, code review process in place
- **Documentation**: Up-to-date architecture, conventions, and API specs
- **Issue tracking**: Feature ticket with acceptance criteria, user stories, and design assets (if applicable)

---

## 3. Workflow Phases & Steps

### Phase 1: Planning & Design

1. **Understand requirements**: Review feature ticket, user stories, and acceptance criteria
2. **Domain modeling**: Identify new/impacted domain entities, value objects, and use cases
3. **API & contract design**: Define interfaces, DTOs, and public APIs (KDoc all new contracts)
4. **Dependency analysis**: Determine module boundaries and dependencies (update build files if needed)
5. **Create feature branch**: `git checkout -b feature/<short-description>`

### Phase 2: Test-Driven Development (TDD)

1. **Write failing unit tests**: For new use cases, domain logic, and repository contracts
2. **Write integration tests**: For repository composition, data sources, and external APIs
3. **Write UI tests**: For ViewModel state, UI flows, and user interactions (Compose, Espresso, etc.)
4. **Commit tests**: `git add . && git commit -m "test: add failing tests for <feature>"`

### Phase 3: Implementation

1. **Implement domain logic**: Use cases, entities, and value objects (pure Kotlin, no framework dependencies)
2. **Implement data layer**: Repository implementations, data sources, mappers, DTOs
3. **Implement presentation layer**: ViewModels, UI state, Composables, navigation
4. **Wire up DI**: Register new types in DI modules (Koin/Hilt)
5. **Refactor as needed**: Maintain SOLID, DRY, and KISS principles
6. **Commit implementation**: `git add . && git commit -m "feat: implement <feature> domain/data/presentation"`

### Phase 4: Validation & Review

1. **Run all tests**: Unit, integration, and UI tests (local and CI)
2. **Static analysis**: Lint, detekt, ktlint, and code coverage tools
3. **Peer review**: Open a pull request (PR) with clear description, link to ticket, and screenshots (if UI)
4. **Address feedback**: Iterate on PR based on reviewer comments
5. **Commit fixes**: `git add . && git commit -m "fix: address PR feedback for <feature>"`

### Phase 5: Release & Retrospective

1. **Merge PR**: Squash and merge to main/develop branch
2. **Tag release**: If feature is release-worthy, create a Git tag (e.g., `v1.2.0-feature-xyz`)
3. **Update documentation**: API docs, changelog, and user guides
4. **Monitor metrics**: Track crash reports, analytics, and performance KPIs post-release
5. **Retrospective**: Document lessons learned and update workflow if needed

---

## 4. Checkpoints & Quality Gates

- **Code review**: All code must be reviewed and approved by at least one other engineer
- **Test coverage**: ≥80% for new/changed code (unit + integration)
- **Static analysis**: No critical lint/detekt/ktlint issues
- **CI pipeline**: All builds and tests must pass before merge
- **Documentation**: All new public APIs and modules must be documented (KDoc, markdown)
- **No direct commits to main/develop**: All changes via feature branches and PRs

---

## 5. KPIs & Assessment Indicators

- **Lead time**: Time from ticket start to PR merge
- **Code review turnaround**: Time from PR open to approval/merge
- **Test coverage delta**: % increase in coverage for affected modules
- **Defect rate**: Bugs reported post-release for the feature
- **Build stability**: CI pass rate for feature branch and after merge
- **Cycle time**: Time from first commit to production deployment

---

## 6. Time Estimation Guidelines

- **Small feature** (1-2 screens, no new APIs): 1-3 days
- **Medium feature** (multi-screen, new use cases, moderate data/API work): 3-7 days
- **Large feature** (cross-module, new APIs, complex flows): 1-3 weeks

> **Tip:** Always buffer for code review, CI, and documentation. Adjust estimates based on team velocity and feature complexity.

---

## 7. Git Command Reference

- **Create feature branch**: `git checkout -b feature/<short-description>`
- **Stage & commit**: `git add . && git commit -m "<type>: <message>"`
- **Rebase with main**: `git fetch origin && git rebase origin/main`
- **Push branch**: `git push origin feature/<short-description>`
- **Open PR**: Use GitHub/GitLab/Bitbucket UI
- **Squash & merge**: Prefer squash merges for clean history
- **Tag release**: `git tag vX.Y.Z-feature-xyz && git push origin --tags`

---

## 8. References

- [Google Android Developer Guides](https://developer.android.com/guide)
- [Kotlin Language Documentation](https://kotlinlang.org/docs/home.html)
- [Clean Architecture](https://8thlight.com/blog/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Test-Driven Development by Example](https://www.amazon.com/Test-Driven-Development-Kent-Beck/dp/0321146530)
- [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)
- [Jetpack Compose Testing](https://developer.android.com/jetpack/compose/testing)

---

**Document Version**: 1.0  
**Last Updated**: November 18, 2025  
**Audience**: Senior Android/Kotlin Engineers
