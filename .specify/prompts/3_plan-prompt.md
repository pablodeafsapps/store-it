# Plan & Tasks (/speckit.plan /speckit.tasks)

/speckit.plan This project will be developed using Kotlin Multi-Platform (KMP), according to @.ai/AGENTS.md and following the conventions stated in @.ai/CONVENTIONS.md.

**Project structure (post-AGP 9.0):** There is no separate `:shared` module. All shared logic and the Android app live in the `:composeApp` module (commonMain, androidMain, etc.). Plans and tasks must use paths under `composeApp/` only.

Initially, the back-end will be hosted in Firebase, so leave some placeholders for later setup. Bear in mind this may change in the future, so keep things as flexible as possible. There should be a linter configured (such as [Detekt](https://github.com/detekt/detekt)), and some Github Actions workflows for basic CI/CD operations, for instance, build and test.

Generate a proper plan for developing "Store it!".
