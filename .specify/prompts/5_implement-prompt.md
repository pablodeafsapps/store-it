# Implement (/speckit.implement)

/speckit.implement

When implementing, follow the post-AGP 9.0 structure: all shared and Android code lives in `:composeApp` (no `:shared` module). Use paths under `composeApp/src/commonMain/`, `composeApp/src/androidMain/`, and `composeApp/src/commonTest/` as per plan.md and tasks.md.
