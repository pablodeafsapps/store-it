# Store It Project Instructions

Apply these instructions for work in this repository.

## Architecture

- This is a Kotlin Multiplatform app with `:composeApp` as the shared KMP module and `:androidApp` as the Android app module. There is no separate `:shared` module.
- Maximise code in `composeApp/src/commonMain`. Keep `androidMain`, `iosMain`, `androidApp`, and `iosApp` thin and limited to platform-specific integration.
- Shared business logic, repositories, use cases, shared presentation state, DI setup, and `expect` declarations belong in `commonMain`.
- `androidMain` and `iosMain` should only contain `actual` implementations and minimal platform services. Do not put platform UI there.
- Android UI, app entry, and navigation belong in `:androidApp`. iOS UI belongs in `iosApp`.
- Respect dependency direction: UI -> Presentation -> Domain -> Data -> Platform APIs.

## Koin And ViewModels

- Android Koin setup is owned by `StoreItApplication` in `:androidApp`, using `AndroidModule` as the composition root.
- iOS Koin setup starts from `KoinInitKt.doInitKoinIos()` and uses shared `AppModule` bindings.
- Shared ViewModels live in `commonMain` and are based on `StoreItViewModel`.
- Do not reintroduce a shared iOS `CoroutineScope`. Each iOS ViewModel instance owns its scope through the `StoreItViewModel` iOS actual.
- For parameterised ViewModels, pass only business parameters such as `rackId`, not a coroutine scope.

## KMP Placement

- Follow the `kmp-source-set-placement` skill when adding or moving Kotlin Multiplatform code.
- Preferred skill path:
  - `.cursor/skills/kmp-source-set-placement/SKILL.md`
- Fallback skill path:
  - `/Users/pablo/.codex/skills/kmp-source-set-placement/SKILL.md`

## Delivery workflow

When starting or finishing substantive work in this repository, follow the `store-it-delivery-workflow` skill.

Preferred skill path:
- `.cursor/skills/store-it-delivery-workflow/SKILL.md`

Fallback skill path:
- `/Users/pablo/.codex/skills/store-it-delivery-workflow/SKILL.md`

This includes:
- updating the related Trello card through the expected workflow,
- running the Gradle verification steps before handoff,
- confirming the iOS build when shared Kotlin or framework integration changed,
- leaving changes in a commit-ready state.

## Feature Workflow

- Use a TDD-first implementation loop when building new features: red, green, refactor.
- Break features into small vertical slices that deliver end-to-end behaviour instead of horizontal layer-only changes.
- Start from the most stable contract, usually domain or use case behaviour, then promote coverage upward into presentation and UI.
- Keep scope aligned with acceptance criteria, edge cases, and observable outcomes.
- Run the relevant verification suite before handoff and keep changes commit-ready.

## Speckit Artifacts

- Project-wide Speckit governance files live under `.specify/`.
- `/speckit.constitution` reads and writes `.specify/memory/constitution.md`.
- Feature-scoped Speckit artifacts live under `specs/<NNN-feature-name>/`.
- `/speckit.specify` creates the feature folder and writes `spec.md`.
- `/speckit.plan` writes `plan.md` and supporting design artifacts such as `research.md`, `data-model.md`, `quickstart.md`, and `contracts/`.
- `/speckit.tasks` writes `tasks.md`.
- `/speckit.clarify` updates the existing feature `spec.md`.
- `/speckit.analyze` reads `spec.md`, `plan.md`, `tasks.md`, and `.specify/memory/constitution.md`; if you persist its output, keep it in the same feature folder.
- `/speckit.implement` reads the feature artifacts and mainly produces code changes rather than a required markdown file.
- `/speckit.checklist` outputs checklist markdown under `specs/<NNN-feature-name>/checklists/`.
- `/speckit.taskstoissues` uses `tasks.md` as input and primarily produces GitHub issues rather than markdown output.

## Kotlin encapsulation

For all new and modified Kotlin files in this project:

- Prefer `internal` for classes, interfaces, objects, sealed subclasses, and data classes unless the type is intentionally part of a module's public API.
- Prefer `internal` for top-level functions, including extension functions, unless they are intentionally public API.
- Use `internal` on both `expect` and `actual` declarations when the API is only used inside the module.
- Leave declarations `public` only when they are intentionally exposed to other Gradle modules or are part of a stable public API.

## Kotlin Conventions

- Use named arguments at call sites.
- Prefer `val` over `var`.
- Prefer expression-bodied functions when the body is a single expression.
- Prefer exhaustive `when` over long `if`/`else` chains for sealed hierarchies.
- Avoid `!!`; use explicit null handling and boundary checks instead.
- Use `value.ok()` and `error.err()` when constructing result values.
- Protect mutable in-memory repository state with `Mutex` and `withLock`.

## Testing

- Follow a GIVEN-WHEN-THEN approach for test design and naming.
- Use `sut` as the variable name for the subject under test.
- Initialise the SUT and shared dependencies in `@BeforeTest setUp()` using `lateinit var` where appropriate.
- Separate test setup, action, and assertions with blank lines only; do not add GIVEN/WHEN/THEN comments.
- Use `runTest {}` for suspend and coroutine-based tests, not `runBlocking`.
- Prefer fakes over real implementations in unit tests. Use mocks only when interaction verification matters.
- Keep unit tests pure and deterministic: no real network, database, filesystem, system clock, or randomness.
- Prefer `commonTest` for platform-agnostic logic and use platform-specific tests only for platform behaviour.

## Reference Files

- `.ai/AGENTS.md`
- `.ai/CONVENTIONS.md`
- `.ai/WORKFLOW_IMPLEMENT_FEATURE.md`
- `.ai/WORKFLOW_UNIT_TEST.md`
