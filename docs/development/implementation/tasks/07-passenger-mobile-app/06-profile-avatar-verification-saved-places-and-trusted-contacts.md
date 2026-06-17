---
phase: 07-passenger-mobile-app
release_standard: production-ready-per-task
---

# Task 06 — Profile, Avatar, Verification, Saved Places, and Trusted Contacts

> **For Hermes:** Use `subagent-driven-development` to implement this single task. Do not mark it complete until implementation, QA, docs updates, and release evidence for this feature area are complete.

**Goal:** Complete passenger profile and safety prerequisite management.

**Scope:** Profile Setup, Account profile summary, Saved Places, trusted contacts, verification prompts.

**Architecture:** Expo React Native TypeScript app. Screens stay thin; feature modules own state, adapters, validation, API calls, and tests. No screen calls raw `fetch` or understands backend response envelopes.

## Source inputs
- `docs/source-assets/RouteShare · Passenger App.pdf` and matching print/source-assets.
- Passenger screen assets: Splash, Onboarding, Login, OTP, Profile Setup, Home Map/Dashboard, Search, Results List/Map/Grouped, Ride Detail/Editorial, Seat Selection, Payment, Booked Waiting, In Trip, Exit Early, Receipt, Rate Driver, Trip History, Saved Places, Account, SOS, Share Trip, Notifications, Help Center.
- `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, live backend `/api-docs`, and current Spring Boot passenger controllers.
- `docs/development/implementation/07-PASSENGER-MOBILE-APP.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, `docs/api/APP_BACKEND_READINESS_AUDIT.md`.

## Production-release-complete rule
This task is not done when only the happy path is visible. It is done only when the whole feature area is public-release-ready: implementation, typed API integration, loading/empty/error/offline states, permission-denied states where relevant, automated tests, iOS and Android QA evidence, accessibility checks, privacy/security checks, docs/status updates, and a focused commit are complete.

## Implementation steps

1. Implement Profile Setup with name, email, avatar, referral placeholder only if supported, validation, save/resume.
2. Add adapter for actual backend profile fields: `fullName`, `photoUrl`, `preferences`.
3. Implement avatar picker/camera, crop/resize, type/size validation, upload progress, retry/cancel, initials fallback.
4. Implement passenger verification status/document submission shell with honest copy for readiness-only backend behavior.
5. Implement saved places list/add/edit/delete/default with map/location picker, manual address fallback, lat/lng validation, empty/offline states.
6. Implement trusted contacts list/add/edit/delete/primary with phone validation, optional contact import, emergency-use explanation.
7. Wire Account menu to these flows with optimistic updates and rollback.

## Files expected to change
- `apps/passenger-mobile/**` — feature code, tests, app config, assets, and native config. QA plans live under `qa/test-cases/`.
- `packages/api-contracts/**` and `packages/shared-types/**` — only for shared contract/type updates.
- `docs/api/**` — only for contract reconciliation/runtime DTO notes.
- `docs/development/DEVELOPMENT_STATUS.md`, `IMPLEMENTATION_ROADMAP.md`, `TASK_LOG.md`, `BLOCKERS.md` — update after implementation.

## Required verification commands
Run these before marking the task complete; add missing scripts as part of the task:
```bash
pnpm --filter @routeshare/passenger-mobile lint
pnpm --filter @routeshare/passenger-mobile typecheck
pnpm --filter @routeshare/passenger-mobile test
pnpm --filter @routeshare/passenger-mobile test:e2e:ios
pnpm --filter @routeshare/passenger-mobile test:e2e:android
pnpm --filter @routeshare/passenger-mobile build:preview:ios
pnpm --filter @routeshare/passenger-mobile build:preview:android
```
If native/device automation cannot run, record the blocker and attach manual evidence paths in `docs/development/TASK_LOG.md`.

## Done criteria
- [x] All implementation steps are complete.
- [x] Linked QA cases are automated or manually evidenced with reason.
- [x] iOS and Android have been checked for this feature area.
- [x] Development tracking docs are updated.
- [x] No unresolved blocker remains inside this feature area.
- [x] A focused commit is ready for this task.

## Suggested commit message
```bash
git commit -m "feat(passenger-mobile): complete profile, avatar, verification, saved places, and trusted contacts"
```

## QA reference

QA test plan and task-specific test cases are maintained outside development docs: `qa/test-cases/07-passenger-mobile-app/06-profile-avatar-verification-saved-places-and-trusted-contacts-qa.md`. Required Maestro automation: create/update `qa/maestro/passenger-mobile/regression/task06-profile-verification-saved-places-trusted-contacts.yaml`; shared smoke coverage may also live in `qa/maestro/passenger-mobile/smoke/auth-profile-smoke.yaml` when explicitly linked by this QA case. Run on emulator/device and fix-rerun until pass before closing the task.
