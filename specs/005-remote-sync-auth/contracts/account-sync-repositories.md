# Contract: Account, Session, And Sync Repositories

**Feature**: 005-remote-sync-auth  
**Date**: 2026-04-09

This document defines the domain-facing contracts needed for optional account-backed synchronization while preserving local-only mode.

## AccountRepository

- **Purpose**: Create accounts, sign users in or out, and expose current account/session state.
- **Responsibilities**:
  - Create account from user-entered credentials or identity details
  - Sign in an existing account
  - Restore previously authenticated session on app start
  - Sign out the active account
  - Expose current account/session state as shared presentation-friendly state
- **Output style**:
  - Success/failure via shared result type
  - Long-lived account/session state via `Flow`

## SyncRepository

- **Purpose**: Coordinate synchronization between the local SQLDelight dataset and the remote account dataset.
- **Responsibilities**:
  - Determine current sync state
  - Upload pending local changes
  - Download remote state into the local copy when needed
  - Retry failed synchronization work
  - Surface reconciliation-required states
  - Mark sync operations as applied or failed
- **Output style**:
  - Sync commands as suspend operations returning shared result type
  - Sync state exposed as `Flow`

## ReconciliationRepository

- **Purpose**: Manage the decision flow when both local and remote datasets contain meaningful data.
- **Responsibilities**:
  - Detect whether reconciliation is required
  - Provide a summary of local vs remote dataset presence
  - Persist the user-confirmed reconciliation decision
  - Apply the selected policy and update sync state
- **Output style**:
  - Detection and summaries as read operations
  - Apply/confirm actions as write operations with shared result type

## SessionCredentialStore

- **Purpose**: Abstract secure local storage of the minimum credential/session material needed for authenticated restore.
- **Responsibilities**:
  - Save authenticated session material securely
  - Restore previously saved session material
  - Clear session material on sign-out or invalidation
- **Placement**:
  - Interface in `shared/src/commonMain`
  - Thin secure-storage implementations in `shared/src/androidMain` and `shared/src/iosMain`

## RemoteAccountDataSource

- **Purpose**: Abstract the remote provider interactions needed for account-backed dataset persistence.
- **Responsibilities**:
  - Fetch the remote account dataset snapshot
  - Apply create/update/delete mutations for organizer entities
  - Expose remote dataset version or sync token metadata
- **Placement**:
  - Shared interface in `commonMain`
  - Thin provider adapters in platform source sets only if provider SDK APIs diverge

## Boundary Rules

- Domain and presentation code must depend on repository interfaces only.
- Shared use cases must not depend directly on Firebase SDK types.
- Platform-specific provider adapters must not own reconciliation policy or sync-state business rules.
