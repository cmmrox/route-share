---
phase: 07-passenger-mobile-app
release_standard: production-ready-per-task
---

# Task 07 — Home, Search, Location, and Route Discovery

> **For Hermes:** Use `subagent-driven-development` to implement this single task. Do not mark it complete until implementation, QA, docs updates, and release evidence for this feature area are complete.

**Goal:** Ship pickup/dropoff/time/seat search start flow.

**Scope:** Home Map A, Home Dashboard B, Search Places, time/seat pickers, location states.

**Architecture:** Expo React Native TypeScript app. Screens stay thin; feature modules own state, adapters, validation, API calls, and tests. No screen calls raw `fetch` or understands backend response envelopes.


## Source inputs
- `docs/source-assets/RouteShare · Passenger App.pdf` and matching print/source-assets.
- Passenger screen assets: Splash, Onboarding, Login, OTP, Profile Setup, Home Map/Dashboard, Search, Results List/Map/Grouped, Ride Detail/Editorial, Seat Selection, Payment, Booked Waiting, In Trip, Exit Early, Receipt, Rate Driver, Trip History, Saved Places, Account, SOS, Share Trip, Notifications, Help Center.
- `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, live backend `/api-docs`, and current Spring Boot passenger controllers.
- `docs/implementation/07-PASSENGER-MOBILE-APP.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, `docs/api/APP_BACKEND_READINESS_AUDIT.md`.


## Production-release-complete rule
This task is not done when only the happy path is visible. It is done only when the whole feature area is public-release-ready: implementation, typed API integration, loading/empty/error/offline states, permission-denied states where relevant, automated tests, iOS and Android QA evidence, accessibility checks, privacy/security checks, docs/status updates, and a focused commit are complete.

## Implementation steps

1. Implement Home Map A with map, Safety chip, account/menu, destination CTA, quick saved chips, frequent/recent rows.
2. Implement Home Dashboard B behind explicit config/feature flag with commute card, stats, quick places, ride suggestions.
3. Implement location permission request, denied/unavailable handling, current location, manual pickup fallback.
4. Implement Search Places with pickup/drop fields, swap, saved places, suggestions, recent searches, keyboard-safe layout.
5. Implement time picker and seat count picker; validate future times and seat limits.
6. Build ride search request adapter to actual backend DTO: coordinates, `requestedDepartureTime`, `seats`, radius/window/limit.
7. Persist recent searches with clear/delete and privacy-safe retention.



## Files expected to change
- `apps/passenger-mobile/**` — feature code, tests, app config, assets, native config, QA helpers.
- `packages/api-contracts/**` and `packages/shared-types/**` — only for shared contract/type updates.
- `docs/api/**` — only for contract reconciliation/runtime DTO notes.
- `docs/development/DEVELOPMENT_STATUS.md`, `IMPLEMENTATION_ROADMAP.md`, `TASK_LOG.md`, `BLOCKERS.md` — update after implementation.

## QA test cases

- Home loads greeting, saved places, recent routes, and map/dashboard without flicker.
- Location denied still allows manual pickup search.
- Pickup/drop swap changes labels and coordinates.
- Past time is blocked; future scheduled search sends correct DTO.
- Empty/no-results/backend-error states offer Retry/Edit Search.
- Search request contains valid lat/lng/time/seats and no stale route context.


## Automated test requirements
- Unit tests for pure mapping, validation, and state logic.
- React Native Testing Library tests for screen states and interactions.
- API client/mutation tests for success, validation error, 401/403, conflict, timeout, malformed JSON, and retry behavior.
- E2E test for the primary user path in this task and at least one failure/recovery path.
- Accessibility tests or manual screen-reader evidence for every interactive flow.


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
- [ ] Every QA case above is automated or manually evidenced with reason.
- [ ] iOS and Android have been checked for this feature area.
- [ ] Development tracking docs are updated.
- [ ] No unresolved blocker remains inside this feature area.
- [ ] A focused commit is ready for this task.

## Suggested commit message
```bash
git commit -m "feat(passenger-mobile): complete home, search, location, and route discovery"
```
