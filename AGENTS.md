# Store It Project Instructions

Apply these instructions for work in this repository.

## Architecture

- This is a Kotlin Multiplatform app with `:shared` as the shared KMP module and `:androidApp` as the Android app module. There is no separate `:shared` module.
- Maximise code in `shared/src/commonMain`. Keep `androidMain`, `iosMain`, `androidApp`, and `iosApp` thin and limited to platform-specific integration.
- Shared business logic, repositories, use cases, shared presentation state, DI setup, and `expect` declarations belong in `commonMain`.
- Data-source interfaces and provider/local data-source implementations belong under `shared/src/commonMain/kotlin/org/deafsapps/storeit/data/datasource`.
- Name data sources with the qualifier immediately before `DataSource`, for example `FirebaseRackDataSource`, `SqlDelightItemDataSource`, `AuthRemoteDataSource`, and `AccountRemoteDataSource`. Do not use names like `RemoteAuthDataSource` where `Remote` appears to qualify the business concept instead of the data source.
- `androidMain` and `iosMain` should only contain `actual` implementations and minimal platform services. Do not put platform UI there.
- Android UI, app entry, and navigation belong in `:androidApp`. iOS UI belongs in `iosApp`.
- Respect dependency direction: UI -> Presentation -> Domain -> Data -> Platform APIs.
- Domain use cases must depend on domain repository/use-case abstractions only. Do not inject or import `data.datasource` types from domain use cases; if a use case needs datasource-backed orchestration, add a domain repository interface and implement it in the data layer.

## Koin And ViewModels

- Android Koin setup is owned by `StoreItApplication` in `:androidApp`, using `AndroidModule` as the composition root.
- iOS Koin setup starts from `KoinInitKt.doInitKoinIos()` and uses shared `AppModule` bindings.
- Shared ViewModels live in `commonMain` and are based on `StoreItViewModel`.
- Do not reintroduce a shared iOS `CoroutineScope`. Each iOS ViewModel instance owns its scope through the `StoreItViewModel` iOS actual.
- For parameterised ViewModels, pass only business parameters such as `rackId`, not a coroutine scope.

## KMP Placement

- Follow the `kmp-source-set-placement` skill when adding or moving Kotlin Multiplatform code.
- Canonical project skill path:
  - `.agents/skills/kmp-source-set-placement/SKILL.md`
- Engine-specific mirrors may also exist in:
  - `.cursor/skills/kmp-source-set-placement/SKILL.md`
- User-level fallback skill path:
  - `/Users/pablo/.codex/skills/kmp-source-set-placement/SKILL.md`

## Delivery workflow

When starting or finishing substantive work in this repository, follow the `store-it-delivery-workflow` skill.

Canonical project skill path:
- `.agents/skills/store-it-delivery-workflow/SKILL.md`

Engine-specific mirrors may also exist in:
- `.cursor/skills/store-it-delivery-workflow/SKILL.md`

User-level fallback skill path:
- `/Users/pablo/.codex/skills/store-it-delivery-workflow/SKILL.md`

This includes:
- updating the related Trello card through the expected workflow,
- running the Gradle verification steps before handoff,
- confirming the iOS build when shared Kotlin or framework integration changed,
- leaving changes in a commit-ready state.

## Skills And Rules

- Canonical project-owned skills live under `.agents/skills/`.
- Engine-specific guidance may also live under `.cursor/skills/`, `.cursor/rules/`, `.claude/commands/`, or other engine folders. Treat those as engine adapters or overlays, not as the primary project source of truth.
- When the same capability exists in more than one place, keep `.agents/skills/` authoritative and keep engine-specific copies aligned with it.

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
- When working with the project `Result` type, prefer `map`, `flatMap`, `suspendFlatMap`, `fold`, and related helpers over explicit branching on `Ok` and `Err` when the combinator form is clearer.
- Do not pattern-match on `Ok`/`Err` in use cases or repositories for ordinary success/failure flow. Use the project `Result` helpers (`map`, `flatMap`, `suspendFlatMap`, `fold`, `failureOrNull`, `getOrNull`, etc.) so error propagation remains consistent and reviewable.
- Avoid generic catch clauses such as `catch (exception: Exception)` and `catch (throwable: Throwable)`. Catch the narrowest concrete exception types the block can actually throw, let programmer bugs fail loudly, and never swallow `CancellationException`.
- When mapping an unexpected exception into `DomainError.Unknown`, preserve the original failure context by setting both `message` and `cause`. Do not replace thrown exceptions with a bare `DomainError.Unknown()` unless no throwable exists.
- For datasource delete and clear operations, prefer `Result<DomainError, Long>` when the backing store can report affected-row counts. Treat `ok(0L)` as a successful no-op, not as an error.
- For Firebase-backed or other remote datasources, apply the same rule explicitly: catch provider-specific exceptions such as `FirebaseFirestoreException`, `FirebaseStorageException`, or serialization failures instead of `Throwable`, and return affected-row/object counts from delete operations whenever the remote API lets you determine them.
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

## Active Technologies
- Kotlin Multiplatform with Swift app integration; Kotlin current project baseline, Swift 5.x for iOS shell + Kotlin Multiplatform, kotlinx-coroutines, Koin annotations, SQLDelight, Kotlinx Serialization, Firebase Authentication, Firebase Cloud Firestore, Firebase Cloud Storage (005-remote-sync-auth)
- Local SQLDelight database plus remote account-backed dataset in Firebase; secure local session/token storage via platform-secure facilities behind shared abstractions (005-remote-sync-auth)

## Recent Changes
- 005-remote-sync-auth: Added Kotlin Multiplatform with Swift app integration; Kotlin current project baseline, Swift 5.x for iOS shell + Kotlin Multiplatform, kotlinx-coroutines, Koin annotations, SQLDelight, Kotlinx Serialization, Firebase Authentication, Firebase Cloud Firestore, Firebase Cloud Storage
