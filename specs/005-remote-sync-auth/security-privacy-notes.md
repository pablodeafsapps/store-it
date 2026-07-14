# Security & Privacy Notes: Remote Account Sync

**Feature**: `005-remote-sync-auth`  
**Last updated**: 2026-06-01

## Scope

This document centralizes the privacy, data-retention, and account-recovery wording intentionally delegated by `spec.md` and referenced by `quickstart.md`.

## Data Categories

- Account identity: email and account ID
- Session material: provider session/token metadata stored via platform-secure facilities
- Organizer dataset: racks, slots, items, and sync metadata
- Media: rack/item photos included in account backup scope

## Retention Summary

- Local dataset is retained for local-only operation.
- On sign-out, the app keeps a readable local copy (`SignedOutWithLocalCopy`) by design.
- Pending sync metadata is retained locally until synchronized or explicitly cleared.

## Security Boundaries

- Secure session storage must remain behind shared abstractions with platform-secure actual implementations.
- Business sync/reconciliation policy remains in shared code; platform adapters must not own policy decisions.
- No secrets or provider credentials should be committed to repository files.

## Account Recovery Wording Baseline

- Authentication errors should return user-facing guidance without exposing sensitive provider internals.
- Recovery flows beyond sign-in/sign-out/session-restore (for example password reset UX) are outside the current feature scope.

## Follow-ups

- Add legal/privacy-policy linkage once product/legal text is finalized.
- Add telemetry governance note if/when telemetry backends persist user-identifiable fields.
