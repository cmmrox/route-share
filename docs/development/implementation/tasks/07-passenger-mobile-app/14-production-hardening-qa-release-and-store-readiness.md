---
phase: 07-passenger-mobile-app
release_standard: production-ready-per-task
---

# Task 14 — Production Hardening, QA Release, and Store Readiness

> **For Hermes:** Use `subagent-driven-development` to implement this single task. Do not mark it complete until implementation, QA, docs updates, and release evidence for this feature area are complete.

**Goal:** Make the completed passenger app public-release-ready.

**Scope:** All passenger screens and flows.

**Architecture:** Expo React Native TypeScript app. Screens stay thin; feature modules own state, adapters, validation, API calls, and tests. No screen calls raw `fetch` or understands backend response envelopes.

## Source inputs
- `docs/source-assets/RouteShare · Passenger App.pdf` and matching print/source-assets.
- Passenger screen assets: Splash, Onboarding, Login, OTP, Profile Setup, Home Map/Dashboard, Search, Results List/Map/Grouped, Ride Detail/Editorial, Seat Selection, Payment, Booked Waiting, In Trip, Exit Early, Receipt, Rate Driver, Trip History, Saved Places, Account, SOS, Share Trip, Notifications, Help Center.
- `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, live backend `/api-docs`, and current Spring Boot passenger controllers.
- `docs/development/implementation/07-PASSENGER-MOBILE-APP.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, `docs/api/APP_BACKEND_READINESS_AUDIT.md`.

## Production-release-complete rule
This task is not done when only the happy path is visible. It is done only when the whole feature area is public-release-ready: implementation, typed API integration, loading/empty/error/offline states, permission-denied states where relevant, automated tests, iOS and Android QA evidence, accessibility checks, privacy/security checks, docs/status updates, and a focused commit are complete.

## Implementation steps

1. Create full QA matrix for iOS/Android, screen sizes, dark/light, offline/slow, fresh/upgrade, permissions, auth states.
2. Implement E2E flows: auth, profile, saved place, trusted contact, search, results, detail, booking, payment, live trip, share, SOS, receipt, rating, support, sign-out.
3. Add performance budgets for cold start, transitions, result list, map, memory, battery/location behavior; capture evidence.
4. Complete accessibility audit for screen reader, dynamic text, contrast, touch targets, reduced motion.
5. Complete privacy/security audit: secure storage, no secrets, redacted crash/log payloads, no raw card/OTP/precise location leaks.
6. Add release automation/build profiles, changelog, artifact storage, QA sign-off.
7. Fill `release-readiness-checklist.md` with evidence and update development tracking docs.

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
- [ ] All implementation steps are complete.
- [ ] Linked QA cases are automated or manually evidenced with reason.
- [ ] iOS and Android have been checked for this feature area.
- [ ] Development tracking docs are updated.
- [ ] No unresolved blocker remains inside this feature area.
- [ ] A focused commit is ready for this task.

## Suggested commit message
```bash
git commit -m "feat(passenger-mobile): complete production hardening, qa release, and store readiness"
```

## QA reference

QA test plan and task-specific test cases are maintained outside development docs: `qa/test-cases/07-passenger-mobile-app/14-production-hardening-qa-release-and-store-readiness-qa.md`.
