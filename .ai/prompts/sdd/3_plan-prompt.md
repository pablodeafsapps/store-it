# Plan & Tasks (/speckit.plan /speckit.tasks)

Sample 1

/speckit.plan This project will be developed using Kotlin Multi-Platform (KMP), according to @.ai/AGENTS.md and following the conventions stated in @.ai/CONVENTIONS.md.

**Project structure (post-AGP 9.0):** All shared logic and the Android app live in the `:shared` module (commonMain, androidMain, etc.). Plans and tasks must use paths under `shared/` only.

Initially, the back-end will be hosted in Firebase, so leave some placeholders for later setup. Bear in mind this may change in the future, so keep things as flexible as possible. There should be a linter configured (such as [Detekt](https://github.com/detekt/detekt)), and some Github Actions workflows for basic CI/CD operations, for instance, build and test.

Generate a proper plan for developing "Store it!".


Sample 2

$speckit-plan This is a feature to be integrated in a Kotlin Multi-Platform (KPM) project, according to AGENTS.md , .ai/AGENTS.md , and .ai/CONVENTIONS.md.

**Project structure (post-AGP 9.0):** All shared logic and the Android app live in the `:shared` module (commonMain, androidMain, etc.). Plans and tasks must use paths under `shared/` only. For this particular feature, unless
Firebase API differ for Android and iOS, all logic should be in the `:shared` module.

Generate a proper plan for developing "Store it!".
