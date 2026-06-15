---
phase: 07-passenger-mobile-app
release_standard: production-ready-per-task
---

# Task 09 — Seat Selection, Booking Idempotency, and Cancellation

> **For Hermes:** Use `subagent-driven-development` to implement this single task. Do not mark it complete until implementation, QA, docs updates, and release evidence for this feature area are complete.

**Goal:** Ship safe booking creation and cancellation.

**Scope:** Seat Selection, booking confirmation loading, Booked Waiting entry, cancellation confirmation.

**Architecture:** Expo React Native TypeScript app. Screens stay thin; feature modules own state, adapters, validation, API calls, and tests. No screen calls raw `fetch` or understands backend response envelopes.


## Source inputs
- `docs/source-assets/RouteShare · Passenger App.pdf` and matching print/source-assets.
- Passenger screen assets: Splash, Onboarding, Login, OTP, Profile Setup, Home Map/Dashboard, Search, Results List/Map/Grouped, Ride Detail/Editorial, Seat Selection, Payment, Booked Waiting, In Trip, Exit Early, Receipt, Rate Driver, Trip History, Saved Places, Account, SOS, Share Trip, Notifications, Help Center.
- `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, live backend `/api-docs`, and current Spring Boot passenger controllers.
- `docs/implementation/07-PASSENGER-MOBILE-APP.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, `docs/api/APP_BACKEND_READINESS_AUDIT.md`.


## Production-release-complete rule
This task is not done when only the happy path is visible. It is done only when the whole feature area is public-release-ready: implementation, typed API integration, loading/empty/error/offline states, permission-denied states where relevant, automated tests, iOS and Android QA evidence, accessibility checks, privacy/security checks, docs/status updates, and a focused commit are complete.

## Implementation steps

1. Implement Seat Selection with 1–3 seats, taken/free/selected states, legend, companion copy, disabled/sold-out states.
2. Validate selected seats against availability and configured max before API call.
3. Adapt booking request to actual backend DTO: `routeOccurrenceId`, `seats`, pickup/drop lat/lng, pickup/drop route fractions.
4. Generate and persist `Idempotency-Key` per booking attempt; reuse on retry of same request.
5. Prevent duplicate submits, handle timeout/unknown result by fetching booking state, and avoid duplicate bookings.
6. Implement cancellation confirmation with reason optional, policy copy, success/failure states.



## Files expected to change
- `apps/passenger-mobile/**` — feature code, tests, app config, assets, native config, QA helpers.
- `packages/api-contracts/**` and `packages/shared-types/**` — only for shared contract/type updates.
- `docs/api/**` — only for contract reconciliation/runtime DTO notes.
- `docs/development/DEVELOPMENT_STATUS.md`, `IMPLEMENTATION_ROADMAP.md`, `TASK_LOG.md`, `BLOCKERS.md` — update after implementation.

## QA test cases

- Single-seat booking sends correct DTO and idempotency key.
- Over-selecting seats is blocked; valid multi-seat succeeds.
- Network timeout retry reuses same idempotency key and creates no duplicate booking.
- Rapid CTA taps start only one mutation.
- Cancellation success updates status; forbidden cancellation keeps booking active with policy message.
- Seat cells are accessible and taken seats cannot be selected.


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
git commit -m "feat(passenger-mobile): complete seat selection, booking idempotency, and cancellation"
```
