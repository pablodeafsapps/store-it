<!--
  Sync Impact Report
  Version change: (none) → 1.0.0
  Modified principles: N/A (initial fill)
  Added sections: N/A
  Removed sections: N/A
  Templates: plan-template.md ✅ (Constitution Check references constitution file);
    spec-template.md ✅ (no mandatory sections changed);
    tasks-template.md ✅ (task types align with documentation + test principles);
    commands: .specify/templates/ has no commands/ subdir; .claude/commands/ not constitution-coupled.
  Follow-up TODOs: None. RATIFICATION_DATE set to first adoption date.
-->

# Store it! Constitution

## Core Principles

### I. User Experience First

The product MUST be easy to use: UX is highly optimised so users can start using it with no previous training. Every feature MUST be designed for clarity, minimal cognitive load, and discoverability. Complexity that does not serve the user is out of scope.

**Rationale**: "Store it!" is defined as an easy-to-use application; this principle makes that a non-negotiable standard for design and implementation.

### II. Full Documentation

Code MUST be fully documented. Public APIs, modules, and non-obvious behaviour MUST have concise, accurate documentation (e.g. KDoc, README, or equivalent). Documentation MUST be kept in sync with code changes.

**Rationale**: Ensures maintainability, onboarding, and safe evolution; supports compliance and auditability.

### III. Test Coverage (NON-NEGOTIABLE)

Code MUST be properly covered by tests. Business-critical and user-facing behaviour MUST have automated tests (unit, integration, or E2E as appropriate). New behaviour MUST not be merged without corresponding tests. Test quality and stability (e.g. no flakiness) are part of this requirement.

**Rationale**: Prevents regressions, enables confident refactoring, and aligns with the project’s quality bar.

### IV. Simplicity

Prefer the simplest design that satisfies requirements (YAGNI). Avoid unnecessary abstraction, indirection, or scope creep. When in doubt, choose the option that is easier to read, test, and change.

**Rationale**: Supports User Experience First and Full Documentation by keeping the codebase understandable and documentable.

### V. Quality Gates & Compliance

All changes MUST comply with this constitution. Pull requests and reviews MUST verify adherence to Principles I–IV. Exceptions or violations MUST be explicitly justified and documented (e.g. in plan complexity tracking). Regular compliance checks (e.g. doc coverage, test coverage, UX review) are expected.

**Rationale**: Makes the constitution enforceable and keeps quality consistent over time.

## Additional Constraints

- **Technology**: Align with the project’s chosen stack (e.g. Kotlin Multiplatform, Android, iOS) and with architecture guidance (e.g. `.ai/AGENTS.md`, `.ai/CONVENTIONS.md`) where present.
- **Security & data**: Sensitive data and credentials MUST follow platform best practices; no secrets in code or in unencrypted storage.
- **Accessibility**: UX decisions MUST consider accessibility so the app remains usable for people with disabilities, in line with “User Experience First”.

## Development Workflow

- **Before merge**: Code review MUST confirm documentation and test coverage for changed code; UX-impacting changes SHOULD be reviewed for Principle I.
- **Definition of done**: A change is done when it meets acceptance criteria, is documented, has appropriate tests, and passes the constitution check.
- **Constitution check**: Plans and feature specs SHOULD include a constitution check (e.g. in implementation plans) that verifies Principles I–V for the scope of the work.

## Governance

- This constitution supersedes conflicting local or ad-hoc practices for this project.
- **Amendments**: Require a documented proposal, rationale, and (where applicable) a short migration note. Version MUST be updated per semantic versioning (MAJOR: incompatible principle change; MINOR: new principle or material expansion; PATCH: clarifications, typos).
- **Compliance**: All contributors and agents MUST follow this constitution; reviewers MUST flag violations. Use `.ai/AGENTS.md` and `.ai/CONVENTIONS.md` for development and coding guidance.

**Version**: 1.0.0 | **Ratified**: 2025-02-10 | **Last Amended**: 2025-02-10
