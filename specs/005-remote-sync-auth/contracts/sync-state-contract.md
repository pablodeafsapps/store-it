# Contract: Sync State And User-Visible Modes

**Feature**: 005-remote-sync-auth  
**Date**: 2026-04-09

This contract defines the user-visible state surface that shared presentation code must receive from the account and sync domain.

## Data Mode States

- **LocalOnly**
  - User is not signed in
  - Device data exists only locally
- **AccountBackedSynchronized**
  - User is signed in
  - Local and remote datasets are aligned
- **AccountBackedPendingSync**
  - User is signed in
  - Local copy contains unsynchronized changes or pending remote updates
- **ReconciliationRequired**
  - User is signed in
  - Both local and remote datasets exist and require explicit resolution
- **SignedOutWithLocalCopy**
  - User has signed out
  - Device still retains a defined local dataset state visible to the user

## Sync Status States

- **Idle**
- **Syncing**
- **Synchronized**
- **PendingUpload**
- **PendingDownload**
- **Failed**
- **BlockedByReconciliation**

## Minimum User-Visible Outputs

Shared presentation must be able to derive:

- Whether the user is signed in
- Whether the app is in local-only mode
- Whether data is safely backed up
- Whether unsynchronized changes exist
- Whether reconciliation is required before proceeding
- Whether sign-out would affect pending backup work
- A recoverable failure message when synchronization cannot complete

## Error Categories

The shared state surface should differentiate at least:

- Authentication failure
- Session unavailable or expired
- Remote unavailable
- Local pending changes not yet uploaded
- Reconciliation required
- Unrecoverable sync failure needing explicit user action

## Notes

- This is a presentation-facing contract, not a transport or API schema.
- UI wording remains platform-specific, but the underlying states should come from shared logic.
