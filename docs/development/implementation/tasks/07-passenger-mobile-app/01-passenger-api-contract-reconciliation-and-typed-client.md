---
phase: 07-passenger-mobile-app
release_standard: production-ready-per-task
---

# Task 01 — Passenger API Contract Reconciliation and Typed Client

> **For Hermes:** Use `subagent-driven-development` to implement this single task. Do not mark it complete until implementation, QA, docs updates, and release evidence for this feature area are complete.

**Goal:** Make the mobile app consume the real backend safely before UI work.

**Scope:** All passenger endpoints.

**Architecture:** Expo React Native TypeScript app. Screens stay thin; feature modules own state, adapters, validation, API calls, and tests. No screen calls raw `fetch` or understands backend response envelopes.

## Source inputs
- `docs/source-assets/RouteShare · Passenger App.pdf` and matching print/source-assets.
- Passenger screen assets: Splash, Onboarding, Login, OTP, Profile Setup, Home Map/Dashboard, Search, Results List/Map/Grouped, Ride Detail/Editorial, Seat Selection, Payment, Booked Waiting, In Trip, Exit Early, Receipt, Rate Driver, Trip History, Saved Places, Account, SOS, Share Trip, Notifications, Help Center.
- `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, live backend `/api-docs`, and current Spring Boot passenger controllers.
- `docs/development/implementation/07-PASSENGER-MOBILE-APP.md`, `docs/development/IMPLEMENTATION_ROADMAP.md`, `docs/api/APP_BACKEND_READINESS_AUDIT.md`.

## Production-release-complete rule
This task is not done when only the happy path is visible. It is done only when the whole feature area is public-release-ready: implementation, typed API integration, loading/empty/error/offline states, permission-denied states where relevant, automated tests, iOS and Android QA evidence, accessibility checks, privacy/security checks, docs/status updates, and a focused commit are complete.

## Implementation steps

1. Compare `docs/api/passenger-app.openapi.json`, `packages/api-contracts/src/index.ts`, live `http://localhost:8080/api-docs`, and actual controller DTOs.
2. Create `docs/api/PASSENGER_MOBILE_CONTRACT_RECONCILIATION.md` with each path marked `MATCHED`, `RUNTIME_ENVELOPE`, `DTO_MISMATCH`, `READINESS_PLACEHOLDER`, or `DEFERRED_PRODUCTION`.
3. Implement a generated or generated-compatible TypeScript contract layer for passenger endpoints.
4. Implement `api-client.ts` with base URL config, bearer injection, timeout, typed errors, JSON parsing, centralized `ApiResponse<T>` envelope unwrap, and redacted logging.
5. Add endpoint modules for app config, auth, profile, saved places, trusted contacts, ride search, bookings, payments, trips, notifications, support, and safety.
6. Add tests for DTO adapters, envelope unwrap, 401/403/409/500/timeout/malformed JSON, and known runtime/OpenAPI mismatches.

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
git commit -m "feat(passenger-mobile): complete passenger api contract reconciliation and typed client"
```

## Implementation result — 2026-06-14

Status: `CORE_VERIFIED_NATIVE_QA_DEFERRED_TO_TASK_02`

Completed:

- [x] Passenger OpenAPI/runtime contract reconciliation created at `docs/api/PASSENGER_MOBILE_CONTRACT_RECONCILIATION.md`.
- [x] Passenger contract inventory updated in `packages/api-contracts/src/index.ts`.
- [x] `@routeshare/passenger-mobile` TypeScript package added.
- [x] Central API client added with base URL config, bearer injection, timeout, retry behavior, typed HTTP errors, JSON parsing, `ApiResponse<T>` unwrap, and redacted logging.
- [x] Endpoint modules added for app config, auth, profile, saved places, trusted contacts, ride search, bookings, payments, trips, notifications, support, and safety.
- [x] DTO adapters and tests added for known runtime/OpenAPI mismatches.
- [x] Verification passed: `pnpm --filter @routeshare/api-contracts typecheck`, passenger-mobile `lint`, `typecheck`, and `test` (`3` files / `23` tests).
- [x] Final spec/quality re-review approved.

Deferred to Task 02 by documented blocker:

- [ ] Real iOS E2E command.
- [ ] Real Android E2E command.
- [ ] Real iOS preview build command.
- [ ] Real Android preview build command.

Reason: Task 01 is a pure contract/client slice. The Expo/native app scaffold and release pipeline are Task 02 scope; current native commands intentionally fail through `apps/passenger-mobile/scripts/native-blocker.mjs` and are tracked as Blocker 008.

## QA reference

QA test plan and task-specific test cases are maintained outside development docs: `qa/test-cases/07-passenger-mobile-app/01-passenger-api-contract-reconciliation-and-typed-client-qa.md`.
