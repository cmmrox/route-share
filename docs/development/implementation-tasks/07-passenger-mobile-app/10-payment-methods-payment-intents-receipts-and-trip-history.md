---
phase: 07-passenger-mobile-app
release_standard: production-ready-per-task
---

# Task 10 — Payment Methods, Payment Intents, Receipts, and Trip History

> **For Hermes:** Use `subagent-driven-development` to implement this single task. Do not mark it complete until implementation, QA, docs updates, and release evidence for this feature area are complete.

**Goal:** Complete payment, receipt, and trip history experience.

**Scope:** Payment, Receipt, Trip History, Account payment entry.

**Architecture:** Expo React Native TypeScript app. Screens stay thin; feature modules own state, adapters, validation, API calls, and tests. No screen calls raw `fetch` or understands backend response envelopes.


## Source inputs
- `docs/source-assets/RouteShare · Passenger App.pdf` and matching print/source-assets.
- Passenger screen assets: Splash, Onboarding, Login, OTP, Profile Setup, Home Map/Dashboard, Search, Results List/Map/Grouped, Ride Detail/Editorial, Seat Selection, Payment, Booked Waiting, In Trip, Exit Early, Receipt, Rate Driver, Trip History, Saved Places, Account, SOS, Share Trip, Notifications, Help Center.
- `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, live backend `/api-docs`, and current Spring Boot passenger controllers.
- `docs/implementation/07-PASSENGER-MOBILE-APP.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, `docs/api/APP_BACKEND_READINESS_AUDIT.md`.


## Production-release-complete rule
This task is not done when only the happy path is visible. It is done only when the whole feature area is public-release-ready: implementation, typed API integration, loading/empty/error/offline states, permission-denied states where relevant, automated tests, iOS and Android QA evidence, accessibility checks, privacy/security checks, docs/status updates, and a focused commit are complete.

## Implementation steps

1. Implement Payment screen with card/cash/wallet/add-card, fare breakdown, preauthorization copy, selected method state.
2. Implement payment method management from Account: list, add/provider-token placeholder, set default, delete, empty/provider-unavailable states.
3. Isolate current readiness payment-method behavior behind provider abstraction; never store raw card details.
4. Implement payment intent creation using actual backend `bookingId` shape; display server-authoritative amounts.
5. Implement Receipt with trip id, paid total, actual km, driver/vehicle, fare rows, export/share, get-help, rate CTA.
6. Implement Trip History filters and summary; list row opens receipt/detail; include empty/error states.



## Files expected to change
- `apps/passenger-mobile/**` — feature code, tests, app config, assets, native config, QA helpers.
- `packages/api-contracts/**` and `packages/shared-types/**` — only for shared contract/type updates.
- `docs/api/**` — only for contract reconciliation/runtime DTO notes.
- `docs/development/DEVELOPMENT_STATUS.md`, `IMPLEMENTATION_ROADMAP.md`, `TASK_LOG.md`, `BLOCKERS.md` — update after implementation.

## QA test cases

- Switching payment methods updates selected state and CTA.
- Payment intent request never sends client-controlled amount unless backend contract explicitly requires it.
- Card failure/wallet insufficient/backend 500 show retry/change-method and create no duplicate payment.
- Receipt displays exact server total/distance/driver/vehicle/trip id.
- Trip history filters and summary are correct.
- Logs/screenshots contain only masked payment details.


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
git commit -m "feat(passenger-mobile): complete payment methods, payment intents, receipts, and trip history"
```
