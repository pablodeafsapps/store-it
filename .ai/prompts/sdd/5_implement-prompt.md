# Implement (/speckit.implement)

/speckit.implement

**Structure:** Post-AGP 9.0 — all shared and Android code lives in `:composeApp` (no `:shared`). Use `composeApp/src/commonMain/`, `composeApp/src/androidMain/`, `composeApp/src/commonTest/` as per plan.md and tasks.md.

Every time some work is carried out:

- Build the project for both Android and iOS to check the application is still functional.
- Use the Trello MCP to properly update any related card, even adding comments to them to best explain what's going on.
