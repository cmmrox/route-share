---
phase: 07-passenger-mobile-app
release_standard: production-ready-per-task
---

# Task 08 — Results List, Results Map, Filtering, and Ride Detail

> **For Hermes:** Use `subagent-driven-development` to implement this single task. Do not mark it complete until implementation, QA, docs updates, and release evidence for this feature area are complete.

**Goal:** Complete ride browsing and pre-booking evaluation.

**Scope:** Results List, Results Map, Grouped Results, Ride Detail, Editorial Detail.

**Architecture:** Expo React Native TypeScript app. Screens stay thin; feature modules own state, adapters, validation, API calls, and tests. No screen calls raw `fetch` or understands backend response envelopes.

## Source inputs
- `docs/source-assets/RouteShare · Passenger App.pdf` and matching print/source-assets.
- Passenger screen assets: Splash, Onboarding, Login, OTP, Profile Setup, Home Map/Dashboard, Search, Results List/Map/Grouped, Ride Detail/Editorial, Seat Selection, Payment, Booked Waiting, In Trip, Exit Early, Receipt, Rate Driver, Trip History, Saved Places, Account, SOS, Share Trip, Notifications, Help Center.
- `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, live backend `/api-docs`, and current Spring Boot passenger controllers.
- `docs/development/implementation/07-PASSENGER-MOBILE-APP.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, `docs/api/APP_BACKEND_READINESS_AUDIT.md`.

## Production-release-complete rule
This task is not done when only the happy path is visible. It is done only when the whole feature area is public-release-ready: implementation, typed API integration, loading/empty/error/offline states, permission-denied states where relevant, automated tests, iOS and Android QA evidence, accessibility checks, privacy/security checks, docs/status updates, and a focused commit are complete.

## Implementation steps

1. Normalize backend result data into UI model: match %, overlap tier, driver, vehicle, seats, departure, ETA, fare, occurrence id, route fractions.
2. Implement Results List A with header, filters, list/map toggle, ride cards, badges, fares, driver/vehicle/seats.
3. Implement Results Map with route line, pickup/drop labels, car pins, selected card stack/swipe, accessible list fallback.
4. Implement Grouped Results B with overlap groups and compact rows.
5. Implement Ride Detail A and Editorial B with map header, share, driver card, timeline, vehicle, fare estimate, why-good-match, policy/safety notes.
6. Implement filters/sorts: match threshold, price, departure, rating, seats. Use client filtering only when backend lacks it.
7. Preserve booking handoff data for route occurrence, coordinates, route fractions, selected seats/fare/payment eligibility.

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
git commit -m "feat(passenger-mobile): complete results list, results map, filtering, and ride detail"
```

## QA reference

QA test plan and task-specific test cases are maintained outside development docs: `qa/test-cases/07-passenger-mobile-app/08-results-list-map-filtering-and-ride-detail-qa.md`.
