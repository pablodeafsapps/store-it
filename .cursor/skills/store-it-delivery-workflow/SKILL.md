---
name: store-it-delivery-workflow
description: >-
  Updates Trello (Doing / AI-Implemented), runs verification builds and tests for
  Android and shared KMP code, and leaves changes commit-ready. Use when
  starting or finishing feature work, bugfixes, or refactors in this
  repository, or when the user mentions Trello, task cards, or handoff for
  commit.
---

# Store It delivery workflow

## Before coding

1. Use the **Trello MCP**: move the related card to **Doing**.
2. Add a short comment on the card if context helps (scope, branch, blockers).

## While coding

- Comment on the card when something important changes (scope cut, dependency, follow-up).

## After coding (before marking work complete)

1. **Build and test** (run from repo root; JDK 17):

   ```bash
   ./gradlew detekt :composeApp:allTests :androidApp:testDebugUnitTest :androidApp:assembleDebug --no-daemon
   ```

   Align with CI (`.github/workflows/build-and-test.yml`). Fix failures before handoff.

2. **iOS** (on macOS, when shared Kotlin or framework integration changed): confirm `iosApp` still builds — open `iosApp/iosApp.xcodeproj` in Xcode and build, or run `xcodebuild` with a **Simulator** destination that exists on the machine (`xcrun simctl list devices available`). If the change is Android-only, say so in the Trello comment and skip iOS when appropriate.

3. **Trello MCP**: move the card to **AI-Implemented** and add a brief comment (what changed, commit message suggestion, tests run).

## Commit readiness

- Files should be in a state the user can commit with a **clear, descriptive message** (user commits and pushes; user moves card to **Done**).

## Trello column names

- **Doing** → work in progress  
- **AI-Implemented** → ready for user commit/review  
- **Done** → user’s responsibility after push
